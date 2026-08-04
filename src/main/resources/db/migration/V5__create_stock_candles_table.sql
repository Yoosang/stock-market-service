CREATE TABLE stock_candles (
    id BIGSERIAL PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol),
    trade_date DATE NOT NULL,
    bucket_time TIME NOT NULL,
    open_price INTEGER NOT NULL,
    high_price INTEGER NOT NULL,
    low_price INTEGER NOT NULL,
    close_price INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (stock_symbol, trade_date, bucket_time)
);
