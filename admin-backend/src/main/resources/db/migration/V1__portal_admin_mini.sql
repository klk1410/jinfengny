-- 门户九宫格 / 管理员 / 小程序角色权限（MySQL / H2 MySQL MODE 通用，无 ENGINE）

CREATE TABLE env_admin_user (
  user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nick_name     VARCHAR(64)  DEFAULT NULL,
  status        CHAR(1)      DEFAULT '0',
  create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_env_admin_username (username)
);

CREATE TABLE env_portal_func_group (
  group_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(100) NOT NULL,
  sort_order   INT          DEFAULT 0,
  status       CHAR(1)      DEFAULT '0',
  del_flag     CHAR(1)      DEFAULT '0'
);

CREATE TABLE env_portal_function (
  func_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id    BIGINT       NOT NULL,
  perm_code   VARCHAR(128) NOT NULL,
  label       VARCHAR(100) NOT NULL,
  icon        VARCHAR(512) DEFAULT NULL,
  route_path  VARCHAR(512) DEFAULT NULL,
  sort_order  INT          DEFAULT 0,
  status      CHAR(1)      DEFAULT '0',
  del_flag    CHAR(1)      DEFAULT '0',
  UNIQUE KEY uk_env_portal_perm (perm_code),
  KEY idx_env_portal_group (group_id)
);

CREATE TABLE env_mini_role (
  role_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(64)  NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  sort_order INT         DEFAULT 0,
  status    CHAR(1)     DEFAULT '0',
  UNIQUE KEY uk_env_mini_role_code (role_code)
);

CREATE TABLE env_mini_role_perm (
  role_id    BIGINT       NOT NULL,
  perm_code  VARCHAR(128) NOT NULL,
  PRIMARY KEY (role_id, perm_code),
  KEY idx_env_mini_role_perm_code (perm_code)
);

CREATE TABLE env_mini_subject (
  openid  VARCHAR(128) NOT NULL PRIMARY KEY,
  role_id BIGINT       NOT NULL,
  UNIQUE KEY uk_env_mini_subject (openid),
  KEY idx_env_mini_subject_role (role_id)
);
