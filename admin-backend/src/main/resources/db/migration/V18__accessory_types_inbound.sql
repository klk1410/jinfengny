-- 配件种类（管理后台维护）与入库扩展字段

CREATE TABLE biz_env_accessory_type (
  type_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  type_name   VARCHAR(120) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag    CHAR(1) NOT NULL DEFAULT '0'
);

INSERT INTO biz_env_accessory_type (type_name, sort_order, del_flag)
VALUES ('未分类', 0, '0');

ALTER TABLE biz_env_accessory ADD COLUMN type_id BIGINT NULL;

UPDATE biz_env_accessory a
CROSS JOIN (SELECT MIN(type_id) AS tid FROM biz_env_accessory_type WHERE del_flag = '0') x
SET a.type_id = x.tid
WHERE a.type_id IS NULL;

ALTER TABLE biz_env_accessory MODIFY COLUMN type_id BIGINT NOT NULL;

ALTER TABLE biz_env_accessory ADD COLUMN acc_code VARCHAR(64) NULL;
ALTER TABLE biz_env_accessory ADD COLUMN inbound_cost DECIMAL(12,2) NOT NULL DEFAULT 0;
UPDATE biz_env_accessory SET inbound_cost = ROUND(COALESCE(qty, 0) * COALESCE(unit_price, 0), 2);
ALTER TABLE biz_env_accessory ADD COLUMN operator_kind CHAR(1) NOT NULL DEFAULT '2';
ALTER TABLE biz_env_accessory ADD COLUMN operator_id BIGINT NULL;
