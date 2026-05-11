INSERT INTO biz_env_agent (agent_name, contact_name, contact_phone, province, city, status, del_flag)
VALUES ('广州一代', '陈总', '13900000001', '广东省', '广州市', '0', '0');

INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag) VALUES
('李师傅', '13800138001', 1, '0', '0'),
('赵师傅', '13800138002', 1, '0', '0');

INSERT INTO biz_env_merchant (agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone,
  province, city, oil_unit_price, merchant_commission, arrears_amount, device_count, status, del_flag)
VALUES
(1, 1, '餐饮', '粤海饭店', '张经理', '13800138000', '广东省', '广州市', 300.00, 10.00, 0.00, 2, '0', '0'),
(1, 2, '餐饮', '潮味大排档', '王老板', '13800138003', '广东省', '广州市', 280.00, 8.00, 120.00, 1, '0', '0');

INSERT INTO biz_env_order (order_no, merchant_id, order_type, oil_unit_price, oil_bucket_count, amount_total,
  discount_amount, amount_payable, status, agent_id, pay_type, del_flag)
VALUES
('EO202605100001', 1, '1', 300.00, 2, 600.00, 0, 600.00, '0', 1, '1', '0'),
('EO202605100002', 2, '2', 280.00, 1, 280.00, 0, 280.00, '1', 1, '2', '0');

INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag) VALUES
('1', 1, 1, 'DEV-GZ-0001', '1', '0'),
('1', NULL, 1, 'DEV-WH-0002', '0', '0');

INSERT INTO biz_env_work_order (work_order_no, order_id, order_no, merchant_id, work_order_type, status, agent_id, del_flag)
VALUES ('WO202605100001', NULL, NULL, 1, '1', '0', 1, '0');

-- user_role: 1主端 2代理 3业务员 4商家
INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id) VALUES
('main-openid-001', '1', NULL, NULL, NULL),
('agent-openid-001', '2', 1, NULL, NULL),
('sales-openid-001', '3', 1, NULL, 1),
('merchant-openid-001', '4', 1, 1, NULL);
