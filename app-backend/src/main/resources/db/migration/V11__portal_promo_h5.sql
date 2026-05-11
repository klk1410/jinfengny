-- 服务号九宫格：推广管理（hash 路由，与 app-frontend 一致）
-- 移除旧版小程序路径入口，避免九宫格重复

DELETE FROM env_mini_role_perm WHERE perm_code IN (
  'env:p:coop', 'env:p:store', 'env:p:dev', 'env:p:newcoop', 'env:p:newstore', 'env:p:withdraw', 'env:p:prepay'
);

DELETE FROM env_portal_function WHERE perm_code IN (
  'env:p:coop', 'env:p:store', 'env:p:dev', 'env:p:newcoop', 'env:p:newstore', 'env:p:withdraw', 'env:p:prepay'
);

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:coops', '合作管理', '🤝', '#/promo/coops', 10, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:coop-new', '新增合作', '➕', '#/promo/coop-new', 20, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:stores', '店铺管理', '🏪', '#/promo/stores', 30, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:store-new', '新增店铺', '🏪✚', '#/promo/store-new', 40, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:devices', '推广设备', '📦', '#/promo/devices', 50, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:withdraws', '提现管理', '💳', '#/promo/withdraws', 60, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag)
SELECT g.group_id, 'env:page:promo:prepaids', '预付款管理', '💰', '#/promo/prepaids', 70, '0', '0'
FROM (SELECT group_id FROM env_portal_func_group WHERE title = '推广管理' ORDER BY group_id ASC LIMIT 1) g;

-- 主端、代理：推广全套；业务员：合作/店铺/设备；商家：店铺与预付款（无新增店铺权限）
INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES
 (1, 'env:page:promo:coops'), (1, 'env:page:promo:coop-new'), (1, 'env:page:promo:stores'), (1, 'env:page:promo:store-new'),
 (1, 'env:page:promo:devices'), (1, 'env:page:promo:withdraws'), (1, 'env:page:promo:prepaids'),
 (2, 'env:page:promo:coops'), (2, 'env:page:promo:coop-new'), (2, 'env:page:promo:stores'), (2, 'env:page:promo:store-new'),
 (2, 'env:page:promo:devices'), (2, 'env:page:promo:withdraws'), (2, 'env:page:promo:prepaids'),
 (3, 'env:page:promo:coops'), (3, 'env:page:promo:coop-new'), (3, 'env:page:promo:stores'), (3, 'env:page:promo:store-new'),
 (3, 'env:page:promo:devices'),
 (4, 'env:page:promo:stores'), (4, 'env:page:promo:prepaids');
