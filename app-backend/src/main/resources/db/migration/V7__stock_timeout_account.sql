-- 工单抢单截止、代理仓储、库存流水、账目流水（H2 / MySQL 通用）

ALTER TABLE biz_env_work_order ADD COLUMN accept_deadline TIMESTAMP NULL;
ALTER TABLE biz_env_work_order ADD COLUMN assign_type CHAR(1) DEFAULT '1';

CREATE TABLE IF NOT EXISTS biz_env_agent_stock (
  agent_id      BIGINT NOT NULL,
  sku_code      CHAR(1) NOT NULL DEFAULT '1',
  qty_on_hand   DECIMAL(14,2) NOT NULL DEFAULT 0,
  qty_reserved  DECIMAL(14,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (agent_id, sku_code)
);

CREATE TABLE IF NOT EXISTS biz_env_stock_flow (
  flow_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id      BIGINT NOT NULL,
  sku_code      CHAR(1) NOT NULL DEFAULT '1',
  ref_type      VARCHAR(32) NOT NULL,
  ref_no        VARCHAR(64),
  flow_kind     CHAR(1) NOT NULL,
  qty           DECIMAL(14,2) NOT NULL,
  remark        VARCHAR(255),
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS biz_env_account_ledger (
  ledger_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id      BIGINT,
  merchant_id   BIGINT,
  ref_type      VARCHAR(32) NOT NULL,
  ref_no        VARCHAR(64),
  title         VARCHAR(128) NOT NULL,
  amount        DECIMAL(14,2) NOT NULL,
  direction     CHAR(1) NOT NULL,
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 种子：代理 1 初始库存（桶）
INSERT INTO biz_env_agent_stock (agent_id, sku_code, qty_on_hand, qty_reserved)
SELECT 1, '1', 10000.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM biz_env_agent_stock WHERE agent_id = 1 AND sku_code = '1');

-- 历史工单：补抢单截止（便于演示超时逻辑）
UPDATE biz_env_work_order SET accept_deadline = work_order_time WHERE accept_deadline IS NULL AND del_flag = '0';
