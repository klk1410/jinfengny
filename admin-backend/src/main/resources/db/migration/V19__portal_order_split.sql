-- 订单入口拆分：查询 / 提交

UPDATE env_portal_function
SET label = '订单查询', route_path = '#/orders'
WHERE perm_code = 'env:page:orders' AND del_flag = '0';

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:order_submit', '提交订单', '📝', '#/order/submit', 11, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g
WHERE NOT EXISTS (SELECT 1 FROM env_portal_function WHERE perm_code = 'env:page:order_submit');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 4, 'env:page:order_submit'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 4 AND perm_code = 'env:page:order_submit');
