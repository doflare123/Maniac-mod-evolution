const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const codePattern = /^[a-z0-9_.:-]{1,96}$/;
const playerPattern = /^p[1-9][0-9]{0,3}$/;
export class InputError extends Error { constructor(message, status=422) { super(message); this.status=status; } }
function check(condition, message) { if (!condition) throw new InputError(message); }
function object(value) { return value !== null && typeof value === 'object' && !Array.isArray(value); }
function code(value) { return typeof value === 'string' && codePattern.test(value); }
function number(value, max=1e12) { return typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= max; }
function counters(value) {
  check(object(value) && Object.keys(value).length <= 96, 'invalid counters');
  for (const [key, n] of Object.entries(value)) check(code(key) && number(n), 'invalid counter');
}
function snapshot(value, initial) {
  check(object(value), 'invalid snapshot');
  if (!initial && Object.keys(value).length === 0) return;
  check(code(value.classCode) && Number.isInteger(value.classScoreboardId), 'invalid class');
  check(Array.isArray(value.perks) && value.perks.length <= 16 && value.perks.every(code), 'invalid loadout');
}
export function validatePacket(p) {
  const pending=[{value:p,depth:0}];
  while(pending.length) {
    const {value,depth}=pending.pop();
    check(depth<=10,'packet nesting too deep');
    if (typeof value==='string') check(value.length<=4096,'string too long');
    if (value && typeof value==='object') {
      const entries=Object.entries(value);
      check(entries.length<=(Array.isArray(value)?512:256),'object too large');
      for(const [key,child] of entries) { check(key.length<=160,'key too long'); pending.push({value:child,depth:depth+1}); }
    }
  }
  check(object(p) && p.schemaVersion === 2, 'unsupported schema');
  check(uuid.test(p.matchId) && uuid.test(p.sessionId), 'invalid match/session UUID');
  check(typeof p.modVersion === 'string' && p.modVersion.length > 0 && p.modVersion.length <= 96, 'invalid mod version');
  // Older v2 senders remain accepted, but their balance version stays explicitly unknown.
  check(p.modStatsVersion === undefined || (typeof p.modStatsVersion === 'string' && /^[A-Za-z0-9][A-Za-z0-9_.-]{0,95}$/.test(p.modStatsVersion)), 'invalid mod stats version');
  check([-1,0,1].includes(p.winner), 'invalid winner');
  check(number(p.durationSeconds, 604800) && Number.isInteger(p.durationTicks) && number(p.durationTicks, 12096000), 'invalid duration');
  for (const k of ['survivorsCount','maniacsCount','computersCharged','droppedEvents']) check(Number.isInteger(p[k]) && number(p[k], k.endsWith('Count') ? 128 : 1e9), 'invalid count');
  check(p.survivorsCount + p.maniacsCount <= 128, 'roster too large');
  const start=Date.parse(p.startedAt), end=Date.parse(p.endedAt);
  check(typeof p.startedAt === 'string' && typeof p.endedAt === 'string' && Number.isFinite(start) && Number.isFinite(end) && end >= start, 'invalid timestamps');
  check(Math.abs((end-start)/1000-p.durationSeconds) <= 300, 'duration/timestamps inconsistent');
  check(Array.isArray(p.qualityFlags) && p.qualityFlags.length <= 32 && p.qualityFlags.every(code), 'invalid quality flags');
  check(code(p.endReason) && typeof p.eligibleForBalance === 'boolean', 'invalid quality');
  check(object(p.map) && code(p.map.code), 'invalid map');
  check(object(p.settings) && object(p.endSettings) && object(p.performance), 'invalid settings/performance');
  check(Array.isArray(p.catalog) && p.catalog.length <= 256, 'catalog too large');
  const catalogKeys=new Set();
  for (const entry of p.catalog) {
    check(object(entry) && ['perk','class'].includes(entry.kind) && code(entry.code), 'invalid catalog code');
    check(typeof entry.name === 'string' && entry.name.length <= 160 && ['all','survivor','maniac'].includes(entry.team), 'invalid catalog metadata');
    check(object(entry.metadata), 'invalid catalog metadata');
    const key=entry.kind+':'+entry.code; check(!catalogKeys.has(key),'duplicate catalog entry'); catalogKeys.add(key);
  }
  check(Array.isArray(p.players) && p.players.length <= 128, 'invalid players');
  const keys=new Set();
  for (const player of p.players) {
    check(object(player) && playerPattern.test(player.key) && !keys.has(player.key), 'invalid/duplicate player key'); keys.add(player.key);
    check(['survivor','maniac'].includes(player.team) && typeof player.lateJoin==='boolean' && typeof player.disconnected==='boolean', 'invalid participant');
    snapshot(player.start, true); snapshot(player.end, false);
    check(new Set(player.start.perks).size === player.start.perks.length, 'duplicate selected perk');
    for (const field of ['metrics','vanillaDelta','damageTypes','itemsUsed']) counters(player[field]);
    check(object(player.perkCounters) && Object.keys(player.perkCounters).length <= 64, 'invalid perk counters');
    for (const [perk, counts] of Object.entries(player.perkCounters)) { check(code(perk),'invalid perk'); counters(counts); }
    // Identity is deliberately match-local. Reject accidental names, UUIDs and chat data.
    check(!('uuid' in player) && !('name' in player) && !('playerUuid' in player), 'identifying player data is unsupported');
  }
  check(Array.isArray(p.events) && p.events.length <= 512, 'too many events');
  for (const e of p.events) {
    check(object(e) && code(e.type) && number(e.seconds,p.durationSeconds+1) && object(e.data), 'invalid event');
    check((e.actor===undefined || keys.has(e.actor)) && (e.target===undefined || keys.has(e.target)), 'unknown event participant');
  }
  const flags=[...p.qualityFlags];
  if (p.players.some(p=>p.lateJoin) && !flags.includes('late_join')) flags.push('late_join');
  if (p.players.some(p=>p.disconnected) && !flags.includes('participant_disconnected')) flags.push('participant_disconnected');
  if (p.durationSeconds<60 && !flags.includes('short_match')) flags.push('short_match');
  if ((!p.survivorsCount || !p.maniacsCount) && !flags.includes('missing_team')) flags.push('missing_team');
  if (p.players.length!==p.survivorsCount+p.maniacsCount && !flags.includes('partial_roster')) flags.push('partial_roster');
  return { packet:p, flags, eligible:p.eligibleForBalance && p.winner>=0 && flags.length===0,
    matchType:p.survivorsCount===1 && p.maniacsCount===1 ? 'duel' : 'group' };
}
