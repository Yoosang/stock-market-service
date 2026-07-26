CREATE TABLE stocks (
    symbol VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    market VARCHAR(20) NOT NULL
);

INSERT INTO stocks (symbol, name, market) VALUES
('005930', '삼성전자', 'KOSPI'),
('000660', 'SK하이닉스', 'KOSPI'),
('035420', 'NAVER', 'KOSPI'),
('035720', '카카오', 'KOSPI'),
('207940', '삼성바이오로직스', 'KOSPI');