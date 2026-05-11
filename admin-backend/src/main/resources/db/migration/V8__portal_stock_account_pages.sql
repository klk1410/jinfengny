-- 九宫格：仓储库存 / 账目流水

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:stock', '仓储库存', '🛢', '#/stock', 60, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:ledger', '账目流水', '💳', '#/ledger', 70, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES
 (1, 'env:page:stock'), (1, 'env:page:ledger'),
 (2, 'env:page:stock'), (2, 'env:page:ledger'),
 (3, 'env:page:stock'),
 (4, 'env:page:ledger');
