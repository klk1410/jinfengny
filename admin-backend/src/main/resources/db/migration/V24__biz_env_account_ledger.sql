-- 账目流水（与 AppBizDataService / AppBizAccountService 一致；原 Java 迁移 V7 若未执行则补表）

CREATE TABLE IF NOT EXISTS biz_env_account_ledger (
  ledger_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id      BIGINT,
  merchant_id   BIGINT,
  ref_type      VARCHAR(32)  NOT NULL,
  ref_no        VARCHAR(64),
  title         VARCHAR(128) NOT NULL,
  amount        DECIMAL(14,2) NOT NULL,
  direction     CHAR(1)      NOT NULL COMMENT '1收入 2支出',
  create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_account_ledger_agent (agent_id),
  KEY idx_account_ledger_merchant (merchant_id),
  KEY idx_account_ledger_time (create_time)
);
