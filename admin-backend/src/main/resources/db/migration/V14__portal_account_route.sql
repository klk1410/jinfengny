-- 账户信息页面：从占位页改为真实路由
UPDATE env_portal_function
SET route_path = '#/account/profile'
WHERE perm_code = 'env:p:acct';

