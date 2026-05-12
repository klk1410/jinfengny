-- 业务员角色展示统一为「运维」（role_code 仍为 sales）；撤销此前误加的独立 ops 角色数据

DELETE FROM env_openid_biz_scope WHERE openid = 'ops-openid-001';
DELETE FROM env_mini_subject WHERE openid = 'ops-openid-001';
DELETE FROM env_mini_role_perm WHERE role_id IN (SELECT role_id FROM env_mini_role WHERE role_code = 'ops');
DELETE FROM env_mini_role WHERE role_code = 'ops';

UPDATE env_mini_role SET role_name = '运维' WHERE role_code = 'sales';
