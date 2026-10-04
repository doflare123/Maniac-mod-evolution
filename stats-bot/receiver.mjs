import { createServer } from 'node:https';
import { readFile } from 'node:fs/promises';
import { timingSafeEqual } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import mysql from 'mysql2/promise';
import { InputError, validatePacket } from './validation.mjs';
import { migrate, storeMatch } from './database.mjs';

const MAX_BYTES=512*1024, MAX_ACTIVE=2;
let started;
function authenticate(header,tokens) {
  if (typeof header!=='string' || !header.startsWith('Bearer ') || header.length>256) return null;
  const supplied=Buffer.from(header.slice(7));
  for (const [sender,token] of Object.entries(tokens)) {
    const expected=Buffer.from(token);
    if (supplied.length===expected.length && timingSafeEqual(supplied,expected)) return sender;
  }
  return null;
}
export function createHandler(pool,tokens) {
  let active=0;
  const rates=new Map();
  return async (req,res) => {
    const reply=(status,data)=> {
      if (res.destroyed || res.writableEnded) return;
      res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store','Connection':'close'});
      res.end(JSON.stringify(data));
    };
    const sender=authenticate(req.headers.authorization,tokens);
    if (!sender) { req.resume(); reply(401,{error:'unauthorized'}); return; }
    if (req.method==='GET' && req.url==='/v1/health') {
      try { await pool.query('SELECT 1'); reply(200,{ok:true,schemaVersion:2}); }
      catch { reply(503,{error:'database unavailable'}); }
      return;
    }
    if (req.method!=='POST' || req.url!=='/v1/matches') { req.resume(); reply(404,{error:'not found'}); return; }
    if (req.headers['content-type']?.split(';')[0].trim()!=='application/json' || req.headers['content-encoding']) {
      req.resume(); reply(415,{error:'application/json required'}); return;
    }
    const now=Date.now();
    let rate=rates.get(sender);
    if (!rate || now-rate.start>=60000) { rate={start:now,count:0}; rates.set(sender,rate); }
    if (++rate.count>30 || active>=MAX_ACTIVE) { req.resume(); reply(429,{error:'retry later'}); return; }
    if (Number(req.headers['content-length'])>MAX_BYTES) { req.resume(); reply(413,{error:'packet too large'}); return; }
    active++;
    try {
      const chunks=[]; let bytes=0;
      for await (const chunk of req) {
        bytes+=chunk.length;
        if (bytes>MAX_BYTES) { reply(413,{error:'packet too large'}); req.destroy(); return; }
        chunks.push(chunk);
      }
      const body=Buffer.concat(chunks);
      let p;
      try { p=JSON.parse(body.toString('utf8')); } catch { throw new InputError('invalid JSON',400); }
      const validated=validatePacket(p);
      const result=await storeMatch(pool,validated,sender,body);
      reply(result.duplicate ? 200 : 201,result);
    } catch (e) {
      if (e instanceof InputError) reply(e.status,{error:e.message});
      else { console.error('[ManiacrevStats] Store failed',e.code??e.name); reply(503,{error:'storage unavailable'}); }
    } finally { active--; }
  };
}

async function start() {
  const configPath=process.env.MANIACREV_STATS_CONFIG || fileURLToPath(new URL('./private.json',import.meta.url));
  const config=JSON.parse(await readFile(configPath,'utf8'));
  if (!config.enabled) return null;
  if (!config.tokens || Object.keys(config.tokens).length===0 || Object.entries(config.tokens).some(([sender,token])=> !/^[a-z0-9_-]{1,64}$/.test(sender) || typeof token!=='string' || token.length<32 || token.length>200)) throw new Error('Invalid ingest tokens');
  const pool=mysql.createPool({...config.database,waitForConnections:true,connectionLimit:2,queueLimit:4,connectTimeout:5000,
    enableKeepAlive:true,multipleStatements:false,timezone:'Z',charset:'utf8mb4'});
  try {
    await migrate(pool,new URL('./migrations/',import.meta.url));
    const server=createServer({key:await readFile(new URL('./receiver-key.pem',import.meta.url)),
      cert:await readFile(new URL('./receiver-cert.pem',import.meta.url)),minVersion:'TLSv1.2'},createHandler(pool,config.tokens));
    server.requestTimeout=15000; server.headersTimeout=10000; server.keepAliveTimeout=1000;
    server.maxConnections=16; server.setTimeout(15000,socket=>socket.destroy());
    await new Promise((resolve,reject)=> {
      server.once('error',reject); server.listen(config.port??25912,'0.0.0.0',resolve);
    });
    server.on('error',e=>console.error('[ManiacrevStats] Listener error',e.code??e.name));
    console.log('[ManiacrevStats] HTTPS receiver ready; schema 2; port',config.port??25912);
    return {server,pool,close:async()=>{ await new Promise(resolve=>server.close(resolve)); await pool.end(); }};
  } catch(e) { await pool.end(); throw e; }
}
export function startStatsReceiver() {
  // Never prevent Markov's Telegram polling when the database/receiver is unavailable.
  if (!started) started=start().catch(e=> {
    console.error('[ManiacrevStats] Receiver startup failed',e.code??e.name);
    started=null; return null;
  });
  return started;
}
