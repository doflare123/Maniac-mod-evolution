-- Roll back Maniac Revolution stats DB to the legacy schema used by the current public mod build.
--
-- Restores the old tables:
--   games(id, played_at, winner, survivors_count, maniacs_count)
--   game_classes(id, game_id, team, class_id, count)
--   game_perks(id, game_id, team, perk_id, count)
--   ref_classes, ref_perks
--
-- If 1v1 rows were moved to duel_games by stats_schema_migration_2026_06_sessions.sql,
-- this script moves them back into games/game_classes/game_perks before dropping the new tables.

START TRANSACTION;

DROP PROCEDURE IF EXISTS `mr_drop_fk_if_exists`;
DROP PROCEDURE IF EXISTS `mr_drop_constraint_if_exists`;
DROP PROCEDURE IF EXISTS `mr_drop_index_if_exists`;
DROP PROCEDURE IF EXISTS `mr_drop_column_if_exists`;
DROP PROCEDURE IF EXISTS `mr_modify_column_if_exists`;
DROP PROCEDURE IF EXISTS `mr_restore_duels_if_exists`;
DROP PROCEDURE IF EXISTS `mr_reset_auto_increment`;

DELIMITER $$

CREATE PROCEDURE `mr_drop_fk_if_exists`(IN p_table varchar(64), IN p_fk varchar(64))
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND CONSTRAINT_NAME = p_fk
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP FOREIGN KEY `', p_fk, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `mr_drop_constraint_if_exists`(IN p_table varchar(64), IN p_constraint varchar(64))
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND CONSTRAINT_NAME = p_constraint
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP CONSTRAINT `', p_constraint, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `mr_drop_index_if_exists`(IN p_table varchar(64), IN p_index varchar(64))
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index
  ) THEN
    SET @sql = CONCAT('DROP INDEX `', p_index, '` ON `', p_table, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `mr_drop_column_if_exists`(IN p_table varchar(64), IN p_column varchar(64))
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `mr_modify_column_if_exists`(IN p_table varchar(64), IN p_column varchar(64), IN p_definition text)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` MODIFY `', p_column, '` ', p_definition);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `mr_restore_duels_if_exists`()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'duel_games'
  ) THEN
    INSERT IGNORE INTO `games` (`id`, `played_at`, `winner`, `survivors_count`, `maniacs_count`)
    SELECT `id`, `played_at`, `winner`, `survivors_count`, `maniacs_count`
    FROM `duel_games`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'duel_game_classes'
  ) THEN
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
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'duel_game_perks'
  ) THEN
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
  END IF;
END$$

CREATE PROCEDURE `mr_reset_auto_increment`(IN p_table varchar(64))
BEGIN
  SET @sql = CONCAT(
    'SELECT @mr_next_ai := COALESCE(MAX(`id`), 0) + 1 FROM `', p_table, '`'
  );
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  SET @sql = CONCAT('ALTER TABLE `', p_table, '` AUTO_INCREMENT = ', @mr_next_ai);
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END$$

DELIMITER ;

-- Remove new constraints on games so 1v1 rows can be restored.
CALL `mr_drop_fk_if_exists`('games', 'games_session_key_fk');
CALL `mr_drop_fk_if_exists`('games', 'games_session_fk');
CALL `mr_drop_constraint_if_exists`('games', 'chk_games_not_duel');
CALL `mr_drop_constraint_if_exists`('games', 'chk_games_min_duration');
CALL `mr_drop_constraint_if_exists`('games', 'chk_games_winner');
CALL `mr_drop_constraint_if_exists`('games', 'chk_games_last_survivor_win');

-- Relax new NOT NULL columns before restoring old rows from duel_games.
CALL `mr_modify_column_if_exists`('games', 'session_id', 'int(11) NULL DEFAULT NULL');
CALL `mr_modify_column_if_exists`('games', 'session_key', 'char(6) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL');

-- Move old 1v1 stats back to the legacy main tables.
CALL `mr_restore_duels_if_exists`();

-- Drop new side tables first.
DROP TABLE IF EXISTS `duel_game_hack_settings`;
DROP TABLE IF EXISTS `game_hack_settings`;
DROP TABLE IF EXISTS `duel_game_classes`;
DROP TABLE IF EXISTS `duel_game_perks`;
DROP TABLE IF EXISTS `duel_games`;
DROP TABLE IF EXISTS `game_session_players`;
DROP TABLE IF EXISTS `game_sessions`;

-- Drop indexes added to games by the new migrations.
CALL `mr_drop_index_if_exists`('games', 'idx_games_session_key');
CALL `mr_drop_index_if_exists`('games', 'idx_games_session_id');
CALL `mr_drop_index_if_exists`('games', 'idx_games_played_at');
CALL `mr_drop_index_if_exists`('games', 'idx_games_player_count');
CALL `mr_drop_index_if_exists`('games', 'idx_games_duration_seconds');
CALL `mr_drop_index_if_exists`('games', 'idx_games_map_numeric_id');
CALL `mr_drop_index_if_exists`('games', 'idx_games_computers');

-- Drop columns added to games by the new migrations.
CALL `mr_drop_column_if_exists`('games', 'session_key');
CALL `mr_drop_column_if_exists`('games', 'session_id');
CALL `mr_drop_column_if_exists`('games', 'started_at');
CALL `mr_drop_column_if_exists`('games', 'ended_at');
CALL `mr_drop_column_if_exists`('games', 'duration_seconds');
CALL `mr_drop_column_if_exists`('games', 'last_survivor_standing');
CALL `mr_drop_column_if_exists`('games', 'last_survivor_standing_won');
CALL `mr_drop_column_if_exists`('games', 'map_numeric_id');
CALL `mr_drop_column_if_exists`('games', 'map_id');
CALL `mr_drop_column_if_exists`('games', 'map_name');
CALL `mr_drop_column_if_exists`('games', 'computers_target');
CALL `mr_drop_column_if_exists`('games', 'computers_charged');

-- Reassert the old games column definitions.
ALTER TABLE `games`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,
  MODIFY `played_at` datetime NOT NULL DEFAULT current_timestamp(),
  MODIFY `winner` tinyint(4) NOT NULL COMMENT '0 = survivors, 1 = maniacs',
  MODIFY `survivors_count` int(11) NOT NULL,
  MODIFY `maniacs_count` int(11) NOT NULL;

CALL `mr_reset_auto_increment`('games');
CALL `mr_reset_auto_increment`('game_classes');
CALL `mr_reset_auto_increment`('game_perks');

DROP PROCEDURE IF EXISTS `mr_drop_fk_if_exists`;
DROP PROCEDURE IF EXISTS `mr_drop_constraint_if_exists`;
DROP PROCEDURE IF EXISTS `mr_drop_index_if_exists`;
DROP PROCEDURE IF EXISTS `mr_drop_column_if_exists`;
DROP PROCEDURE IF EXISTS `mr_modify_column_if_exists`;
DROP PROCEDURE IF EXISTS `mr_restore_duels_if_exists`;
DROP PROCEDURE IF EXISTS `mr_reset_auto_increment`;

COMMIT;
