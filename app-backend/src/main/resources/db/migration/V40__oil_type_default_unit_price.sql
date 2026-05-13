-- 油品种类：参考单价（元/桶），用于建档与列表展示；商家仍可单独维护 oil_unit_price

ALTER TABLE biz_env_oil_type
  ADD COLUMN default_unit_price DECIMAL(12, 4) NULL COMMENT '参考单价(元/桶)' AFTER liters_per_bucket;
