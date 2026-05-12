-- 转移商家订单：设备当前所在门店为 merchant_id，目标门店为 to_merchant_id
ALTER TABLE biz_env_order ADD COLUMN to_merchant_id BIGINT NULL COMMENT '转移商家：目标门店' AFTER merchant_id;
