ALTER TABLE watchlist
    DROP COLUMN change_rate_threshold,
    ADD COLUMN change_rate_threshold_above NUMERIC(5,2),
    ADD COLUMN change_rate_threshold_below NUMERIC(5,2);
