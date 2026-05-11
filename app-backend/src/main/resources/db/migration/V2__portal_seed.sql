-- 分组 + 门户功能示例数据 + 小程序角色映射（与用户截图九宫格对齐，属示例可后台改）

INSERT INTO env_portal_func_group (title, sort_order) VALUES ('统计管理', 10), ('推广管理', 20), ('售后管理', 30),
  ('系统管理', 40), ('异常管理', 50);

INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order) VALUES
 (1,'env:p:earning','收益统计','🧾','/pages/stat/earning',10),
 (1,'env:p:order','订单统计','📋','/pages/stat/order',20),
 (1,'env:p:data','数据统计','📈','/pages/stat/data',30),
 (2,'env:p:coop','合作管理','🤝','/pages/promo/coop',10),
 (2,'env:p:store','店铺管理','🏪','/pages/promo/store',20),
 (2,'env:p:dev','设备管理','📦','/pages/promo/device',30),
 (2,'env:p:newcoop','新增合作','➕','/pages/promo/new-co',40),
 (2,'env:p:newstore','新增店铺','🏪✚','/pages/promo/new-st',50),
 (2,'env:p:withdraw','提现管理','💳','/pages/promo/with',60),
 (2,'env:p:prepay','预付款管理','💳','/pages/promo/prep',70),
 (3,'env:p:acct','账户信息','👤','/pages/as/acct',10),
 (3,'env:p:vip','VIP管理','🅥','/pages/as/vip',20),
 (3,'env:p:ctrl','设备控制','🎛','/pages/as/ctrl',30),
 (3,'env:p:poplog','弹出记录','🎧','/pages/as/pop',40),
 (3,'env:p:battery','电池管理','🔧','/pages/as/bat',50),
 (3,'env:p:cs','联系客服','💬','/pages/as/cs',60),
 (3,'env:p:rmvlog','设备移除日志','📋','/pages/as/rmv',70),
 (3,'env:p:recharge','会员充值记录','💳','/pages/as/rcg',80),
 (3,'env:p:modlog','修改日志','📋','/pages/as/mod',90),
 (4,'env:p:m1','管理一','⚙','/pages/sys/m1',10),
 (4,'env:p:one','一键管理','⚙','/pages/sys/one',20),
 (4,'env:p:pkg','会员套餐','📇','/pages/sys/pkg',30),
 (4,'env:p:anno','公告管理','⚙','/pages/sys/anno',40),
 (4,'env:p:deposit','押金统计','📊','/pages/sys/dep',50),
 (4,'env:p:m2','管理二','⚙','/pages/sys/m2',60),
 (5,'env:p:abstore','异常店铺','🏪','/pages/ab/store',10),
 (5,'env:p:abdev','异常设备','🏪','/pages/ab/dev',20),
 (5,'env:p:abbat','异常电池','🏪','/pages/ab/bat',30);

INSERT INTO env_mini_role (role_code, role_name, sort_order) VALUES
 ('main','主端',10),
 ('agent','代理',20),
 ('sales','业务员',30),
 ('merchant','商家',40),
 ('guest','未授权',90);

INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES
 (1,'env:p:earning'),(1,'env:p:order'),(1,'env:p:data'),
 (1,'env:p:coop'),(1,'env:p:store'),(1,'env:p:dev'),(1,'env:p:newcoop'),(1,'env:p:newstore'),(1,'env:p:withdraw'),(1,'env:p:prepay'),
 (1,'env:p:acct'),(1,'env:p:vip'),(1,'env:p:ctrl'),(1,'env:p:poplog'),(1,'env:p:battery'),(1,'env:p:cs'),(1,'env:p:rmvlog'),(1,'env:p:recharge'),(1,'env:p:modlog'),
 (1,'env:p:m1'),(1,'env:p:one'),(1,'env:p:pkg'),(1,'env:p:anno'),(1,'env:p:deposit'),(1,'env:p:m2'),
 (1,'env:p:abstore'),(1,'env:p:abdev'),(1,'env:p:abbat'),
 (2,'env:p:earning'),(2,'env:p:order'),(2,'env:p:data'),
 (2,'env:p:coop'),(2,'env:p:store'),(2,'env:p:dev'),(2,'env:p:newcoop'),(2,'env:p:newstore'),(2,'env:p:withdraw'),(2,'env:p:prepay'),
 (2,'env:p:acct'),(2,'env:p:poplog'),(2,'env:p:battery'),(2,'env:p:cs'),(2,'env:p:rmvlog'),(2,'env:p:recharge'),(2,'env:p:modlog'),
 (2,'env:p:m1'),(2,'env:p:one'),(2,'env:p:pkg'),(2,'env:p:anno'),(2,'env:p:deposit'),(2,'env:p:m2'),
 (2,'env:p:abstore'),(2,'env:p:abdev'),(2,'env:p:abbat'),
 (3,'env:p:order'),(3,'env:p:acct'),(3,'env:p:poplog'),(3,'env:p:battery'),(3,'env:p:cs'),
 (3,'env:p:modlog'),(3,'env:p:earning'),
 (4,'env:p:acct'),(4,'env:p:coop'),(4,'env:p:order'),(4,'env:p:data'),
 (5,'env:p:acct');

INSERT INTO env_mini_subject (openid, role_id) VALUES
 ('main-openid-001',1),
 ('agent-openid-001',2),
 ('sales-openid-001',3),
 ('merchant-openid-001',4);
