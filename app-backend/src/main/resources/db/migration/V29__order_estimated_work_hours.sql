-- 订单确认时填写预计工作时间（小时），代理必填
ALTER TABLE biz_env_order
  ADD COLUMN estimated_work_hours DECIMAL(6,2) NULL COMMENT '预计工作时间（小时），代理确认时填写' AFTER work_order_no;
