USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE strength_snapshot
    ADD COLUMN strategy_version INT NOT NULL DEFAULT 1 AFTER data_quality;

ALTER TABLE strength_snapshot
    ADD INDEX idx_strength_snapshot_strategy_version (strategy_version);

UPDATE strength_snapshot
SET strategy_version = 1
WHERE strategy_version IS NULL OR strategy_version <= 0;

ALTER TABLE opportunity_signal
    ADD COLUMN strategy_version INT NOT NULL DEFAULT 1 AFTER status;

ALTER TABLE opportunity_signal
    ADD INDEX idx_opportunity_signal_strategy_version (strategy_version);

UPDATE opportunity_signal
SET strategy_version = 1
WHERE strategy_version IS NULL OR strategy_version <= 0;
