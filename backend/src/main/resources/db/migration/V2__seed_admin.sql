-- 种子管理员，仅用于本地开发和首次部署引导。
-- 密码为 admin-dev-2026（BCrypt），README 已声明：任何非本地环境必须在首次登录后修改。
-- 回滚：DELETE FROM users WHERE identifier = 'admin';

INSERT INTO users (identifier, password_hash, display_name, role, status)
VALUES ('admin', '$2b$10$BY5O1DQg4HULxmcR3bqFoeVbGGniUDVC6ffJ3kB8HJVTagvBBW7Di', 'Administrator', 'ADMIN', 'ACTIVE');
