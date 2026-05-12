-- 审核管理入口：权限码须与 AppPortalJdbcService / V23 一致为 env:page:promo:merchant-audits
-- 修复手工改成 env:p:audits 后与 env_mini_role_perm、重复行不一致的问题

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT r.role_id, 'env:page:promo:merchant-audits'
FROM env_mini_role_perm r
WHERE r.perm_code = 'env:p:audits'
  AND NOT EXISTS (
    SELECT 1 FROM env_mini_role_perm x
    WHERE x.role_id = r.role_id AND x.perm_code = 'env:page:promo:merchant-audits'
  );

DELETE FROM env_mini_role_perm WHERE perm_code = 'env:p:audits';

DELETE bad
FROM env_portal_function bad
INNER JOIN env_portal_function good
  ON good.perm_code = 'env:page:promo:merchant-audits'
 AND good.del_flag = '0'
WHERE bad.perm_code = 'env:p:audits'
  AND bad.del_flag = '0';

UPDATE env_portal_function
SET perm_code = 'env:page:promo:merchant-audits',
    label      = '审核管理',
    icon       = '📋',
    route_path = '#/promo/merchant-audits',
    sort_order = 35
WHERE perm_code = 'env:p:audits'
  AND del_flag = '0';

UPDATE env_portal_function
SET label = '审核管理',
    icon = '📋',
    route_path = '#/promo/merchant-audits',
    sort_order = 35
WHERE perm_code = 'env:page:promo:merchant-audits'
  AND del_flag = '0';
