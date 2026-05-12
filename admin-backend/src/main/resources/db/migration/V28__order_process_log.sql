-- 订单流程节点日志（下单、确认、接单/指派、完工、取消等）
CREATE TABLE IF NOT EXISTS biz_env_order_process_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号',
  event_code VARCHAR(32) NOT NULL COMMENT '事件编码',
  event_title VARCHAR(255) NOT NULL COMMENT '展示文案（写入时固化）',
  actor_role CHAR(1) NULL COMMENT '操作人角色 1主端2代理3业务员4商家',
  actor_ref_id BIGINT NULL COMMENT '关联ID（如 agent_id / salesman_id / merchant_id）',
  operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (log_id),
  KEY idx_opp_order_no (order_no),
  KEY idx_opp_order_id (order_id)
) COMMENT='环保油-订单流程节点';
