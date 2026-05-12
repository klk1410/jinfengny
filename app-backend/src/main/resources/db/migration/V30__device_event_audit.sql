-- 业务员设备操作（新增/移除/报废/调至门店）须代理审核后生效

CREATE TABLE IF NOT EXISTS biz_env_device_event_audit (
  audit_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id               BIGINT        NOT NULL,
  submitter_salesman_id  BIGINT        NOT NULL,
  submitter_openid       VARCHAR(128)  NOT NULL,
  event_type             CHAR(1)       NOT NULL COMMENT 'A R S T 与设备事件一致',
  device_no              VARCHAR(64)   NOT NULL,
  status                 CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0待审 1通过 2驳回',
  payload_json           LONGTEXT      NOT NULL,
  submit_remark          VARCHAR(500)  NULL,
  review_openid          VARCHAR(128)  NULL,
  review_remark          VARCHAR(500)  NULL,
  create_time            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  review_time            TIMESTAMP     NULL DEFAULT NULL,
  del_flag               CHAR(1)       NOT NULL DEFAULT '0',
  KEY idx_dev_evt_audit_agent_status (agent_id, status, del_flag),
  KEY idx_dev_evt_audit_pending (agent_id, device_no, status, del_flag)
);

-- 系统管理：设备操作审核（主端/代理/业务员可见列表）
INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id,
       'env:page:promo:device-event-audits',
       '设备操作审核',
       '🛠️',
       '#/promo/device-event-audits',
       37,
       '0',
       '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '系统管理' AND del_flag = '0' ORDER BY group_id ASC LIMIT 1) g
WHERE NOT EXISTS (SELECT 1 FROM env_portal_function WHERE perm_code = 'env:page:promo:device-event-audits' AND del_flag = '0');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 1, 'env:page:promo:device-event-audits'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 1 AND perm_code = 'env:page:promo:device-event-audits');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 2, 'env:page:promo:device-event-audits'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 2 AND perm_code = 'env:page:promo:device-event-audits');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 3, 'env:page:promo:device-event-audits'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 3 AND perm_code = 'env:page:promo:device-event-audits');
