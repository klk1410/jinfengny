-- 审核管理入口：权限码须与 AppPortalJdbcService / V23 一致为 env:page:promo:merchant-audits
-- 修复手工改成 env:p:audits 后与 env_mini_role_perm、重复行不一致的问题

-- 1) 角色权限：已有规范码则只删错误码；否则把错误码升为规范码
INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT r.role_id, 'env:page:promo:merchant-audits'
FROM env_mini_role_perm r
WHERE r.perm_code = 'env:p:audits'
  AND NOT EXISTS (
    SELECT 1 FROM env_mini_role_perm x
    WHERE x.role_id = r.role_id AND x.perm_code = 'env:page:promo:merchant-audits'
  );

DELETE FROM env_mini_role_perm WHERE perm_code = 'env:p:audits';

-- 2) 门户：若已存在规范行，删除错误的重复行（自连接删除，避免 MySQL #1093）
DELETE bad
FROM env_portal_function bad
INNER JOIN env_portal_function good
  ON good.perm_code = 'env:page:promo:merchant-audits'
 AND good.del_flag = '0'
WHERE bad.perm_code = 'env:p:audits'
  AND bad.del_flag = '0';

-- 3) 仅剩错误码时，改正为规范码
UPDATE env_portal_function
SET perm_code = 'env:page:promo:merchant-audits',
    label      = '审核管理',
    icon       = '📋',
    route_path = '#/promo/merchant-audits',
    sort_order = 35
WHERE perm_code = 'env:p:audits'
  AND del_flag = '0';

-- 4) 统一展示文案与路由（规范行）
UPDATE env_portal_function
SET label = '审核管理',
    icon = '📋',
    route_path = '#/promo/merchant-audits',
    sort_order = 35
WHERE perm_code = 'env:page:promo:merchant-audits'
  AND del_flag = '0';
