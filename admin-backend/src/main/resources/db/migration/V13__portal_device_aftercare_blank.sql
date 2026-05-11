-- 推广：推广设备 → 新增设备（路由 #/promo/device-new）
-- 售后：设备移除日志 → 设备日志；电池管理 → 设备管理（监测状态）
-- 删除：弹出记录、会员充值记录（门户项 + 角色权限）
-- 未实现的旧小程序路径统一改为占位页 #/blank
-- 设备增加/移除流水表

CREATE TABLE biz_env_device_event_log (
  log_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id           BIGINT NOT NULL,
  merchant_id        BIGINT,
  device_no          VARCHAR(64) NOT NULL,
  event_type         CHAR(1) NOT NULL,
  remark             VARCHAR(500),
  operator_openid    VARCHAR(128),
  create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag           CHAR(1) DEFAULT '0'
);

DELETE FROM env_mini_role_perm WHERE perm_code IN ('env:p:poplog', 'env:p:recharge');
DELETE FROM env_portal_function WHERE perm_code IN ('env:p:poplog', 'env:p:recharge');

UPDATE env_portal_function SET label = '新增设备', route_path = '#/promo/device-new', icon = '➕'
WHERE perm_code = 'env:page:promo:devices';

UPDATE env_portal_function SET route_path = '#/blank'
WHERE route_path LIKE '/pages/%' OR route_path LIKE 'pages/%';

UPDATE env_portal_function SET label = '设备日志', route_path = '#/after/device-log'
WHERE perm_code = 'env:p:rmvlog';

UPDATE env_portal_function SET label = '设备管理', route_path = '#/after/device-mgmt'
WHERE perm_code = 'env:p:battery';
