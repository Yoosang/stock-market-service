CREATE TABLE watchlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, stock_symbol)
);