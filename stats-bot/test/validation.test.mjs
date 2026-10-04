import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { validatePacket } from '../validation.mjs';

export function packet() {
  return {schemaVersion:2,matchId:randomUUID(),sessionId:randomUUID(),modVersion:'integration-test',modStatsVersion:'balance-1',
    startedAt:'2026-10-04T12:00:00Z',endedAt:'2026-10-04T12:02:00Z',durationSeconds:120,durationTicks:2400,
    winner:0,survivorsCount:1,maniacsCount:1,computersCharged:2,droppedEvents:0,endReason:'result_reported',
    eligibleForBalance:true,qualityFlags:[],map:{code:'unknown_0'},settings:{},endSettings:{},performance:{},catalog:[],
    players:[{key:'p1',team:'survivor',lateJoin:false,disconnected:false,
      start:{classCode:'alchemist',classScoreboardId:6,perks:['sber_sprout']},end:{},metrics:{damage_taken_hp:4},vanillaDelta:{},
      damageTypes:{},itemsUsed:{},perkCounters:{sber_sprout:{attempt_success:1}}},
      {key:'p2',team:'maniac',lateJoin:false,disconnected:false,
      start:{classCode:'pudge',classScoreboardId:5,perks:['high_voltage']},end:{},metrics:{},vanillaDelta:{},damageTypes:{},itemsUsed:{},perkCounters:{}}],
    events:[{type:'knockdown',seconds:20,actor:'p2',target:'p1',data:{}}]};
}
test('new stable perk codes require no numeric mapping',()=> {
  const p=packet(); p.players[0].start.perks=['future_perk'];
  assert.equal(validatePacket(p).matchType,'duel');
});
test('partial/short games cannot enter balance views',()=> {
  const p=packet(); p.players.pop(); p.events=[]; p.durationSeconds=30; p.endedAt='2026-10-04T12:00:30Z';
  const r=validatePacket(p); assert.equal(r.eligible,false); assert.ok(r.flags.includes('partial_roster')); assert.ok(r.flags.includes('short_match'));
});
test('event references must point to anonymous participants',()=> {
  const p=packet(); p.events[0].target='removed'; assert.throws(()=>validatePacket(p));
});
test('limits, invalid numbers and accidental identity are rejected',()=> {
  for(const change of [p=>p.players[0].metrics.hp=Infinity,p=>p.players[0].uuid=randomUUID(),p=>p.events=Array(513).fill(p.events[0]),p=>p.schemaVersion=99]) {
    const p=packet(); change(p); assert.throws(()=>validatePacket(p));
  }
});
test('duplicate participant keys are rejected',()=> {
  const p=packet(); p.players[1].key='p1'; assert.throws(()=>validatePacket(p));
});

test('stats build labels are independent and legacy packets remain accepted',()=> {
  const p=packet(); assert.equal(validatePacket(p).packet.modStatsVersion,'balance-1');
  delete p.modStatsVersion; assert.doesNotThrow(()=>validatePacket(p));
  for (const invalid of ['', 'x'.repeat(97), 'with space', '\n', 2, null]) {
    p.modStatsVersion=invalid; assert.throws(()=>validatePacket(p));
  }
});
