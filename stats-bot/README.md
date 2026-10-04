# Maniacrev statistics v2

Hidden HTTPS receiver used by the Markov bot. No Telegram commands, messages or chat listeners are added.

Deploy `receiver.mjs`, `database.mjs`, `validation.mjs`, `catalog-v2.json`, the `migrations` directory,
`receiver-cert.pem`, `receiver-key.pem` and `private.json` in `/home/container/maniacrev-stats`.
The existing container already supplies `mysql2`. In `startMarkovBot()` call
`void startStatsReceiver()` after importing it from `./maniacrev-stats/receiver.mjs`.
Listener/database failures do not stop Telegram polling. The receiver applies additive migrations at startup.
Keep the private config and TLS key outside Git; never put database credentials in the mod.

The HTTPS endpoint is `https://5.83.140.208:25912/v1/matches`. The mod checks the receiver's SHA-256
certificate pin in `src/main/resources/stats-receiver.sha256`, certificate validity and IP hostname.
The certificate expires in October 2028; renewing a certificate requires distributing its new pin.
No certificate validation is disabled.

The release JAR contains its sender settings and starts collecting/uploading automatically on
dedicated and integrated servers. Players install only the mod and map; no separate config is needed.
`processResources` bundles the maintainer's existing `.local-stats/maniacrev-stats.properties`:

```properties
enabled=true
endpoint=https://5.83.140.208:25912/v1/matches
token=YOUR_INGEST_TOKEN
```

The ingestion token is intentionally bundled in the public JAR. Database credentials and TLS private key
remain only on the bot. The token is extractable and cannot prove that a match was genuine;
the receiver treats submissions as reported results, not authoritative gameplay.
External config files and environment tokens are not read. Setting `enabled=false` in the build's
sender settings disables collection/upload. `/maniacrev offsendstats` persists per player,
survives respawn and excludes that participant's individual records from current/future packets.
Total team counts remain aggregated; packets contain no player names, player UUIDs, chat, IPs or full item NBT.

## Data and overhead

### Statistics/balance build version

Set `mod_stats_version=1` in `gradle.properties`, or build with
`./gradlew.bat jar -Pmod_stats_version=balance-2`. Increase this label after balance changes.
It is baked into the JAR, sent as `modStatsVersion` and stored in
`mr_stats_matches.mod_stats_version`. Both balance views group by this label as well as the
existing mod version, so different balance builds do not mix. Minecraft version remains in
the raw packet as `minecraftVersion`. Old packets/rows without this label use `unknown`.
This build parameter is independent of the bundled sender token.

Example: `SELECT * FROM mr_stats_perk_balance WHERE mod_stats_version='1';`

- Starting roster and loadouts survive spectator/team changes and cleared end-of-game perks.
- Start/end snapshots: classes, perks, health/max health/absorption, food, mana, progression,
  six equipment slots, active effects, numeric scoreboard values and built-in stat deltas.
- Event counters: final damage capped to remaining HP, hit counts, damage sources, deaths/kills
  (including existing plague/nightmare attribution), knockdowns, revives and finished item use.
- Hacking: real contributed points, hacker/support time, started/interrupted/completed sessions,
  QTE results/bonus points, and Sprout extracted/preserved/planted progress.
- Perks: active attempt outcomes, generic passive-cooldown triggers, granted/consumed/expired charges,
  Go Next triggers. Passive mechanics bypassing these central paths need a specific hook for a semantic
  trigger count; their damage/healing/hacking outcomes and loadouts are still recorded.
- Nightmares: maze/arena/fear-race starts and results. Armor ability requests are counted as attempts,
  not successes. Mana counters explicitly distinguish perk costs from ManaUtil changes; direct capability
  edits are represented by snapshots and samples, not falsely labelled as complete mana accounting.
- One sample per 20 server ticks: health, food, mana, ping, sampled movement/state/phase time,
  server mean/max MSPT and samples above 50 ms. These are tick samples, not exact wall-clock lifetimes;
  distance sampling ignores dimension changes and movements over 32 blocks. No client rendering hooks.
- Max 128 tracked participants, 96 counter categories (excess folded into `other`), 64 perk counters
  per participant and 512 significant timeline events per match. Overflow is reported explicitly.
- Network, JSON serialization and disk IO use one worker. Queue: eight in-memory jobs, 200 pending
  files, max 512 KiB per packet, up to five retries per batch and exponential backoff up to 15 minutes.
  Queue overflow reports an error instead of silently pretending to save. Rejected packets are kept
  as `.json.rejected`; review/remove them manually. Abrupt process termination can lose an active match
  or jobs not yet persisted; the outbox survives normal network failures and subsequent launches.
- Receiver: two concurrent submissions, two DB connections, bounded DB wait queue, 512 KiB request limit,
  30 submissions per minute per sender and connection/request timeouts. Only transaction commit produces
  a success acknowledgement. UUID + body hash provide idempotency; different content for the same UUID is rejected.

No TPS/FPS benchmark is claimed: measure a real match on the target machine. The sampler reads tracked
players only; it does not scan world entities, chunks or full registries each tick.

## Database and migration

`migrations/stats_v2_2026_10.sql` creates `mr_stats_*` tables and views without changing any legacy table.
`catalog-v2.json` seeds the current 44 perks and 15 classes; runtime submissions automatically register
new stable codes and their version metadata. Adding a gameplay entity does not require another SQL ID map.
Historical `ref_perks`/`ref_classes` codes are also imported into the v2 catalog.

Old matches stay in `games`/`duel_games`; they cannot acquire per-player detail retroactively and are not
silently mixed into v2 balance views. Use old queries for legacy history and `mr_stats_class_balance` /
`mr_stats_perk_balance` for complete v2 matches, grouped by mod version/team/group or duel.
Short, partial, disconnected, changed-team and unfinished matches are retained with quality flags and
excluded from balance. Raw packets are gzip-compressed for future reprocessing; the normalized tables
support queries without decompressing packets. Catalog numeric IDs are private DB implementation details.

Local checks: `npm install --prefix stats-bot`, `npm test --prefix stats-bot`, Java `compileJava` and
`StatsMetricsTest`. Operator-only `node stats-bot/db-check.mjs --migrate --verify` uses the ignored local
`.local-stats/private.json`: verifies insertion, new codes, identical/conflicting duplicates and rollback,
then removes its uniquely identified synthetic match. It is not deployed or exposed over HTTP.
