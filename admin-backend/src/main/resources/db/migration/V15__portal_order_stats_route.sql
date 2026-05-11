-- 订单统计：指向独立页面
UPDATE env_portal_function
SET route_path = '#/order/stats'
WHERE perm_code = 'env:p:order';

