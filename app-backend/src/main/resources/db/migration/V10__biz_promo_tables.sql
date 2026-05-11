-- 推广管理：合作、提现申请、预付款流水

CREATE TABLE biz_env_promo_coop (
  coop_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id         BIGINT NOT NULL,
  partner_name     VARCHAR(100) NOT NULL,
  contact_name     VARCHAR(50),
  contact_phone    VARCHAR(20),
  remark           VARCHAR(500),
  status           CHAR(1) DEFAULT '0',
  create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag         CHAR(1) DEFAULT '0'
);

CREATE TABLE biz_env_promo_withdraw (
  withdraw_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id         BIGINT NOT NULL,
  applicant_openid VARCHAR(128),
  amount           DECIMAL(14,2) NOT NULL,
  status           CHAR(1) DEFAULT '0',
  audit_remark     VARCHAR(255),
  create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag         CHAR(1) DEFAULT '0'
);

CREATE TABLE biz_env_promo_prepay (
  prepay_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id         BIGINT NOT NULL,
  merchant_id      BIGINT,
  title            VARCHAR(128) NOT NULL,
  amount           DECIMAL(14,2) NOT NULL,
  direction        CHAR(1) NOT NULL,
  ref_note         VARCHAR(255),
  create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag         CHAR(1) DEFAULT '0'
);

INSERT INTO biz_env_promo_coop (agent_id, partner_name, contact_name, contact_phone, remark, status, del_flag)
SELECT agent_id, '示例渠道商', '周经理', '13900001111', '推广合作示例', '0', '0'
FROM biz_env_agent WHERE del_flag = '0' ORDER BY agent_id LIMIT 1;
