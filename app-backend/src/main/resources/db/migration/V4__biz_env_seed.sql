-- 示例业务数据（幂等：可重复执行，已存在则跳过，避免 uk_env_order_no 等唯一键冲突）

INSERT INTO biz_env_agent (agent_name, contact_name, contact_phone, province, city, status, del_flag)
SELECT '广州一代', '陈总', '13900000001', '广东省', '广州市', '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000001' AND del_flag = '0');

INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag)
SELECT '李师傅', '13800138001', (SELECT MIN(agent_id) FROM biz_env_agent), '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13800138001')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag)
SELECT '赵师傅', '13800138002', (SELECT MIN(agent_id) FROM biz_env_agent), '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13800138002')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO biz_env_merchant (agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  province, city, oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag)
SELECT
  (SELECT MIN(agent_id) FROM biz_env_agent),
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13800138001' LIMIT 1),
  '餐饮', '粤海饭店', '张经理', '13800138000', '广东省', '广州市', 300.00, 10.00, 0.00, 2, '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO biz_env_merchant (agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  province, city, oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag)
SELECT
  (SELECT MIN(agent_id) FROM biz_env_agent),
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13800138002' LIMIT 1),
  '餐饮', '潮味大排档', '王老板', '13800138003', '广东省', '广州市', 280.00, 8.00, 120.00, 1, '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '潮味大排档' AND del_flag = '0')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO biz_env_order (order_no, merchant_id, order_type, oil_unit_price, oil_bucket_count, amount_total,
  discount_amount, amount_payable, status, agent_id, pay_type, del_flag)
SELECT 'EO202605100001',
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  '1', 300.00, 2, 600.00, 0, 600.00, '0', (SELECT MIN(agent_id) FROM biz_env_agent), '1', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_order WHERE order_no = 'EO202605100001')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0');

INSERT INTO biz_env_order (order_no, merchant_id, order_type, oil_unit_price, oil_bucket_count, amount_total,
  discount_amount, amount_payable, status, agent_id, pay_type, del_flag)
SELECT 'EO202605100002',
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '潮味大排档' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  '2', 280.00, 1, 280.00, 0, 280.00, '1', (SELECT MIN(agent_id) FROM biz_env_agent), '2', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_order WHERE order_no = 'EO202605100002')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '潮味大排档' AND del_flag = '0');

INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag)
SELECT '1',
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  (SELECT MIN(agent_id) FROM biz_env_agent), 'DEV-GZ-0001', '1', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_device WHERE device_no = 'DEV-GZ-0001')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag)
SELECT '1', NULL, (SELECT MIN(agent_id) FROM biz_env_agent), 'DEV-WH-0002', '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_device WHERE device_no = 'DEV-WH-0002')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO biz_env_work_order (work_order_no, order_id, order_no, merchant_id, work_order_type, status, agent_id, del_flag)
SELECT 'WO202605100001', NULL, NULL,
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  '1', '0', (SELECT MIN(agent_id) FROM biz_env_agent), '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_work_order WHERE work_order_no = 'WO202605100001')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0');

-- user_role: 1主端 2代理 3业务员 4商家
INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'main-openid-001', '1', NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'main-openid-001');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'agent-openid-001', '2', (SELECT MIN(agent_id) FROM biz_env_agent), NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'agent-openid-001')
  AND EXISTS (SELECT 1 FROM biz_env_agent);

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'sales-openid-001', '3', (SELECT MIN(agent_id) FROM biz_env_agent), NULL,
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13800138001' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'sales-openid-001')
  AND EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13800138001');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'merchant-openid-001', '4', (SELECT MIN(agent_id) FROM biz_env_agent),
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'merchant-openid-001')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '粤海饭店' AND del_flag = '0');
