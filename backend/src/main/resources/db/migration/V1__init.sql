-- 首期平台模型：用户、会话、工具注册、版本、策略、用量计数与审计。
-- 兼容目标：MySQL 8.4 与 H2（MySQL 模式，测试环境），因此不使用 MySQL 方言类型（如 JSON）。
-- 回滚：按依赖逆序 DROP TABLE audit_logs, tool_usage_counters, tool_policies, tool_versions, tools, auth_sessions, users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    identifier VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(128),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_type VARCHAR(16) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE tools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_id VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    current_version VARCHAR(32) NOT NULL,
    input_schema TEXT,
    output_schema TEXT,
    aggregate_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tool_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_id VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    input_schema TEXT,
    output_schema TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tool_versions UNIQUE (tool_id, version)
);

CREATE TABLE tool_policies (
    tool_id VARCHAR(64) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    login_required BOOLEAN NOT NULL DEFAULT FALSE,
    guest_limit INT NULL,
    guest_limit_window_seconds INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tool_policies_tool FOREIGN KEY (tool_id) REFERENCES tools (tool_id)
);

CREATE TABLE tool_usage_counters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_id VARCHAR(64) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tool_usage_counters UNIQUE (tool_id, subject_id, window_start)
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target VARCHAR(128) NOT NULL,
    details TEXT,
    request_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
