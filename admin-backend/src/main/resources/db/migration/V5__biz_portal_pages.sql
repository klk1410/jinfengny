-- 九宫格增加「环保油业务」子页（hash 路由，与 app-frontend vue-router 一致）

INSERT INTO env_portal_func_group (title, sort_order, status, del_flag) VALUES ('环保油业务', 55, '0', '0');

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:orders', '我的订单', '📋', '#/orders', 10, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:merchants', '商家', '🏪', '#/merchants', 20, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:agents', '代理', '🤝', '#/agents', 30, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:salesmen', '业务员', '👷', '#/salesmen', 40, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:devices', '设备', '📦', '#/devices', 50, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES
 (1, 'env:page:orders'), (1, 'env:page:merchants'), (1, 'env:page:agents'), (1, 'env:page:salesmen'), (1, 'env:page:devices'),
 (2, 'env:page:orders'), (2, 'env:page:merchants'), (2, 'env:page:agents'), (2, 'env:page:salesmen'), (2, 'env:page:devices'),
 (3, 'env:page:orders'), (3, 'env:page:merchants'), (3, 'env:page:devices'),
 (4, 'env:page:orders');
