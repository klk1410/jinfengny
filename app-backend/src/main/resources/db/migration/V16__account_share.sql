-- 账户信息：共享账号
CREATE TABLE biz_env_account_share (
  share_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_openid      VARCHAR(128) NOT NULL,
  shared_openid     VARCHAR(128) NOT NULL,
  user_role         CHAR(1) NOT NULL,
  agent_id          BIGINT,
  merchant_id       BIGINT,
  salesman_id       BIGINT,
  status            CHAR(1) DEFAULT '0',
  create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag          CHAR(1) DEFAULT '0',
  UNIQUE (owner_openid, shared_openid)
);

