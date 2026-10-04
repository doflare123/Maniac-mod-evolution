import { readFile } from 'node:fs/promises';
import { request } from 'node:https';
const config=JSON.parse(await readFile(new URL('../.local-stats/private.json',import.meta.url),'utf8'));
const ca=await readFile(new URL('../.local-stats/receiver-cert.pem',import.meta.url));
const token=Object.values(config.tokens)[0];
await new Promise((resolve,reject)=> {
  const req=request('https://5.83.140.208:25912/v1/health',{ca,headers:{Authorization:'Bearer '+token},timeout:10000},res=> {
    let body=''; res.setEncoding('utf8'); res.on('data',chunk=>body+=chunk);
    res.on('end',()=>{ console.log('Receiver health',res.statusCode,body); if(res.statusCode===200) resolve(); else reject(new Error('Receiver unavailable')); });
  });
  req.on('error',reject);req.on('timeout',()=>req.destroy(new Error('timeout')));req.end();
});
