-- Test data for shopagent. Loaded after schema-mysql.sql on startup.

INSERT IGNORE INTO user (id, name, level) VALUES
(1, 'Alice', 'NORMAL'),
(2, 'Bob', 'VIP'),
(999, 'Mallory', 'BLACKLIST');

INSERT IGNORE INTO product (product_id, name, category, price, description) VALUES
('P1', '蓝牙耳机', '数码', 299.00, '主动降噪'),
('P2', '保温杯', '家居', 89.00, '316不锈钢'),
('P3', '机械键盘', '数码', 599.00, '青轴'),
('P4', '帆布鞋', '服饰', 199.00, '经典款');

INSERT IGNORE INTO orders (order_id, user_id, items, total_amount, status, tracking_number, created_at) VALUES
('O1001', 1, '[{"productId":"P1","productName":"蓝牙耳机","quantity":1,"unitPrice":299.00}]', 299.00, 'PENDING', NULL, NOW()),
('O1002', 1, '[{"productId":"P2","productName":"保温杯","quantity":1,"unitPrice":89.00}]', 89.00, 'SHIPPED', 'SF1234567890', NOW()),
('O1003', 2, '[{"productId":"P3","productName":"机械键盘","quantity":1,"unitPrice":599.00}]', 599.00, 'DELIVERED', 'SF9876543210', NOW()),
('O1004', 2, '[{"productId":"P4","productName":"帆布鞋","quantity":1,"unitPrice":199.00}]', 199.00, 'REFUNDED', 'YT1122334455', NOW());

INSERT IGNORE INTO coupon (coupon_id, user_id, name, discount, expires_at, used) VALUES
('C1', 1, '新人立减', 20.00, DATE_ADD(NOW(), INTERVAL 30 DAY), 0),
('C2', 2, 'VIP专享', 50.00, DATE_ADD(NOW(), INTERVAL 60 DAY), 0);
