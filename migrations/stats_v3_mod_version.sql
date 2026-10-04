-- MariaDB additive, repeatable migration. Historical version labels are never guessed.
ALTER TABLE mr_stats_matches
  ADD COLUMN IF NOT EXISTS mod_stats_version VARCHAR(96) COLLATE utf8mb4_bin NOT NULL DEFAULT 'unknown' AFTER mod_version;

ALTER TABLE mr_stats_matches
  ADD INDEX IF NOT EXISTS ix_stats_version (eligible_for_balance, mod_stats_version, match_type);

CREATE OR REPLACE VIEW mr_stats_class_balance AS
SELECT c.code, m.mod_version, m.mod_stats_version, m.match_type, p.team,
  COUNT(*) AS picks,
  SUM((p.team='survivor' AND m.winner=0) OR (p.team='maniac' AND m.winner=1)) AS wins,
  AVG(COALESCE(JSON_EXTRACT(p.metrics, '$.damage_dealt_to_players_hp'),0)) AS mean_damage_hp,
  AVG(COALESCE(JSON_EXTRACT(p.metrics, '$.hack_points_contributed'),0)) AS mean_hack_points
FROM mr_stats_players p
JOIN mr_stats_matches m ON m.id=p.match_id
JOIN mr_stats_catalog c ON c.id=p.class_id
WHERE m.eligible_for_balance=1
GROUP BY c.code, m.mod_version, m.mod_stats_version, m.match_type, p.team;

CREATE OR REPLACE VIEW mr_stats_perk_balance AS
SELECT c.code, m.mod_version, m.mod_stats_version, m.match_type, p.team,
  COUNT(*) AS picks,
  SUM((p.team='survivor' AND m.winner=0) OR (p.team='maniac' AND m.winner=1)) AS wins
FROM mr_stats_player_perks pp
JOIN mr_stats_players p ON p.match_id=pp.match_id AND p.player_key=pp.player_key
JOIN mr_stats_matches m ON m.id=pp.match_id
JOIN mr_stats_catalog c ON c.id=pp.perk_id
WHERE m.eligible_for_balance=1 AND pp.selected_at_start=1
GROUP BY c.code, m.mod_version, m.mod_stats_version, m.match_type, p.team;

INSERT IGNORE INTO mr_stats_schema(version) VALUES (3);
