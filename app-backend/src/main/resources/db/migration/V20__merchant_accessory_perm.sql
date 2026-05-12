-- 商家可查看本代理配件库存（只读九宫格）
INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 4, 'env:page:accessories'
WHERE NOT EXISTS (SELECT 1 FROM env_mini_role_perm WHERE role_id = 4 AND perm_code = 'env:page:accessories');
