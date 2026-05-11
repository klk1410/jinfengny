-- 环保油核心业务表（与《环保油数据库设计.sql》对齐，去掉 ENGINE 以便 H2 兼容）

CREATE TABLE biz_env_agent (
  agent_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_name          VARCHAR(100) NOT NULL,
  contact_name        VARCHAR(50),
  contact_phone       VARCHAR(20),
  province            VARCHAR(32),
  city                VARCHAR(32),
  district            VARCHAR(32),
  address_detail      VARCHAR(255),
  status              CHAR(1) DEFAULT '0',
  create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag            CHAR(1) DEFAULT '0'
);

CREATE TABLE biz_env_salesman (
  salesman_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  salesman_name VARCHAR(50) NOT NULL,
  phone           VARCHAR(20),
  agent_id        BIGINT NOT NULL,
  status          CHAR(1) DEFAULT '0',
  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag        CHAR(1) DEFAULT '0'
);

CREATE TABLE biz_env_merchant (
  merchant_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id              BIGINT NOT NULL,
  salesman_id           BIGINT,
  industry_type         VARCHAR(50),
  merchant_name         VARCHAR(100) NOT NULL,
  contact_name          VARCHAR(50),
  contact_phone         VARCHAR(20),
  longitude             DECIMAL(10,6),
  latitude              DECIMAL(10,6),
  province              VARCHAR(32),
  city                  VARCHAR(32),
  district              VARCHAR(32),
  address_detail        VARCHAR(255),
  oil_unit_price        DECIMAL(10,2) DEFAULT 0,
  merchant_commission   DECIMAL(10,2) DEFAULT 0,
  arrears_amount        DECIMAL(12,2) DEFAULT 0,
  device_count          INT DEFAULT 0,
  status                CHAR(1) DEFAULT '0',
  create_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag              CHAR(1) DEFAULT '0'
);

CREATE TABLE biz_env_order (
  order_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no              VARCHAR(32) NOT NULL,
  order_time            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  merchant_id           BIGINT NOT NULL,
  order_type            CHAR(1) NOT NULL,
  oil_unit_price        DECIMAL(10,2) DEFAULT 0,
  oil_bucket_count      DECIMAL(10,2) DEFAULT 0,
  amount_total          DECIMAL(12,2) DEFAULT 0,
  discount_amount       DECIMAL(12,2) DEFAULT 0,
  amount_payable        DECIMAL(12,2) DEFAULT 0,
  status                CHAR(1) DEFAULT '0',
  agent_id              BIGINT NOT NULL,
  pay_type              CHAR(1) DEFAULT '1',
  receive_salesman_id   BIGINT,
  work_order_no         VARCHAR(32),
  cancel_reason         VARCHAR(255),
  cancel_time           TIMESTAMP,
  finish_time           TIMESTAMP,
  create_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag              CHAR(1) DEFAULT '0',
  UNIQUE (order_no)
);

CREATE TABLE biz_env_work_order (
  work_order_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  work_order_no     VARCHAR(32) NOT NULL,
  order_id          BIGINT,
  order_no          VARCHAR(32),
  work_order_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  merchant_id       BIGINT NOT NULL,
  work_order_type   CHAR(1) NOT NULL,
  work_start_time   TIMESTAMP,
  work_end_time     TIMESTAMP,
  status            CHAR(1) DEFAULT '0',
  agent_id          BIGINT NOT NULL,
  receive_salesman_id BIGINT,
  create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag          CHAR(1) DEFAULT '0',
  UNIQUE (work_order_no)
);

CREATE TABLE biz_env_device (
  device_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_type     CHAR(1) NOT NULL,
  merchant_id     BIGINT,
  agent_id        BIGINT NOT NULL,
  device_no       VARCHAR(64) NOT NULL,
  device_status   CHAR(1) DEFAULT '0',
  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag        CHAR(1) DEFAULT '0',
  UNIQUE (device_no)
);

-- openid → 数据范围（首版：与文档角色/数据隔离配合；无记录则仅按 env_mini_subject 角色推断）
CREATE TABLE env_openid_biz_scope (
  openid        VARCHAR(128) NOT NULL PRIMARY KEY,
  user_role     CHAR(1) NOT NULL,
  agent_id      BIGINT,
  merchant_id   BIGINT,
  salesman_id   BIGINT
);
