-- 环保油管理系统数据库设计（基于 MySQL 8.0，兼容若依字段风格）
-- 说明：
-- 1) 本脚本仅新增业务表，不修改若依系统表
-- 2) 业务与后台用户建议通过 sys_user.user_id 做关联
-- 3) 默认不建外键，遵循若依常见实践，使用索引保证查询性能

SET NAMES utf8mb4;

-- =========================
-- 1. 代理信息
-- =========================
DROP TABLE IF EXISTS biz_env_agent;
CREATE TABLE biz_env_agent (
  agent_id            BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '代理ID',
  agent_name          VARCHAR(100)    NOT NULL COMMENT '代理名称',
  contact_name        VARCHAR(50)     DEFAULT NULL COMMENT '联系人',
  contact_phone       VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
  province            VARCHAR(32)     DEFAULT NULL COMMENT '省',
  city                VARCHAR(32)     DEFAULT NULL COMMENT '市',
  district            VARCHAR(32)     DEFAULT NULL COMMENT '区',
  address_detail      VARCHAR(255)    DEFAULT NULL COMMENT '详细地址',
  status              CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (agent_id),
  KEY idx_env_agent_name (agent_name),
  KEY idx_env_agent_phone (contact_phone),
  KEY idx_env_agent_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-代理信息';

-- =========================
-- 2. 业务员信息
-- =========================
DROP TABLE IF EXISTS biz_env_salesman;
CREATE TABLE biz_env_salesman (
  salesman_id         BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '业务员ID',
  salesman_name       VARCHAR(50)     NOT NULL COMMENT '业务员名称',
  phone               VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
  agent_id            BIGINT(20)      NOT NULL COMMENT '所属代理ID',
  status              CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (salesman_id),
  KEY idx_env_salesman_agent (agent_id),
  KEY idx_env_salesman_phone (phone),
  KEY idx_env_salesman_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-业务员信息';

-- =========================
-- 3. 商家信息
-- =========================
DROP TABLE IF EXISTS biz_env_merchant;
CREATE TABLE biz_env_merchant (
  merchant_id         BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '商家ID',
  agent_id            BIGINT(20)      NOT NULL COMMENT '所属代理ID',
  salesman_id         BIGINT(20)      DEFAULT NULL COMMENT '归属业务员ID',
  industry_type       VARCHAR(50)     DEFAULT NULL COMMENT '所属行业',
  merchant_name       VARCHAR(100)    NOT NULL COMMENT '商家名称',
  contact_name        VARCHAR(50)     DEFAULT NULL COMMENT '联系人',
  contact_phone       VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
  longitude           DECIMAL(10,6)   DEFAULT NULL COMMENT '经度',
  latitude            DECIMAL(10,6)   DEFAULT NULL COMMENT '纬度',
  province            VARCHAR(32)     DEFAULT NULL COMMENT '省',
  city                VARCHAR(32)     DEFAULT NULL COMMENT '市',
  district            VARCHAR(32)     DEFAULT NULL COMMENT '区',
  address_detail      VARCHAR(255)    DEFAULT NULL COMMENT '详细地址',
  oil_unit_price      DECIMAL(10,2)   DEFAULT 0.00 COMMENT '油单价（元/桶）',
  merchant_commission DECIMAL(10,2)   DEFAULT 0.00 COMMENT '商家提成（元）',
  arrears_amount      DECIMAL(12,2)   DEFAULT 0.00 COMMENT '欠款金额（元）',
  device_count        INT(11)         DEFAULT 0 COMMENT '设备数量（冗余字段）',
  status              CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (merchant_id),
  KEY idx_env_merchant_agent (agent_id),
  KEY idx_env_merchant_salesman (salesman_id),
  KEY idx_env_merchant_phone (contact_phone),
  KEY idx_env_merchant_name (merchant_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-商家信息';

-- 商家图片（广告图、门头照）
DROP TABLE IF EXISTS biz_env_merchant_image;
CREATE TABLE biz_env_merchant_image (
  image_id            BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  merchant_id         BIGINT(20)      NOT NULL COMMENT '商家ID',
  image_type          CHAR(1)         NOT NULL COMMENT '图片类型（1广告图 2门头照）',
  image_url           VARCHAR(500)    NOT NULL COMMENT '图片地址',
  sort_num            INT(11)         DEFAULT 0 COMMENT '排序',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (image_id),
  KEY idx_env_merchant_image_mid (merchant_id),
  KEY idx_env_merchant_image_type (image_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-商家图片信息';

-- =========================
-- 4. 订单信息
-- =========================
DROP TABLE IF EXISTS biz_env_order;
CREATE TABLE biz_env_order (
  order_id            BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  order_no            VARCHAR(32)     NOT NULL COMMENT '订单号',
  order_time          DATETIME        DEFAULT NULL COMMENT '下单时间',
  merchant_id         BIGINT(20)      NOT NULL COMMENT '所属门店（商家ID）',
  order_type          CHAR(1)         NOT NULL COMMENT '订单类型（1加油 2维护）',
  oil_unit_price      DECIMAL(10,2)   DEFAULT 0.00 COMMENT '油单价（元/桶）',
  oil_bucket_count    DECIMAL(10,2)   DEFAULT 0.00 COMMENT '数量（桶）',
  amount_total        DECIMAL(12,2)   DEFAULT 0.00 COMMENT '订单金额（自动计算）',
  discount_amount     DECIMAL(12,2)   DEFAULT 0.00 COMMENT '优惠金额',
  amount_payable      DECIMAL(12,2)   DEFAULT 0.00 COMMENT '应付金额',
  status              CHAR(1)         DEFAULT '0' COMMENT '状态（0待确认 1待分配 2已接收 3已完成 4订单取消）',
  agent_id            BIGINT(20)      NOT NULL COMMENT '所属代理ID',
  pay_type            CHAR(1)         DEFAULT '1' COMMENT '支付类型（1现结 2赊欠）',
  receive_salesman_id BIGINT(20)      DEFAULT NULL COMMENT '接单师傅（已接收/已完成/取消才确认）',
  work_order_no       VARCHAR(32)     DEFAULT NULL COMMENT '处理工单号',
  cancel_reason       VARCHAR(255)    DEFAULT NULL COMMENT '取消原因',
  cancel_time         DATETIME        DEFAULT NULL COMMENT '取消时间',
  finish_time         DATETIME        DEFAULT NULL COMMENT '完成时间',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_env_order_no (order_no),
  KEY idx_env_order_merchant (merchant_id),
  KEY idx_env_order_agent (agent_id),
  KEY idx_env_order_status (status),
  KEY idx_env_order_time (order_time),
  KEY idx_env_order_salesman (receive_salesman_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-订单信息';

-- 订单状态流水（用于推送与追踪）
DROP TABLE IF EXISTS biz_env_order_log;
CREATE TABLE biz_env_order_log (
  log_id              BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  order_id            BIGINT(20)      NOT NULL COMMENT '订单ID',
  order_no            VARCHAR(32)     NOT NULL COMMENT '订单号',
  old_status          CHAR(1)         DEFAULT NULL COMMENT '原状态',
  new_status          CHAR(1)         NOT NULL COMMENT '新状态',
  operation_user_id   BIGINT(20)      DEFAULT NULL COMMENT '操作人ID',
  operation_role      CHAR(1)         DEFAULT NULL COMMENT '操作角色（1主端 2代理 3业务员 4商家）',
  operation_time      DATETIME        DEFAULT NULL COMMENT '操作时间',
  operation_content   VARCHAR(255)    DEFAULT NULL COMMENT '操作描述',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (log_id),
  KEY idx_env_order_log_oid (order_id),
  KEY idx_env_order_log_no (order_no),
  KEY idx_env_order_log_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-订单状态流水';

-- =========================
-- 5. 工单信息
-- =========================
DROP TABLE IF EXISTS biz_env_work_order;
CREATE TABLE biz_env_work_order (
  work_order_id       BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  work_order_no       VARCHAR(32)     NOT NULL COMMENT '工单号',
  order_id            BIGINT(20)      DEFAULT NULL COMMENT '来源订单ID',
  order_no            VARCHAR(32)     DEFAULT NULL COMMENT '来源订单号',
  work_order_time     DATETIME        DEFAULT NULL COMMENT '下单时间',
  merchant_id         BIGINT(20)      NOT NULL COMMENT '所属门店（商家ID）',
  work_order_type     CHAR(1)         NOT NULL COMMENT '工单类型（1加油 2维护 3外出访问）',
  work_start_time     DATETIME        DEFAULT NULL COMMENT '工作开始时间',
  work_end_time       DATETIME        DEFAULT NULL COMMENT '工作结束时间',
  status              CHAR(1)         DEFAULT '0' COMMENT '工单状态（0待确认 1待分配 2已接收 3已完成 4工单取消）',
  agent_id            BIGINT(20)      NOT NULL COMMENT '所属代理ID',
  receive_salesman_id BIGINT(20)      DEFAULT NULL COMMENT '接单师傅ID',
  assign_type         CHAR(1)         DEFAULT '1' COMMENT '分配方式（1自动抢单 2代理指派）',
  accept_deadline     DATETIME        DEFAULT NULL COMMENT '抢单截止时间（默认生成后5分钟）',
  finish_time         DATETIME        DEFAULT NULL COMMENT '完成时间',
  cancel_reason       VARCHAR(255)    DEFAULT NULL COMMENT '取消原因',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (work_order_id),
  UNIQUE KEY uk_env_work_order_no (work_order_no),
  KEY idx_env_work_order_order (order_id),
  KEY idx_env_work_order_merchant (merchant_id),
  KEY idx_env_work_order_agent (agent_id),
  KEY idx_env_work_order_status (status),
  KEY idx_env_work_order_salesman (receive_salesman_id),
  KEY idx_env_work_order_deadline (accept_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-工单信息';

-- 工单状态流水
DROP TABLE IF EXISTS biz_env_work_order_log;
CREATE TABLE biz_env_work_order_log (
  log_id              BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  work_order_id       BIGINT(20)      NOT NULL COMMENT '工单ID',
  work_order_no       VARCHAR(32)     NOT NULL COMMENT '工单号',
  old_status          CHAR(1)         DEFAULT NULL COMMENT '原状态',
  new_status          CHAR(1)         NOT NULL COMMENT '新状态',
  operation_user_id   BIGINT(20)      DEFAULT NULL COMMENT '操作人ID',
  operation_role      CHAR(1)         DEFAULT NULL COMMENT '操作角色（1主端 2代理 3业务员 4商家）',
  operation_time      DATETIME        DEFAULT NULL COMMENT '操作时间',
  operation_content   VARCHAR(255)    DEFAULT NULL COMMENT '操作描述',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (log_id),
  KEY idx_env_wo_log_woid (work_order_id),
  KEY idx_env_wo_log_wono (work_order_no),
  KEY idx_env_wo_log_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-工单状态流水';

-- =========================
-- 6. 设备信息
-- =========================
DROP TABLE IF EXISTS biz_env_device;
CREATE TABLE biz_env_device (
  device_id           BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  device_type         CHAR(1)         NOT NULL COMMENT '设备类型（1加油设备 2维护设备 3其他）',
  merchant_id         BIGINT(20)      DEFAULT NULL COMMENT '所属门店ID',
  agent_id            BIGINT(20)      NOT NULL COMMENT '所属代理ID',
  device_no           VARCHAR(64)     NOT NULL COMMENT '设备编号',
  device_status       CHAR(1)         DEFAULT '0' COMMENT '设备状态（0在库 1在商家 2维修中 3停用）',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (device_id),
  UNIQUE KEY uk_env_device_no (device_no),
  KEY idx_env_device_merchant (merchant_id),
  KEY idx_env_device_agent (agent_id),
  KEY idx_env_device_status (device_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-设备信息';

-- =========================
-- 7. 用户管理（服务号/小程序用户）
-- =========================
DROP TABLE IF EXISTS biz_env_user;
CREATE TABLE biz_env_user (
  env_user_id         BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '业务用户ID',
  sys_user_id         BIGINT(20)      DEFAULT NULL COMMENT '关联后台用户ID（sys_user.user_id）',
  wechat_openid       VARCHAR(64)     NOT NULL COMMENT '微信openid',
  user_role           CHAR(1)         NOT NULL COMMENT '所属权限（1主端 2代理 3业务员 4商家）',
  owner_main_user_id  BIGINT(20)      DEFAULT NULL COMMENT '所属主端用户ID（主端自身为空）',
  owner_agent_id      BIGINT(20)      DEFAULT NULL COMMENT '所属代理ID',
  is_share            CHAR(1)         DEFAULT '0' COMMENT '是否共享（0否 1是）',
  share_wechat_openid VARCHAR(64)     DEFAULT NULL COMMENT '共享微信openid',
  status              CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1禁用）',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (env_user_id),
  UNIQUE KEY uk_env_user_openid (wechat_openid),
  KEY idx_env_user_sys_uid (sys_user_id),
  KEY idx_env_user_role (user_role),
  KEY idx_env_user_agent (owner_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-业务用户信息';

-- =========================
-- 8. 账目流水
-- =========================
DROP TABLE IF EXISTS biz_env_account_flow;
CREATE TABLE biz_env_account_flow (
  flow_id             BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  env_user_id         BIGINT(20)      NOT NULL COMMENT '业务用户ID',
  user_role           CHAR(1)         NOT NULL COMMENT '所属权限（1主端 2代理 3业务员 4商家）',
  user_name           VARCHAR(100)    NOT NULL COMMENT '名称',
  biz_type            CHAR(1)         DEFAULT '1' COMMENT '业务类型（1订单 2工单 3仓储 4手工调整）',
  related_no          VARCHAR(32)     DEFAULT NULL COMMENT '关联单号（订单号/工单号）',
  income_amount       DECIMAL(12,2)   DEFAULT 0.00 COMMENT '收入金额',
  expense_amount      DECIMAL(12,2)   DEFAULT 0.00 COMMENT '支出金额',
  balance_after       DECIMAL(12,2)   DEFAULT NULL COMMENT '变更后余额',
  flow_time           DATETIME        DEFAULT NULL COMMENT '流水时间',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (flow_id),
  KEY idx_env_account_user (env_user_id),
  KEY idx_env_account_role (user_role),
  KEY idx_env_account_related_no (related_no),
  KEY idx_env_account_time (flow_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-账目流水';

-- =========================
-- 9. 代理仓储（来自业务逻辑扩展）
-- =========================
DROP TABLE IF EXISTS biz_env_agent_stock;
CREATE TABLE biz_env_agent_stock (
  stock_id            BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  agent_id            BIGINT(20)      NOT NULL COMMENT '代理ID',
  stock_item_type     CHAR(1)         NOT NULL COMMENT '库存项类型（1环保油 2设备）',
  stock_item_code     VARCHAR(64)     DEFAULT NULL COMMENT '库存项编码（设备可用设备编号）',
  stock_item_name     VARCHAR(100)    NOT NULL COMMENT '库存项名称',
  unit_name           VARCHAR(20)     DEFAULT '桶' COMMENT '单位',
  total_qty           DECIMAL(12,2)   DEFAULT 0.00 COMMENT '总库存',
  lock_qty            DECIMAL(12,2)   DEFAULT 0.00 COMMENT '预扣库存',
  available_qty       DECIMAL(12,2)   DEFAULT 0.00 COMMENT '可用库存',
  status              CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  del_flag            CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (stock_id),
  KEY idx_env_stock_agent (agent_id),
  KEY idx_env_stock_type (stock_item_type),
  KEY idx_env_stock_code (stock_item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-代理仓储主表';

DROP TABLE IF EXISTS biz_env_agent_stock_flow;
CREATE TABLE biz_env_agent_stock_flow (
  flow_id             BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '库存流水ID',
  stock_id            BIGINT(20)      NOT NULL COMMENT '库存ID',
  agent_id            BIGINT(20)      NOT NULL COMMENT '代理ID',
  change_type         CHAR(1)         NOT NULL COMMENT '变更类型（1手工入库 2订单预扣 3订单完成扣减 4订单取消回滚）',
  related_no          VARCHAR(32)     DEFAULT NULL COMMENT '关联单号',
  change_qty          DECIMAL(12,2)   DEFAULT 0.00 COMMENT '变更数量',
  qty_before          DECIMAL(12,2)   DEFAULT 0.00 COMMENT '变更前数量',
  qty_after           DECIMAL(12,2)   DEFAULT 0.00 COMMENT '变更后数量',
  operation_user_id   BIGINT(20)      DEFAULT NULL COMMENT '操作人ID',
  operation_time      DATETIME        DEFAULT NULL COMMENT '操作时间',
  create_by           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
  remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (flow_id),
  KEY idx_env_stock_flow_sid (stock_id),
  KEY idx_env_stock_flow_agent (agent_id),
  KEY idx_env_stock_flow_related_no (related_no),
  KEY idx_env_stock_flow_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环保油-代理仓储流水';
