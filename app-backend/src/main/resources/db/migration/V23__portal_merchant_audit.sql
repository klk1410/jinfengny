-- 店铺审核：主端/代理/业务员可见列表（业务员仅本人发起）

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:merchant-audits', '店铺审核', '📋', '#/promo/merchant-audits', 35, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g
WHERE NOT EXISTS (SELECT 1 FROM env_portal_function WHERE perm_code = 'env:page:promo:merchant-audits');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 1, 'env:page:promo:merchant-audits'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 1 AND perm_code = 'env:page:promo:merchant-audits');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 2, 'env:page:promo:merchant-audits'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 2 AND perm_code = 'env:page:promo:merchant-audits');

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 3, 'env:page:promo:merchant-audits'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 3 AND perm_code = 'env:page:promo:merchant-audits');
