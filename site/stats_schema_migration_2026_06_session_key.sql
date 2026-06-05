-- Adds a public 6-character alphanumeric session key.
-- Run after stats_schema_migration_2026_06_sessions.sql.
--
-- `id` remains the internal numeric primary key.
-- `session_key` is the public tracking key and is case-sensitive.

START TRANSACTION;

ALTER TABLE `game_sessions`
  ADD COLUMN IF NOT EXISTS `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL AFTER `id`;

UPDATE `game_sessions`
SET `session_key` = LPAD(CONV(`id`, 10, 36), 6, '0')
WHERE `session_key` IS NULL OR `session_key` = '';

ALTER TABLE `game_sessions`
  MODIFY `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS `uk_game_sessions_session_key`
  ON `game_sessions` (`session_key`);

ALTER TABLE `game_session_players`
  ADD COLUMN IF NOT EXISTS `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL AFTER `session_id`;

UPDATE `game_session_players` `p`
JOIN `game_sessions` `s` ON `s`.`id` = `p`.`session_id`
SET `p`.`session_key` = `s`.`session_key`
WHERE `p`.`session_key` IS NULL OR `p`.`session_key` = '';

ALTER TABLE `game_session_players`
  MODIFY `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;

CREATE INDEX IF NOT EXISTS `idx_game_session_players_session_key`
  ON `game_session_players` (`session_key`);

CREATE UNIQUE INDEX IF NOT EXISTS `uk_session_key_player_uuid`
  ON `game_session_players` (`session_key`, `player_uuid`);

ALTER TABLE `game_session_players`
  ADD CONSTRAINT `game_session_players_session_key_fk`
    FOREIGN KEY (`session_key`) REFERENCES `game_sessions` (`session_key`) ON DELETE CASCADE;

ALTER TABLE `games`
  ADD COLUMN IF NOT EXISTS `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL AFTER `session_id`;

UPDATE `games` `g`
JOIN `game_sessions` `s` ON `s`.`id` = `g`.`session_id`
SET `g`.`session_key` = `s`.`session_key`
WHERE `g`.`session_key` IS NULL OR `g`.`session_key` = '';

ALTER TABLE `games`
  MODIFY `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;

CREATE INDEX IF NOT EXISTS `idx_games_session_key`
  ON `games` (`session_key`);

ALTER TABLE `games`
  ADD CONSTRAINT `games_session_key_fk`
    FOREIGN KEY (`session_key`) REFERENCES `game_sessions` (`session_key`) ON DELETE RESTRICT;

ALTER TABLE `duel_games`
  ADD COLUMN IF NOT EXISTS `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL AFTER `session_id`;

UPDATE `duel_games` `g`
JOIN `game_sessions` `s` ON `s`.`id` = `g`.`session_id`
SET `g`.`session_key` = `s`.`session_key`
WHERE `g`.`session_key` IS NULL OR `g`.`session_key` = '';

ALTER TABLE `duel_games`
  MODIFY `session_key` char(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;

CREATE INDEX IF NOT EXISTS `idx_duel_games_session_key`
  ON `duel_games` (`session_key`);

ALTER TABLE `duel_games`
  ADD CONSTRAINT `duel_games_session_key_fk`
    FOREIGN KEY (`session_key`) REFERENCES `game_sessions` (`session_key`) ON DELETE RESTRICT;

COMMIT;
