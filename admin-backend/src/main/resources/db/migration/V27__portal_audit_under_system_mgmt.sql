-- 审核管理：放入「系统管理」分组，服务号 H5 路由 #/promo/merchant-audits
-- 权限码保持与 AppPortalJdbcService / V23 一致：env:page:promo:merchant-audits

UPDATE env_portal_function f
INNER JOIN env_portal_func_group g ON g.title = '系统管理' AND g.del_flag = '0'
SET f.group_id = g.group_id,
    f.label = '审核管理',
    f.icon = '📋',
    f.route_path = '#/promo/merchant-audits',
    f.sort_order = 35
WHERE f.perm_code = 'env:page:promo:merchant-audits'
  AND f.del_flag = '0';

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id,
       'env:page:promo:merchant-audits',
       '审核管理',
       '📋',
       '#/promo/merchant-audits',
       35,
       '0',
       '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '系统管理' AND del_flag = '0' ORDER BY group_id ASC LIMIT 1) g
WHERE NOT EXISTS (
  SELECT 1 FROM env_portal_function f WHERE f.perm_code = 'env:page:promo:merchant-audits' AND f.del_flag = '0'
);
