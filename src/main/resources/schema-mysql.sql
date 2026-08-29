-- Prod-profile schema for MySQL 8.x.
-- Loaded by Spring Boot on prod startup when spring.sql.init.mode=always
-- (or manually via `mysql < schema.sql`). All tables are idempotent.

CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64),
    level VARCHAR(16)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT,
    total_amount DECIMAL(10,2),
    status VARCHAR(16),
    tracking_number VARCHAR(64),
    created_at DATETIME,
    INDEX idx_user (user_id)
);

CREATE TABLE IF NOT EXISTS refund (
    refund_id VARCHAR(32) PRIMARY KEY,
    order_id VARCHAR(32),
    user_id BIGINT,
    reason TEXT,
    status VARCHAR(16),
    created_at DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_order (order_id)
);

CREATE TABLE IF NOT EXISTS coupon (
    coupon_id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(128),
    discount DECIMAL(10,2),
    expires_at DATETIME,
    used TINYINT(1)
);