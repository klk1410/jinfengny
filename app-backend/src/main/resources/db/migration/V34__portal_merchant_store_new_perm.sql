-- 商家角色（role_id=4）：补充「新增店铺」「店铺审核」门户权限
-- 原 V11 注释为「无新增店铺」仅作用于九宫格；列表页按钮依赖 portal.roleCode，此处补齐入口与权限数据一致

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
