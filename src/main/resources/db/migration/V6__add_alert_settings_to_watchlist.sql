ALTER TABLE watchlist
    ADD COLUMN alert_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN target_price_above INTEGER,
    ADD COLUMN target_price_below INTEGER,
    ADD COLUMN change_rate_threshold NUMERIC(5,2);
