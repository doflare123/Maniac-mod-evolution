// Operator-only CLI. The HTTP receiver never exposes inspection or migrations.
import { readFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import mysql from 'mysql2/promise';
import { migrate, storeMatch } from './database.mjs';
import { validatePacket } from './validation.mjs';

const config=JSON.parse(await readFile(new URL('../.local-stats/private.json',import.meta.url),'utf8'));
const pool=mysql.createPool({...config.database,connectionLimit:2,connectTimeout:5000,timezone:'Z',multipleStatements:false});
try {
  const [version]=await pool.query('SELECT VERSION() AS version');
  const [tables]=await pool.query('SHOW TABLES');
  console.log(JSON.stringify({version:version[0].version,tables:tables.map(r=>Object.values(r)[0])}));
  if (process.argv.includes('--migrate')) {
    await migrate(pool,new URL('../migrations/',import.meta.url));
    const [catalog]=await pool.query('SELECT kind,COUNT(*) AS count FROM mr_stats_catalog GROUP BY kind');
    console.log('Migration complete',JSON.stringify(catalog));
  }
  if (process.argv.includes('--verify')) {
    const id=randomUUID(), future='stats_test_'+randomUUID().replaceAll('-','');
    const p={schemaVersion:2,matchId:id,sessionId:randomUUID(),modVersion:'integration-test',
      startedAt:'2026-10-04T12:00:00Z',endedAt:'2026-10-04T12:02:00Z',durationSeconds:120,durationTicks:2400,
      winner:0,survivorsCount:1,maniacsCount:1,computersCharged:0,droppedEvents:0,endReason:'integration_test',
      eligibleForBalance:false,qualityFlags:['integration_test'],map:{code:'unknown_0'},settings:{},endSettings:{},performance:{},catalog:[],
      players:[{key:'p1',team:'survivor',lateJoin:false,disconnected:false,
        start:{classCode:'alchemist',classScoreboardId:6,perks:[future]},end:{},metrics:{damage_taken_hp:4},vanillaDelta:{},
        damageTypes:{},itemsUsed:{},perkCounters:{[future]:{attempt_success:1}}}],
      events:[{type:'knockdown',seconds:20,target:'p1',data:{}}]};
    const body=Buffer.from(JSON.stringify(p)), v=validatePacket(p);
    let gameId;
    try {
      const result=await storeMatch(pool,v,'integration-test',body); gameId=result.gameId;
      const duplicate=await storeMatch(pool,v,'integration-test',body);
      if (!duplicate.duplicate || duplicate.gameId!==gameId) throw new Error('idempotency failed');
      const [rows]=await pool.execute('SELECT COUNT(*) AS n FROM mr_stats_player_perks WHERE match_id=?',[gameId]);
      if (Number(rows[0].n)!==1) throw new Error('dynamic catalog linkage failed');
      const conflict={...p,winner:1};
      let rejected=false;
      try { await storeMatch(pool,validatePacket(conflict),'integration-test',Buffer.from(JSON.stringify(conflict))); }
      catch(e) { rejected=e.status===409; }
      if (!rejected) throw new Error('conflicting duplicate accepted');
      const failed={...p,matchId:randomUUID()};
      const brokenPool={getConnection:async()=> {
        const conn=await pool.getConnection();
        return {beginTransaction:()=>conn.beginTransaction(),commit:()=>conn.commit(),rollback:()=>conn.rollback(),release:()=>conn.release(),
          query:(...args)=>conn.query(...args),execute:(sql,...args)=> {
            if(sql.startsWith('INSERT INTO mr_stats_player_perks')) throw new Error('injected failure');
            return conn.execute(sql,...args);
          }};
      }};
      let rolledBack=false;
      try { await storeMatch(brokenPool,validatePacket(failed),'integration-test',Buffer.from(JSON.stringify(failed))); }
      catch { const [absent]=await pool.execute('SELECT id FROM mr_stats_matches WHERE match_uuid=?',[failed.matchId]); rolledBack=absent.length===0; }
      if (!rolledBack) throw new Error('transaction rollback failed');
      console.log('Verified: full insert, dynamic perk, duplicate, conflicting UUID, transaction rollback');
    } finally {
      const conn=await pool.getConnection();
      try {
        await conn.beginTransaction();
        const [matches]=await conn.execute('SELECT id FROM mr_stats_matches WHERE match_uuid=? AND sender=?',[id,'integration-test']);
        if(matches.length) {
          const testId=matches[0].id;
          for(const table of ['mr_stats_events','mr_stats_player_perks','mr_stats_players']) await conn.execute(`DELETE FROM ${table} WHERE match_id=?`,[testId]);
          await conn.execute('DELETE FROM mr_stats_matches WHERE id=?',[testId]);
        }
        await conn.execute('DELETE v FROM mr_stats_catalog_versions v JOIN mr_stats_catalog c ON c.id=v.catalog_id WHERE c.kind=? AND c.code=?',['perk',future]);
        await conn.execute('DELETE FROM mr_stats_catalog WHERE kind=? AND code=?',['perk',future]);
        await conn.commit();
      } catch(e) { await conn.rollback(); throw e; } finally { conn.release(); }
      console.log('Synthetic match removed');
    }
  }
} catch(e) { console.error('Database check failed',e.code??e.name,e.message?.replace(config.database.password,'[redacted]')); process.exitCode=1; }
finally { await pool.end(); }
