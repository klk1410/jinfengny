-- 业务员修改店铺信息：审核单（代理/主端审批）

CREATE TABLE IF NOT EXISTS biz_env_merchant_audit (
  audit_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_id            BIGINT        NOT NULL,
  agent_id               BIGINT        NOT NULL COMMENT '冗余：代理数据范围过滤',
  submitter_salesman_id  BIGINT        NULL,
  submitter_openid       VARCHAR(128)  NOT NULL,
  status                 CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0待审 1通过 2驳回',
  payload_json           LONGTEXT      NOT NULL,
  submit_remark          VARCHAR(500)  NULL,
  review_openid          VARCHAR(128)  NULL,
  review_remark          VARCHAR(500)  NULL,
  create_time            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  review_time            TIMESTAMP     NULL DEFAULT NULL,
  del_flag               CHAR(1)       NOT NULL DEFAULT '0',
  KEY idx_merchant_audit_agent_status (agent_id, status, del_flag),
  KEY idx_merchant_audit_merchant (merchant_id, status, del_flag)
);
