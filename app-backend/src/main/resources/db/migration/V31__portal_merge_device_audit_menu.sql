-- 设备操作审核并入「审核管理」（仅保留 env:page:promo:merchant-audits 入口）；下架独立菜单及业务员对该入口的权限

UPDATE env_portal_function
SET del_flag = '1'
WHERE perm_code = 'env:page:promo:device-event-audits'
  AND del_flag = '0';

DELETE FROM env_mini_role_perm
WHERE perm_code = 'env:page:promo:device-event-audits';
