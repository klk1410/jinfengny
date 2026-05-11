-- 九宫格增加「工单」入口（与 app-frontend 路由 #/work-orders 一致）

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:workorders', '工单', '🛠', '#/work-orders', 15, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '环保油业务' ORDER BY group_id DESC LIMIT 1) g;

INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES
 (1, 'env:page:workorders'),
 (2, 'env:page:workorders'),
 (3, 'env:page:workorders'),
 (4, 'env:page:workorders');
