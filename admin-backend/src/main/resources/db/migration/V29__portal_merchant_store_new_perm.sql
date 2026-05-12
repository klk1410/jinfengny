-- 与 app-backend V34 对齐：商家九宫格补充「新增店铺」「店铺审核」（独立 Flyway 历史时需在本模块执行）

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 4, 'env:page:promo:store-new'
WHERE NOT EXISTS (
  SELECT 1 FROM env_mini_role_perm WHERE role_id = 4 AND perm_code = 'env:page:promo:store-new'
);

INSERT INTO env_mini_role_perm (role_id, perm_code)
SELECT 4, 'env:page:promo:merchant-audits'
WHERE NOT EXISTS (
  SELECT 1 FROM env_mini_role_perm WHERE role_id = 4 AND perm_code = 'env:page:promo:merchant-audits'
);
