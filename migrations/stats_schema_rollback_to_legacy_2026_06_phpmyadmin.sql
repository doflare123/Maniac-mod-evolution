-- phpMyAdmin-friendly rollback to legacy stats schema.
--
-- Use this when FK/constraint DROP statements from the direct rollback stop phpMyAdmin.
-- Before running: make a DB dump.

SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- If the new duel tables were not created or were already dropped, create empty
-- fallback tables so the restore SELECT statements below do not fail.
CREATE TABLE IF NOT EXISTS `duel_games` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `played_at` datetime NOT NULL DEFAULT current_timestamp(),
  `winner` tinyint(4) NOT NULL,
  `survivors_count` int(11) NOT NULL,
  `maniacs_count` int(11) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `duel_game_classes` (
  `duel_game_id` int(11) NOT NULL,
  `team` tinyint(4) NOT NULL,
  `class_id` varchar(64) NOT NULL,
  `count` int(11) NOT NULL
);

CREATE TABLE IF NOT EXISTS `duel_game_perks` (
  `duel_game_id` int(11) NOT NULL,
  `team` tinyint(4) NOT NULL,
  `perk_id` varchar(64) NOT NULL,
  `count` int(11) NOT NULL
);

-- Restore 1v1 games moved to duel_games.
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

-- Drop all new side/session/duel tables. FK checks are disabled, so no FK drops needed here.
DROP TABLE IF EXISTS `duel_game_hack_settings`;
DROP TABLE IF EXISTS `game_hack_settings`;
DROP TABLE IF EXISTS `duel_game_classes`;
DROP TABLE IF EXISTS `duel_game_perks`;
DROP TABLE IF EXISTS `duel_games`;
DROP TABLE IF EXISTS `game_session_players`;
DROP TABLE IF EXISTS `game_sessions`;

-- Drop constraints from games if your SQL console accepts them.
-- If any of these four lines fail because the constraint is missing, skip that line.
ALTER TABLE `games` DROP CONSTRAINT `chk_games_not_duel`;
ALTER TABLE `games` DROP CONSTRAINT `chk_games_min_duration`;
ALTER TABLE `games` DROP CONSTRAINT `chk_games_winner`;
ALTER TABLE `games` DROP CONSTRAINT `chk_games_last_survivor_win`;

-- Drop new indexes from games.
-- If any line fails because the index is missing, skip it.
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
SET FOREIGN_KEY_CHECKS = 1;
