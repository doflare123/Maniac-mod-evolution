import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { gzipSync } from 'node:zlib';
import { InputError } from './validation.mjs';

const json=v=>JSON.stringify(v);
const sqlDate=value=>new Date(value).toISOString().replace('T',' ').replace('Z','');
export async function migrate(pool, directory=new URL('../migrations/',import.meta.url)) {
  for (const file of ['stats_v2_2026_10.sql','stats_v3_mod_version.sql']) {
    const sql=await readFile(new URL(file,directory),'utf8');
    // Controlled migration file, no multiStatements enabled on the connection.
    for (const statement of sql.replace(/^--.*$/gm,'').split(';').map(s=>s.trim()).filter(Boolean)) await pool.query(statement);
  }
  const seed=JSON.parse(await readFile(new URL('catalog-v2.json',import.meta.url),'utf8'));
  for (const c of seed) await pool.execute('INSERT INTO mr_stats_catalog(kind,code,name,team) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE code=VALUES(code)',[c.kind,c.code,c.name,c.team]);
  // Bring historical codes into the new catalog without changing legacy IDs or matches.
  for (const [table,kind] of [['ref_perks','perk'],['ref_classes','class']]) {
    const [exists]=await pool.execute('SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?',[table]);
    if (exists.length) await pool.query(`INSERT INTO mr_stats_catalog(kind,code,name,team) SELECT ?,name,name,'all' FROM ${table} WHERE name REGEXP '^[a-z0-9_.:-]{1,96}$' ON DUPLICATE KEY UPDATE code=VALUES(code)`,[kind]);
  }
}

export async function storeMatch(pool, validated, sender, body) {
  const {packet:p,flags,eligible,matchType}=validated;
  const hash=createHash('sha256').update(body).digest('hex');
  const conn=await pool.getConnection();
  try {
    await conn.beginTransaction();
    let gameId;
    try {
      const [insert]=await conn.execute(`INSERT INTO mr_stats_matches
        (match_uuid,session_uuid,sender,schema_version,mod_version,mod_stats_version,match_type,started_at,ended_at,duration_seconds,duration_ticks,
         winner,survivors_count,maniacs_count,recorded_players,eligible_for_balance,end_reason,quality_flags,map_code,map_snapshot,
         settings,end_settings,performance,computers_charged,dropped_events,body_sha256,raw_packet_gzip)
        VALUES (${Array(27).fill('?').join(',')})`,
        [p.matchId,p.sessionId,sender,2,p.modVersion,p.modStatsVersion??'unknown',matchType,sqlDate(p.startedAt),sqlDate(p.endedAt),p.durationSeconds,p.durationTicks,
          p.winner,p.survivorsCount,p.maniacsCount,p.players.length,eligible,p.endReason,json(flags),p.map.code,json(p.map),
          json(p.settings),json(p.endSettings),json(p.performance),p.computersCharged,p.droppedEvents,hash,gzipSync(body)]);
      gameId=insert.insertId;
    } catch (e) {
      if (e.code!=='ER_DUP_ENTRY') throw e;
      const [rows]=await conn.execute('SELECT id,body_sha256,sender FROM mr_stats_matches WHERE match_uuid=?',[p.matchId]);
      if (rows.length && rows[0].body_sha256===hash && rows[0].sender===sender) {
        await conn.rollback(); return {gameId:rows[0].id,duplicate:true};
      }
      throw new InputError('match UUID already has different data',409);
    }
    const catalog=new Map(p.catalog.map(c=>[c.kind+':'+c.code,c]));
    for (const player of p.players) {
      const code=player.start.classCode;
      if (!catalog.has('class:'+code)) catalog.set('class:'+code,{kind:'class',code,name:code,team:player.team,metadata:{scoreboardId:player.start.classScoreboardId}});
      for (const code of new Set([...player.start.perks,...Object.keys(player.perkCounters)])) {
        if (!catalog.has('perk:'+code)) catalog.set('perk:'+code,{kind:'perk',code,name:code,team:'all',metadata:{}});
      }
    }
    // Deterministic order also avoids catalog row lock inversions between concurrent matches.
    const entries=[...catalog.values()].sort((a,b)=>(a.kind+':'+a.code).localeCompare(b.kind+':'+b.code));
    const ids=new Map();
    for (const c of entries) {
      await conn.execute(`INSERT INTO mr_stats_catalog(kind,code,name,team) VALUES (?,?,?,?)
        ON DUPLICATE KEY UPDATE last_seen_at=CURRENT_TIMESTAMP`,[c.kind,c.code,c.name,c.team]);
      const [rows]=await conn.execute('SELECT id FROM mr_stats_catalog WHERE kind=? AND code=?',[c.kind,c.code]);
      ids.set(c.kind+':'+c.code,rows[0].id);
      await conn.execute(`INSERT INTO mr_stats_catalog_versions(catalog_id,mod_version,name,metadata) VALUES (?,?,?,?)
        ON DUPLICATE KEY UPDATE mod_version=VALUES(mod_version)`,[rows[0].id,p.modVersion,c.name,json(c.metadata)]);
    }
    for (const player of p.players) {
      await conn.execute(`INSERT INTO mr_stats_players
        (match_id,player_key,team,class_id,late_join,disconnected,start_snapshot,end_snapshot,metrics,vanilla_delta,damage_types,items_used)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)`,[gameId,player.key,player.team,ids.get('class:'+player.start.classCode),player.lateJoin,player.disconnected,
        json(player.start),json(player.end),json(player.metrics),json(player.vanillaDelta),json(player.damageTypes),json(player.itemsUsed)]);
      for (const code of new Set([...player.start.perks,...Object.keys(player.perkCounters)])) {
        await conn.execute('INSERT INTO mr_stats_player_perks(match_id,player_key,slot,perk_id,selected_at_start,counters) VALUES (?,?,?,?,?,?)',
          [gameId,player.key,player.start.perks.indexOf(code),ids.get('perk:'+code),player.start.perks.includes(code),json(player.perkCounters[code]??{})]);
      }
    }
    for (let offset=0;offset<p.events.length;offset+=100) {
      const rows=p.events.slice(offset,offset+100).map((e,i)=>[gameId,offset+i,e.type,e.seconds,e.actor??null,e.target??null,json(e.data)]);
      await conn.query('INSERT INTO mr_stats_events(match_id,sequence_no,event_type,elapsed_seconds,actor_key,target_key,event_data) VALUES ?',[rows]);
    }
    await conn.commit(); return {gameId,duplicate:false};
  } catch (e) { await conn.rollback(); throw e; }
  finally { conn.release(); }
}
