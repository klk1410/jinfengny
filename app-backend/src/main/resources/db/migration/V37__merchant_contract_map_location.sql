-- 新建店铺：合同图片、地图定位说明。
-- 先将店铺图改为 MEDIUMTEXT，再新增合同图（同为 MEDIUMTEXT），避免 VARCHAR(8000)×2 与整表字段叠加触发 InnoDB 行宽上限（#1118）。

ALTER TABLE biz_env_merchant MODIFY COLUMN store_image_url MEDIUMTEXT NULL COMMENT '店铺图片';

ALTER TABLE biz_env_merchant ADD COLUMN contract_image_url MEDIUMTEXT NULL COMMENT '合同图片' AFTER store_image_url;

ALTER TABLE biz_env_merchant ADD COLUMN map_location_info VARCHAR(500) NULL COMMENT '地图定位信息' AFTER contract_image_url;
