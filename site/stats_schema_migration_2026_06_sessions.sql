-- Migration for Maniac Revolution stats schema.
-- Target DB: MariaDB/MySQL, current schema from s5893_minecraft_stats.sql.
--
-- Rules represented by this schema:
--   * Every stored match belongs to a game session.
--   * Main `games` stores only relevant non-1v1 matches.
--   * 1v1 matches are stored separately in `duel_games`.
--   * Matches shorter than 60 seconds are not valid stats rows.
--   * Matches where someone left should not be inserted into stats tables.

START TRANSACTION;

CREATE TABLE IF NOT EXISTS `game_sessions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `started_at` datetime NOT NULL DEFAULT current_timestamp(),
  `ended_at` datetime NULL DEFAULT NULL,
  `source` enum('server_start','first_world_join','manual') NOT NULL DEFAULT 'first_world_join',
  `note` varchar(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_game_sessions_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `game_session_players` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` int(11) NOT NULL,
  `player_uuid` char(36) NOT NULL,
  `player_name` varchar(64) NOT NULL,
  `first_seen_at` datetime NOT NULL DEFAULT current_timestamp(),
  `last_seen_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_player_uuid` (`session_id`, `player_uuid`),
  KEY `idx_game_session_players_name` (`player_name`),
  CONSTRAINT `game_session_players_session_fk`
    FOREIGN KEY (`session_id`) REFERENCES `game_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- One legacy session for already existing rows.
INSERT INTO `game_sessions` (`id`, `started_at`, `source`, `note`)
VALUES (1, current_timestamp(), 'manual', 'Legacy session for rows created before session tracking')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- Main table: only matches with more than 2 total players.
ALTER TABLE `games`
  ADD COLUMN IF NOT EXISTS `session_id` int(11) NULL DEFAULT NULL AFTER `id`,
  ADD COLUMN IF NOT EXISTS `started_at` datetime NULL DEFAULT NULL AFTER `played_at`,
  ADD COLUMN IF NOT EXISTS `ended_at` datetime NULL DEFAULT NULL AFTER `started_at`,
  ADD COLUMN IF NOT EXISTS `duration_seconds` int(11) NULL DEFAULT NULL AFTER `ended_at`,
  ADD COLUMN IF NOT EXISTS `last_survivor_standing` tinyint(1) NOT NULL DEFAULT 0
    COMMENT '1 = at match end only one survivor from survivor team was alive/active' AFTER `maniacs_count`,
  ADD COLUMN IF NOT EXISTS `last_survivor_standing_won` tinyint(1) NOT NULL DEFAULT 0
    COMMENT '1 = last_survivor_standing happened and survivors won' AFTER `last_survivor_standing`;

UPDATE `games`
SET `session_id` = 1
WHERE `session_id` IS NULL;

ALTER TABLE `games`
  MODIFY `session_id` int(11) NOT NULL;

CREATE INDEX IF NOT EXISTS `idx_games_session_id` ON `games` (`session_id`);
CREATE INDEX IF NOT EXISTS `idx_games_played_at` ON `games` (`played_at`);
CREATE INDEX IF NOT EXISTS `idx_games_player_count` ON `games` (`survivors_count`, `maniacs_count`);
CREATE INDEX IF NOT EXISTS `idx_games_duration_seconds` ON `games` (`duration_seconds`);

ALTER TABLE `games`
  ADD CONSTRAINT `games_session_fk`
    FOREIGN KEY (`session_id`) REFERENCES `game_sessions` (`id`) ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS `duel_games` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` int(11) NOT NULL,
  `played_at` datetime NOT NULL DEFAULT current_timestamp(),
  `started_at` datetime NULL DEFAULT NULL,
  `ended_at` datetime NULL DEFAULT NULL,
  `duration_seconds` int(11) NULL DEFAULT NULL,
  `winner` tinyint(4) NOT NULL COMMENT '0 = survivors, 1 = maniacs',
  `survivors_count` int(11) NOT NULL DEFAULT 1,
  `maniacs_count` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_duel_games_session_id` (`session_id`),
  KEY `idx_duel_games_played_at` (`played_at`),
  KEY `idx_duel_games_duration_seconds` (`duration_seconds`),
  CONSTRAINT `duel_games_session_fk`
    FOREIGN KEY (`session_id`) REFERENCES `game_sessions` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_duel_games_counts`
    CHECK (`survivors_count` = 1 AND `maniacs_count` = 1),
  CONSTRAINT `chk_duel_games_min_duration`
    CHECK (`duration_seconds` IS NULL OR `duration_seconds` >= 60),
  CONSTRAINT `chk_duel_games_winner`
    CHECK (`winner` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `duel_game_classes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `duel_game_id` int(11) NOT NULL,
  `team` tinyint(4) NOT NULL COMMENT '0 = survivors, 1 = maniacs',
  `class_id` int(11) NOT NULL,
  `count` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_duel_game_classes_game_id` (`duel_game_id`),
  KEY `idx_duel_game_classes_class_id` (`class_id`),
  CONSTRAINT `duel_game_classes_game_fk`
    FOREIGN KEY (`duel_game_id`) REFERENCES `duel_games` (`id`) ON DELETE CASCADE,
  CONSTRAINT `duel_game_classes_class_fk`
    FOREIGN KEY (`class_id`) REFERENCES `ref_classes` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_duel_game_classes_team`
    CHECK (`team` IN (0, 1)),
  CONSTRAINT `chk_duel_game_classes_count`
    CHECK (`count` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `duel_game_perks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `duel_game_id` int(11) NOT NULL,
  `team` tinyint(4) NOT NULL COMMENT '0 = survivors, 1 = maniacs',
  `perk_id` int(11) NOT NULL,
  `count` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_duel_game_perks_game_id` (`duel_game_id`),
  KEY `idx_duel_game_perks_perk_id` (`perk_id`),
  CONSTRAINT `duel_game_perks_game_fk`
    FOREIGN KEY (`duel_game_id`) REFERENCES `duel_games` (`id`) ON DELETE CASCADE,
  CONSTRAINT `duel_game_perks_perk_fk`
    FOREIGN KEY (`perk_id`) REFERENCES `ref_perks` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_duel_game_perks_team`
    CHECK (`team` IN (0, 1)),
  CONSTRAINT `chk_duel_game_perks_count`
    CHECK (`count` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Move already existing 1v1 rows out of the main stats table.
-- Legacy rows do not have duration data, so `duration_seconds` remains NULL.
INSERT IGNORE INTO `duel_games` (
  `id`, `session_id`, `played_at`, `started_at`, `ended_at`,
  `duration_seconds`, `winner`, `survivors_count`, `maniacs_count`
)
SELECT
  `id`, `session_id`, `played_at`, `started_at`, `ended_at`,
  `duration_seconds`, `winner`, `survivors_count`, `maniacs_count`
FROM `games`
WHERE `survivors_count` = 1 AND `maniacs_count` = 1;

INSERT INTO `duel_game_classes` (`duel_game_id`, `team`, `class_id`, `count`)
SELECT `gc`.`game_id`, `gc`.`team`, `gc`.`class_id`, `gc`.`count`
FROM `game_classes` `gc`
JOIN `games` `g` ON `g`.`id` = `gc`.`game_id`
WHERE `g`.`survivors_count` = 1 AND `g`.`maniacs_count` = 1;

INSERT INTO `duel_game_perks` (`duel_game_id`, `team`, `perk_id`, `count`)
SELECT `gp`.`game_id`, `gp`.`team`, `gp`.`perk_id`, `gp`.`count`
FROM `game_perks` `gp`
JOIN `games` `g` ON `g`.`id` = `gp`.`game_id`
WHERE `g`.`survivors_count` = 1 AND `g`.`maniacs_count` = 1;

DELETE FROM `games`
WHERE `survivors_count` = 1 AND `maniacs_count` = 1;

ALTER TABLE `games`
  ADD CONSTRAINT `chk_games_not_duel`
    CHECK (`survivors_count` + `maniacs_count` > 2),
  ADD CONSTRAINT `chk_games_min_duration`
    CHECK (`duration_seconds` IS NULL OR `duration_seconds` >= 60),
  ADD CONSTRAINT `chk_games_winner`
    CHECK (`winner` IN (0, 1)),
  ADD CONSTRAINT `chk_games_last_survivor_win`
    CHECK (`last_survivor_standing_won` IN (0, 1)
      AND `last_survivor_standing` IN (0, 1)
      AND (`last_survivor_standing_won` = 0 OR (`last_survivor_standing` = 1 AND `winner` = 0)));

COMMIT;
