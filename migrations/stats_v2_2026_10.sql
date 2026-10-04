-- MariaDB/MySQL, additive and repeatable. Legacy games/ref_* tables are preserved.
CREATE TABLE IF NOT EXISTS mr_stats_schema (
  version INT NOT NULL PRIMARY KEY,
  applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mr_stats_catalog (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  code VARCHAR(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  name VARCHAR(160) NOT NULL,
  team VARCHAR(16) NOT NULL DEFAULT 'all',
  first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_catalog_code (kind, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mr_stats_catalog_versions (
  catalog_id INT UNSIGNED NOT NULL,
  mod_version VARCHAR(96) NOT NULL,
  name VARCHAR(160) NOT NULL,
  metadata JSON NOT NULL,
  PRIMARY KEY (catalog_id, mod_version),
  FOREIGN KEY (catalog_id) REFERENCES mr_stats_catalog(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mr_stats_matches (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  match_uuid CHAR(36) CHARACTER SET ascii NOT NULL,
  session_uuid CHAR(36) CHARACTER SET ascii NOT NULL,
  sender VARCHAR(64) NOT NULL,
  schema_version INT NOT NULL,
  mod_version VARCHAR(96) NOT NULL,
  match_type VARCHAR(16) NOT NULL,
  started_at DATETIME(3) NOT NULL,
  ended_at DATETIME(3) NOT NULL,
  duration_seconds DOUBLE NOT NULL,
  duration_ticks INT NOT NULL,
  winner TINYINT NOT NULL,
  survivors_count INT NOT NULL,
  maniacs_count INT NOT NULL,
  recorded_players INT NOT NULL,
  eligible_for_balance BOOLEAN NOT NULL,
  end_reason VARCHAR(64) NOT NULL,
  quality_flags JSON NOT NULL,
  map_code VARCHAR(96) NOT NULL,
  map_snapshot JSON NOT NULL,
  settings JSON NOT NULL,
  end_settings JSON NOT NULL,
  performance JSON NOT NULL,
  computers_charged INT NOT NULL,
  dropped_events INT NOT NULL,
  body_sha256 CHAR(64) CHARACTER SET ascii NOT NULL,
  raw_packet_gzip MEDIUMBLOB NOT NULL,
  received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_match_uuid (match_uuid),
  KEY ix_balance (eligible_for_balance, match_type, mod_version),
  KEY ix_map_time (map_code, started_at),
  KEY ix_session (session_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mr_stats_players (
  match_id INT UNSIGNED NOT NULL,
  player_key VARCHAR(16) CHARACTER SET ascii NOT NULL,
  team VARCHAR(16) NOT NULL,
  class_id INT UNSIGNED NOT NULL,
  late_join BOOLEAN NOT NULL,
  disconnected BOOLEAN NOT NULL,
  start_snapshot JSON NOT NULL,
  end_snapshot JSON NOT NULL,
  metrics JSON NOT NULL,
  vanilla_delta JSON NOT NULL,
  damage_types JSON NOT NULL,
  items_used JSON NOT NULL,
  PRIMARY KEY (match_id, player_key),
  KEY ix_class (class_id, team),
  FOREIGN KEY (match_id) REFERENCES mr_stats_matches(id),
  FOREIGN KEY (class_id) REFERENCES mr_stats_catalog(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mr_stats_player_perks (
  match_id INT UNSIGNED NOT NULL,
  player_key VARCHAR(16) CHARACTER SET ascii NOT NULL,
  slot INT NOT NULL,
  perk_id INT UNSIGNED NOT NULL,
  selected_at_start BOOLEAN NOT NULL,
  counters JSON NOT NULL,
  PRIMARY KEY (match_id, player_key, perk_id),
  KEY ix_perk (perk_id),
  FOREIGN KEY (match_id, player_key) REFERENCES mr_stats_players(match_id, player_key),
  FOREIGN KEY (perk_id) REFERENCES mr_stats_catalog(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mr_stats_events (
  match_id INT UNSIGNED NOT NULL,
  sequence_no INT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  elapsed_seconds DOUBLE NOT NULL,
  actor_key VARCHAR(16) NULL,
  target_key VARCHAR(16) NULL,
  event_data JSON NOT NULL,
  PRIMARY KEY (match_id, sequence_no),
  KEY ix_event (event_type, match_id),
  FOREIGN KEY (match_id) REFERENCES mr_stats_matches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW mr_stats_class_balance AS
SELECT c.code, m.mod_version, m.match_type, p.team,
  COUNT(*) AS picks,
  SUM((p.team='survivor' AND m.winner=0) OR (p.team='maniac' AND m.winner=1)) AS wins,
  AVG(COALESCE(JSON_EXTRACT(p.metrics, '$.damage_dealt_to_players_hp'),0)) AS mean_damage_hp,
  AVG(COALESCE(JSON_EXTRACT(p.metrics, '$.hack_points_contributed'),0)) AS mean_hack_points
FROM mr_stats_players p
JOIN mr_stats_matches m ON m.id=p.match_id
JOIN mr_stats_catalog c ON c.id=p.class_id
WHERE m.eligible_for_balance=1
GROUP BY c.code, m.mod_version, m.match_type, p.team;

CREATE OR REPLACE VIEW mr_stats_perk_balance AS
SELECT c.code, m.mod_version, m.match_type, p.team,
  COUNT(*) AS picks,
  SUM((p.team='survivor' AND m.winner=0) OR (p.team='maniac' AND m.winner=1)) AS wins
FROM mr_stats_player_perks pp
JOIN mr_stats_players p ON p.match_id=pp.match_id AND p.player_key=pp.player_key
JOIN mr_stats_matches m ON m.id=pp.match_id
JOIN mr_stats_catalog c ON c.id=pp.perk_id
WHERE m.eligible_for_balance=1 AND pp.selected_at_start=1
GROUP BY c.code, m.mod_version, m.match_type, p.team;

INSERT IGNORE INTO mr_stats_schema(version) VALUES (2);
