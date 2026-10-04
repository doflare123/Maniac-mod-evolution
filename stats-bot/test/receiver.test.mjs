import test from 'node:test';
import assert from 'node:assert/strict';
import { createHandler } from '../receiver.mjs';

const token='x'.repeat(64);
async function request(handler,{auth=token,body='{}',method='POST',url='/v1/matches',extra={}}={}) {
  const req={headers:{authorization:auth===null?undefined:'Bearer '+auth,'content-type':'application/json',...extra},method,url,
    resume(){},destroy(){},async *[Symbol.asyncIterator](){yield Buffer.from(body);}};
  const res={writeHead(status){this.status=status;},end(body){this.body=JSON.parse(body);this.writableEnded=true;}};
  await handler(req,res); return res;
}
test('receiver rejects unauthenticated requests before touching DB',async()=> {
  const handler=createHandler({getConnection(){throw new Error('must not connect');}},{community:token});
  assert.equal((await request(handler,{auth:null})).status,401);
  assert.equal((await request(handler,{auth:'wrong'})).status,401);
});
test('receiver enforces schema, body size, media type and rate limit',async()=> {
  const handler=createHandler({}, {community:token});
  assert.equal((await request(handler)).status,422);
  assert.equal((await request(handler,{body:'{'})).status,400);
  assert.equal((await request(handler,{extra:{'content-length':600000}})).status,413);
  assert.equal((await request(handler,{extra:{'content-type':'text/plain'}})).status,415);
  let last;
  for(let i=0;i<31;i++) last=await request(handler);
  assert.equal(last.status,429);
});
test('only authenticated health endpoint reports DB availability',async()=> {
  const handler=createHandler({query:async()=>[]},{community:token});
  assert.equal((await request(handler,{method:'GET',url:'/v1/health'})).status,200);
  assert.equal((await request(handler,{method:'GET',url:'/anything'})).status,404);
});
