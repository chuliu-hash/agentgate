-- AgentGate 建库建表脚本（MySQL 8.0）
-- 管理员、套餐、演示 API 由应用启动时的 DataInitializer 幂等补齐。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS agentgate
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE agentgate;

CREATE TABLE IF NOT EXISTS tb_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    nickname    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    role        VARCHAR(16)  NOT NULL DEFAULT 'DEVELOPER' COMMENT 'ADMIN/DEVELOPER',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS tb_app (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           BIGINT       NOT NULL COMMENT '所属开发者',
    name              VARCHAR(64)  NOT NULL COMMENT '应用名称',
    app_key           VARCHAR(64)  NOT NULL COMMENT 'AppKey',
    app_secret_cipher VARCHAR(256) NOT NULL COMMENT 'AES-GCM 加密后的 AppSecret',
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    ip_whitelist      VARCHAR(512) DEFAULT NULL COMMENT '逗号分隔，* 表示不限制',
    create_time       DATETIME     NOT NULL,
    update_time       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_key (app_key),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开发者应用表';

CREATE TABLE IF NOT EXISTS tb_api (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code              VARCHAR(64)  NOT NULL COMMENT '对外稳定编码',
    name              VARCHAR(128) NOT NULL COMMENT 'API 名称',
    method            VARCHAR(8)   NOT NULL DEFAULT 'ANY' COMMENT 'GET/POST/PUT/DELETE/ANY',
    upstream_url      VARCHAR(512) NOT NULL COMMENT '上游地址',
    version           VARCHAR(32)  NOT NULL DEFAULT 'v1' COMMENT '版本',
    sensitivity_level TINYINT      NOT NULL DEFAULT 1 COMMENT '1 普通 2 敏感 3 高敏',
    status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    timeout_ms        INT          NOT NULL DEFAULT 5000 COMMENT '上游超时（预留）',
    description       VARCHAR(512) DEFAULT NULL,
    create_time       DATETIME     NOT NULL,
    update_time       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开放 API 定义表';

CREATE TABLE IF NOT EXISTS tb_quota_plan (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(64)  NOT NULL COMMENT '套餐名称',
    qps_limit     INT          NOT NULL COMMENT 'QPS 上限',
    daily_quota   INT          NOT NULL COMMENT '日调用上限',
    monthly_quota INT          NOT NULL COMMENT '月调用上限',
    description   VARCHAR(256) DEFAULT NULL,
    create_time   DATETIME     NOT NULL,
    update_time   DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '配额套餐表';

CREATE TABLE IF NOT EXISTS tb_subscription (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    app_id        BIGINT       NOT NULL COMMENT '应用 ID',
    api_id        BIGINT       NOT NULL COMMENT 'API ID',
    quota_plan_id BIGINT       NOT NULL COMMENT '配额套餐 ID',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/REJECTED',
    reject_reason VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    create_time   DATETIME     NOT NULL,
    update_time   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_api (app_id, api_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应用订阅表';

CREATE TABLE IF NOT EXISTS tb_call_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id      VARCHAR(64) NOT NULL COMMENT '链路 ID',
    app_id        BIGINT      DEFAULT NULL COMMENT '应用 ID',
    api_id        BIGINT      DEFAULT NULL COMMENT 'API ID',
    method        VARCHAR(8)  DEFAULT NULL,
    path          VARCHAR(512) DEFAULT NULL,
    status_code   INT         DEFAULT NULL,
    latency_ms    BIGINT      DEFAULT NULL,
    client_ip     VARCHAR(64) DEFAULT NULL,
    error_code    INT         DEFAULT NULL,
    request_size  INT         DEFAULT NULL,
    response_size INT         DEFAULT NULL,
    create_time   DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trace_id (trace_id),
    KEY idx_app_time (app_id, create_time),
    KEY idx_api_time (api_id, create_time),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开放接口调用日志';

CREATE TABLE IF NOT EXISTS tb_usage_stat (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    app_id      BIGINT   NOT NULL COMMENT '应用 ID',
    api_id      BIGINT   NOT NULL COMMENT 'API ID',
    stat_date   DATE     NOT NULL COMMENT '统计日期',
    call_count  BIGINT   NOT NULL DEFAULT 0 COMMENT '调用次数',
    error_count BIGINT   NOT NULL DEFAULT 0 COMMENT '错误次数',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_api_date (app_id, api_id, stat_date),
    KEY idx_date (stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '按日用量统计';

CREATE TABLE IF NOT EXISTS tb_audit_log (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT        DEFAULT NULL COMMENT '操作人 ID',
    username      VARCHAR(32)   DEFAULT NULL COMMENT '操作人',
    action        VARCHAR(64)   NOT NULL COMMENT '动作',
    resource_type VARCHAR(64)   DEFAULT NULL COMMENT '资源类型',
    resource_id   VARCHAR(64)   DEFAULT NULL COMMENT '资源 ID',
    detail        VARCHAR(1024) DEFAULT NULL COMMENT '关键参数（截断）',
    ip            VARCHAR(64)   DEFAULT NULL COMMENT '客户端 IP',
    create_time   DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理端操作审计';
