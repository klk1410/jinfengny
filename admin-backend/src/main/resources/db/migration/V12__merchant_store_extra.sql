-- 新增店铺扩展：说明、店铺图、关联商家（与前端表单项对齐）

ALTER TABLE biz_env_merchant ADD COLUMN remark VARCHAR(500);
ALTER TABLE biz_env_merchant ADD COLUMN store_image_url VARCHAR(8000);
ALTER TABLE biz_env_merchant ADD COLUMN linked_merchant_id BIGINT;
