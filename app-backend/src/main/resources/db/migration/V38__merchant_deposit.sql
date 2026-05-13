-- 门店押金（元），新建必填且 >= 0；历史数据默认 0

ALTER TABLE biz_env_merchant ADD COLUMN deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '押金(元)' AFTER merchant_commission;
