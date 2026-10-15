-- Review/apply to the target statistics database; never run automatically by the mod.
-- Preserve every existing id/name. Java resolves this row by name, not scoreboard id 14.
INSERT INTO ref_classes (id, name)
SELECT COALESCE(MAX(id), 0) + 1, 'techies'
FROM ref_classes
HAVING SUM(CASE WHEN name = 'techies' THEN 1 ELSE 0 END) = 0 OR COUNT(*) = 0;
