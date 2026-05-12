-- 多代理仿真数据：便于联调「数据隔离 / 切换代理」类功能（幂等，可重复执行）

-- ---------- 代理（按联系电话唯一识别） ----------
INSERT INTO biz_env_agent (agent_name, contact_name, contact_phone, province, city, district, address_detail, status, del_flag)
SELECT '深圳南山服务站', '林主管', '13900000002', '广东省', '深圳市', '南山区', '科技园科苑路88号', '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0');

INSERT INTO biz_env_agent (agent_name, contact_name, contact_phone, province, city, district, address_detail, status, del_flag)
SELECT '东莞厚街服务站', '黄主管', '13900000003', '广东省', '东莞市', '厚街镇', '家具大道168号', '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0');

-- ---------- 业务员 ----------
INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag)
SELECT '周前锋', '13911110001',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13911110001')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002');

INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag)
SELECT '吴后勤', '13911110002',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13911110002')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002');

INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag)
SELECT '郑开拓', '13922220001',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13922220001')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003');

INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag)
SELECT '钱维护', '13922220002',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13922220002')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003');

-- ---------- 门店 ----------
INSERT INTO biz_env_merchant (
  agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  longitude, latitude, province, city, district, address_detail,
  oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag
)
SELECT
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13911110001' LIMIT 1),
  '餐饮', '深南茶餐厅', '刘店长', '13911110901',
  114.057868, 22.543099, '广东省', '深圳市', '南山区', '科技园南路101号',
  295.00, 9.00, 0.00, 1, '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '深南茶餐厅' AND del_flag = '0')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002');

INSERT INTO biz_env_merchant (
  agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  longitude, latitude, province, city, district, address_detail,
  oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag
)
SELECT
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13911110002' LIMIT 1),
  '酒店', '前海假日公寓', '孙前台', '13911110902',
  113.898582, 22.556481, '广东省', '深圳市', '宝安区', '新安街道创业二路66号',
  310.00, 10.00, 50.00, 0, '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '前海假日公寓' AND del_flag = '0')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002');

INSERT INTO biz_env_merchant (
  agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  longitude, latitude, province, city, district, address_detail,
  oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag
)
SELECT
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13922220001' LIMIT 1),
  '餐饮', '厚街砂锅粥', '冯老板', '13922220901',
  113.668372, 22.932965, '广东省', '东莞市', '厚街镇', '康乐南路208号',
  268.00, 8.00, 0.00, 1, '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '厚街砂锅粥' AND del_flag = '0')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003');

INSERT INTO biz_env_merchant (
  agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  longitude, latitude, province, city, district, address_detail,
  oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag
)
SELECT
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13922220002' LIMIT 1),
  '工厂', '莞南五金加工', '蒋厂长', '13922220902',
  113.752349, 23.020596, '广东省', '东莞市', '南城区', '宏图路99号',
  275.00, 8.50, 200.00, 0, '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '莞南五金加工' AND del_flag = '0')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003');

-- ---------- 设备（编号全局唯一） ----------
INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag)
SELECT '1',
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '深南茶餐厅' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  'DEV-SZ-SHOP-001', '1', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_device WHERE device_no = 'DEV-SZ-SHOP-001')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '深南茶餐厅' AND del_flag = '0');

INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag)
SELECT '1', NULL,
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  'DEV-SZ-STOCK-001', '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_device WHERE device_no = 'DEV-SZ-STOCK-001')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002');

INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag)
SELECT '1',
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '厚街砂锅粥' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  'DEV-DG-SHOP-001', '1', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_device WHERE device_no = 'DEV-DG-SHOP-001')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '厚街砂锅粥' AND del_flag = '0');

INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag)
SELECT '1', NULL,
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  'DEV-DG-STOCK-001', '0', '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_env_device WHERE device_no = 'DEV-DG-STOCK-001')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003');

-- ---------- 小程序账号范围（含门户角色 env_mini_subject） ----------
-- user_role: 1主端 2代理 3业务员 4商家

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'agent-openid-002', '2',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'agent-openid-002')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000002');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'agent-openid-003', '2',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'agent-openid-003')
  AND EXISTS (SELECT 1 FROM biz_env_agent WHERE contact_phone = '13900000003');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'sales-openid-002', '3',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  NULL,
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13911110001' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'sales-openid-002')
  AND EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13911110001');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'sales-openid-003', '3',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  NULL,
  (SELECT salesman_id FROM biz_env_salesman WHERE phone = '13922220001' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'sales-openid-003')
  AND EXISTS (SELECT 1 FROM biz_env_salesman WHERE phone = '13922220001');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'merchant-openid-002', '4',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000002' AND del_flag = '0' LIMIT 1),
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '深南茶餐厅' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'merchant-openid-002')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '深南茶餐厅' AND del_flag = '0');

INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id)
SELECT 'merchant-openid-003', '4',
  (SELECT agent_id FROM biz_env_agent WHERE contact_phone = '13900000003' AND del_flag = '0' LIMIT 1),
  (SELECT merchant_id FROM biz_env_merchant WHERE merchant_name = '厚街砂锅粥' AND del_flag = '0' ORDER BY merchant_id LIMIT 1),
  NULL
WHERE NOT EXISTS (SELECT 1 FROM env_openid_biz_scope WHERE openid = 'merchant-openid-003')
  AND EXISTS (SELECT 1 FROM biz_env_merchant WHERE merchant_name = '厚街砂锅粥' AND del_flag = '0');

INSERT INTO env_mini_subject (openid, role_id)
SELECT 'agent-openid-002', 2
WHERE NOT EXISTS (SELECT 1 FROM env_mini_subject WHERE openid = 'agent-openid-002');

INSERT INTO env_mini_subject (openid, role_id)
SELECT 'agent-openid-003', 2
WHERE NOT EXISTS (SELECT 1 FROM env_mini_subject WHERE openid = 'agent-openid-003');

INSERT INTO env_mini_subject (openid, role_id)
SELECT 'sales-openid-002', 3
WHERE NOT EXISTS (SELECT 1 FROM env_mini_subject WHERE openid = 'sales-openid-002');

INSERT INTO env_mini_subject (openid, role_id)
SELECT 'sales-openid-003', 3
WHERE NOT EXISTS (SELECT 1 FROM env_mini_subject WHERE openid = 'sales-openid-003');

INSERT INTO env_mini_subject (openid, role_id)
SELECT 'merchant-openid-002', 4
WHERE NOT EXISTS (SELECT 1 FROM env_mini_subject WHERE openid = 'merchant-openid-002');

INSERT INTO env_mini_subject (openid, role_id)
SELECT 'merchant-openid-003', 4
WHERE NOT EXISTS (SELECT 1 FROM env_mini_subject WHERE openid = 'merchant-openid-003');

-- 门店 device_count 与已装机设备数对齐（全表校正一次）
UPDATE biz_env_merchant m
SET device_count = COALESCE(
  (SELECT COUNT(*) FROM biz_env_device d WHERE d.merchant_id = m.merchant_id AND d.del_flag = '0'),
  0
)
WHERE m.del_flag = '0';
