// Synthetic HTTPS round-trip; always removes its own DB rows.
import { readFile } from 'node:fs/promises';
import { request } from 'node:https';
import { randomUUID } from 'node:crypto';
import mysql from 'mysql2/promise';
const config=JSON.parse(await readFile(new URL('../.local-stats/private.json',import.meta.url),'utf8'));
const ca=await readFile(new URL('../.local-stats/receiver-cert.pem',import.meta.url));
const token=Object.values(config.tokens)[0];
const id=randomUUID();
const secondId=randomUUID(), version='test_'+id;
const p={schemaVersion:2,matchId:id,sessionId:randomUUID(),modVersion:'https-integration-test',modStatsVersion:version,
  startedAt:'2026-10-04T12:00:00Z',endedAt:'2026-10-04T12:02:00Z',durationSeconds:120,durationTicks:2400,
  winner:0,survivorsCount:1,maniacsCount:1,computersCharged:2,droppedEvents:0,endReason:'integration_test',
  eligibleForBalance:true,qualityFlags:[],map:{code:'unknown_0'},settings:{},endSettings:{},performance:{},catalog:[],
  players:[{key:'p1',team:'survivor',lateJoin:false,disconnected:false,
    start:{classCode:'alchemist',classScoreboardId:6,perks:['sber_sprout']},end:{},metrics:{hack_points_contributed:1.5},vanillaDelta:{},
    damageTypes:{},itemsUsed:{},perkCounters:{sber_sprout:{attempt_success:1}}},
    {key:'p2',team:'maniac',lateJoin:false,disconnected:false,
    start:{classCode:'pudge',classScoreboardId:5,perks:['high_voltage']},end:{},metrics:{},vanillaDelta:{},damageTypes:{},itemsUsed:{},perkCounters:{}}],
  events:[{type:'knockdown',seconds:20,actor:'p2',target:'p1',data:{}}]};
function send(body,authorization=token) {
  return new Promise((resolve,reject)=> {
    const req=request('https://5.83.140.208:25912/v1/matches',{method:'POST',ca,headers:{Authorization:'Bearer '+authorization,'Content-Type':'application/json'},timeout:15000},res=> {
      let response='';res.setEncoding('utf8');res.on('data',chunk=>response+=chunk);
      res.on('end',()=>resolve({status:res.statusCode,data:JSON.parse(response)}));
    });req.on('error',reject);req.on('timeout',()=>req.destroy(new Error('timeout')));req.end(body);
  });
}
const pool=mysql.createPool({...config.database,connectionLimit:1,connectTimeout:5000});
try {
  const denied=await send(JSON.stringify(p),'incorrect'); if(denied.status!==401) throw new Error('authentication failed');
  const first=await send(JSON.stringify(p)); if(first.status!==201) throw new Error('insert failed: '+JSON.stringify(first));
  const again=await send(JSON.stringify(p)); if(again.status!==200 || !again.data.duplicate || again.data.gameId!==first.data.gameId) throw new Error('duplicate failed');
  const [rows]=await pool.execute('SELECT recorded_players,eligible_for_balance,mod_stats_version FROM mr_stats_matches WHERE match_uuid=?',[id]);
  if(rows[0].recorded_players!==2 || rows[0].eligible_for_balance!==1 || rows[0].mod_stats_version!==p.modStatsVersion) throw new Error('stored data mismatch');
  const second={...p,matchId:secondId,modStatsVersion:version+'_b',winner:1};
  const secondReply=await send(JSON.stringify(second));
  if(secondReply.status!==201) throw new Error('second version insert failed');
  for(const [view,code] of [['mr_stats_class_balance','alchemist'],['mr_stats_perk_balance','sber_sprout']]) {
    const [balance]=await pool.execute(`SELECT mod_stats_version,picks,wins FROM ${view} WHERE code=? AND mod_stats_version IN (?,?) ORDER BY mod_stats_version`,[code,version,version+'_b']);
    if(balance.length!==2 || balance.some(r=>Number(r.picks)!==1) || Number(balance[0].wins)!==1 || Number(balance[1].wins)!==0) throw new Error('balance versions mixed');
  }
  console.log('HTTPS round-trip passed: certificate, auth, acknowledgement, duplicate, stored build version, separate class/perk balance versions');
} finally {
  const conn=await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [matches]=await conn.execute('SELECT id FROM mr_stats_matches WHERE match_uuid IN (?,?) AND mod_version=?',[id,secondId,'https-integration-test']);
    for(const match of matches) {
      const gameId=match.id;
      for(const table of ['mr_stats_events','mr_stats_player_perks','mr_stats_players']) await conn.execute(`DELETE FROM ${table} WHERE match_id=?`,[gameId]);
      await conn.execute('DELETE FROM mr_stats_matches WHERE id=?',[gameId]);
    }
    await conn.commit();console.log('HTTPS synthetic match removed');
  } catch(e) { await conn.rollback();throw e; } finally { conn.release();await pool.end(); }
}
