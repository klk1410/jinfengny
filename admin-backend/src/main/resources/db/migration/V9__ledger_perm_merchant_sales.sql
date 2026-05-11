-- 账目流水仅主端/代理可见：撤销业务员、商家九宫格权限

DELETE FROM env_mini_role_perm WHERE perm_code = 'env:page:ledger' AND role_id IN (3, 4);
