-- Adds map and computer objective/progress stats.
-- Run after stats_schema_migration_2026_06_sessions.sql.

START TRANSACTION;

ALTER TABLE `games`
  ADD COLUMN IF NOT EXISTS `map_numeric_id` int(11) NULL DEFAULT NULL AFTER `last_survivor_standing_won`,
  ADD COLUMN IF NOT EXISTS `map_id` varchar(64) NULL DEFAULT NULL AFTER `map_numeric_id`,
  ADD COLUMN IF NOT EXISTS `map_name` varchar(128) NULL DEFAULT NULL AFTER `map_id`,
  ADD COLUMN IF NOT EXISTS `computers_target` int(11) NULL DEFAULT NULL AFTER `map_name`,
  ADD COLUMN IF NOT EXISTS `computers_charged` int(11) NULL DEFAULT NULL AFTER `computers_target`;

ALTER TABLE `duel_games`
  ADD COLUMN IF NOT EXISTS `map_numeric_id` int(11) NULL DEFAULT NULL AFTER `maniacs_count`,
  ADD COLUMN IF NOT EXISTS `map_id` varchar(64) NULL DEFAULT NULL AFTER `map_numeric_id`,
  ADD COLUMN IF NOT EXISTS `map_name` varchar(128) NULL DEFAULT NULL AFTER `map_id`,
  ADD COLUMN IF NOT EXISTS `computers_target` int(11) NULL DEFAULT NULL AFTER `map_name`,
  ADD COLUMN IF NOT EXISTS `computers_charged` int(11) NULL DEFAULT NULL AFTER `computers_target`;

CREATE INDEX IF NOT EXISTS `idx_games_map_numeric_id` ON `games` (`map_numeric_id`);
CREATE INDEX IF NOT EXISTS `idx_games_computers` ON `games` (`computers_target`, `computers_charged`);
CREATE INDEX IF NOT EXISTS `idx_duel_games_map_numeric_id` ON `duel_games` (`map_numeric_id`);
CREATE INDEX IF NOT EXISTS `idx_duel_games_computers` ON `duel_games` (`computers_target`, `computers_charged`);

CREATE TABLE IF NOT EXISTS `game_hack_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `game_id` int(11) NOT NULL,
  `hack_points_required` decimal(8,3) NOT NULL,
  `points_per_player_per_second` decimal(8,3) NOT NULL,
  `points_per_specialist_per_second` decimal(8,3) NOT NULL,
  `max_bonus_players` int(11) NOT NULL,
  `hacker_radius` decimal(8,3) NOT NULL,
  `support_radius` decimal(8,3) NOT NULL,
  `qte_interval_min_seconds` int(11) NOT NULL,
  `qte_interval_max_seconds` int(11) NOT NULL,
  `qte_success_bonus` decimal(8,3) NOT NULL,
  `qte_crit_bonus` decimal(8,3) NOT NULL,
  `computers_needed_for_win` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_game_hack_settings_game_id` (`game_id`),
  CONSTRAINT `game_hack_settings_game_fk`
    FOREIGN KEY (`game_id`) REFERENCES `games` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `duel_game_hack_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `duel_game_id` int(11) NOT NULL,
  `hack_points_required` decimal(8,3) NOT NULL,
  `points_per_player_per_second` decimal(8,3) NOT NULL,
  `points_per_specialist_per_second` decimal(8,3) NOT NULL,
  `max_bonus_players` int(11) NOT NULL,
  `hacker_radius` decimal(8,3) NOT NULL,
  `support_radius` decimal(8,3) NOT NULL,
  `qte_interval_min_seconds` int(11) NOT NULL,
  `qte_interval_max_seconds` int(11) NOT NULL,
  `qte_success_bonus` decimal(8,3) NOT NULL,
  `qte_crit_bonus` decimal(8,3) NOT NULL,
  `computers_needed_for_win` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_duel_game_hack_settings_game_id` (`duel_game_id`),
  CONSTRAINT `duel_game_hack_settings_game_fk`
    FOREIGN KEY (`duel_game_id`) REFERENCES `duel_games` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

COMMIT;
