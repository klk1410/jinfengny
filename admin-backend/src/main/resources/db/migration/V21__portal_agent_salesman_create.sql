-- 九宫格：新增代理（仅主端）、新增业务员（仅代理）

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:agent_new', '新增代理', '➕', '#/agents/new', 31, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g
WHERE NOT EXISTS (SELECT 1 FROM env_portal_function WHERE perm_code = 'env:page:agent_new');

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:salesman_new', '新增业务员', '➕', '#/salesmen/new', 41, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g
WHERE NOT EXISTS (SELECT 1 FROM env_portal_function WHERE perm_code = 'env:page:salesman_new');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 1, 'env:page:agent_new'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 1 AND perm_code = 'env:page:agent_new');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 2, 'env:page:salesman_new'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 2 AND perm_code = 'env:page:salesman_new');
