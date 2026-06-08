-- Direct rollback to the legacy stats schema.
-- Use this if phpMyAdmin/MySQL rejects the procedure-based rollback.
--
-- IMPORTANT:
--   This assumes all three new migrations were applied:
--     stats_schema_migration_2026_06_sessions.sql
--     stats_schema_migration_2026_06_session_key.sql
--     stats_schema_migration_2026_06_computer_map.sql
--
-- If a DROP FOREIGN KEY / DROP CHECK / DROP INDEX line fails because that object
-- does not exist, skip that specific line and continue with the next one.

START TRANSACTION;

-- Remove FKs/checks that depend on new columns/tables.
ALTER TABLE `duel_game_hack_settings` DROP FOREIGN KEY `duel_game_hack_settings_game_fk`;
ALTER TABLE `game_hack_settings` DROP FOREIGN KEY `game_hack_settings_game_fk`;
ALTER TABLE `duel_game_classes` DROP FOREIGN KEY `duel_game_classes_game_fk`;
ALTER TABLE `duel_game_classes` DROP FOREIGN KEY `duel_game_classes_class_fk`;
ALTER TABLE `duel_game_perks` DROP FOREIGN KEY `duel_game_perks_game_fk`;
ALTER TABLE `duel_game_perks` DROP FOREIGN KEY `duel_game_perks_perk_fk`;
ALTER TABLE `duel_games` DROP FOREIGN KEY `duel_games_session_key_fk`;
ALTER TABLE `duel_games` DROP FOREIGN KEY `duel_games_session_fk`;
ALTER TABLE `game_session_players` DROP FOREIGN KEY `game_session_players_session_key_fk`;
ALTER TABLE `game_session_players` DROP FOREIGN KEY `game_session_players_session_fk`;
ALTER TABLE `games` DROP FOREIGN KEY `games_session_key_fk`;
ALTER TABLE `games` DROP FOREIGN KEY `games_session_fk`;

ALTER TABLE `games` DROP CONSTRAINT `chk_games_not_duel`;
ALTER TABLE `games` DROP CONSTRAINT `chk_games_min_duration`;
ALTER TABLE `games` DROP CONSTRAINT `chk_games_winner`;
ALTER TABLE `games` DROP CONSTRAINT `chk_games_last_survivor_win`;

-- Allow legacy 1v1 rows to be restored.
ALTER TABLE `games`
  MODIFY `session_id` int(11) NULL DEFAULT NULL,
  MODIFY `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL;

-- Move 1v1 rows back to the old main stats table.
INSERT IGNORE INTO `games` (`id`, `played_at`, `winner`, `survivors_count`, `maniacs_count`)
SELECT `id`, `played_at`, `winner`, `survivors_count`, `maniacs_count`
FROM `duel_games`;

INSERT INTO `game_classes` (`game_id`, `team`, `class_id`, `count`)
SELECT `dc`.`duel_game_id`, `dc`.`team`, `dc`.`class_id`, `dc`.`count`
FROM `duel_game_classes` `dc`
WHERE NOT EXISTS (
  SELECT 1
  FROM `game_classes` `gc`
  WHERE `gc`.`game_id` = `dc`.`duel_game_id`
    AND `gc`.`team` = `dc`.`team`
    AND `gc`.`class_id` = `dc`.`class_id`
    AND `gc`.`count` = `dc`.`count`
);

INSERT INTO `game_perks` (`game_id`, `team`, `perk_id`, `count`)
SELECT `dp`.`duel_game_id`, `dp`.`team`, `dp`.`perk_id`, `dp`.`count`
FROM `duel_game_perks` `dp`
WHERE NOT EXISTS (
  SELECT 1
  FROM `game_perks` `gp`
  WHERE `gp`.`game_id` = `dp`.`duel_game_id`
    AND `gp`.`team` = `dp`.`team`
    AND `gp`.`perk_id` = `dp`.`perk_id`
    AND `gp`.`count` = `dp`.`count`
);

-- Drop new tables.
DROP TABLE `duel_game_hack_settings`;
DROP TABLE `game_hack_settings`;
DROP TABLE `duel_game_classes`;
DROP TABLE `duel_game_perks`;
DROP TABLE `duel_games`;
DROP TABLE `game_session_players`;
DROP TABLE `game_sessions`;

-- Drop new indexes from games. ALTER TABLE syntax avoids "DROP INDEX ... ON ..."
-- which some phpMyAdmin parsers complain about.
ALTER TABLE `games` DROP INDEX `idx_games_session_key`;
ALTER TABLE `games` DROP INDEX `idx_games_session_id`;
ALTER TABLE `games` DROP INDEX `idx_games_played_at`;
ALTER TABLE `games` DROP INDEX `idx_games_player_count`;
ALTER TABLE `games` DROP INDEX `idx_games_duration_seconds`;
ALTER TABLE `games` DROP INDEX `idx_games_map_numeric_id`;
ALTER TABLE `games` DROP INDEX `idx_games_computers`;

-- Drop new columns from games.
ALTER TABLE `games`
  DROP COLUMN `session_key`,
  DROP COLUMN `session_id`,
  DROP COLUMN `started_at`,
  DROP COLUMN `ended_at`,
  DROP COLUMN `duration_seconds`,
  DROP COLUMN `last_survivor_standing`,
  DROP COLUMN `last_survivor_standing_won`,
  DROP COLUMN `map_numeric_id`,
  DROP COLUMN `map_id`,
  DROP COLUMN `map_name`,
  DROP COLUMN `computers_target`,
  DROP COLUMN `computers_charged`;

-- Reassert legacy column definitions.
ALTER TABLE `games`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,
  MODIFY `played_at` datetime NOT NULL DEFAULT current_timestamp(),
  MODIFY `winner` tinyint(4) NOT NULL COMMENT '0 = survivors, 1 = maniacs',
  MODIFY `survivors_count` int(11) NOT NULL,
  MODIFY `maniacs_count` int(11) NOT NULL;

COMMIT;
