-- 环保油业务：配件管理

CREATE TABLE biz_env_accessory (
  acc_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id          BIGINT NOT NULL,
  merchant_id       BIGINT,
  acc_name          VARCHAR(120) NOT NULL,
  qty               DECIMAL(12,2) DEFAULT 0,
  unit_price        DECIMAL(12,2) DEFAULT 0,
  remark            VARCHAR(500),
  create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag          CHAR(1) DEFAULT '0'
);

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:accessories', '配件管理', '🧰', '#/accessories', 55, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES
 (1, 'env:page:accessories'),
 (2, 'env:page:accessories'),
 (3, 'env:page:accessories');

