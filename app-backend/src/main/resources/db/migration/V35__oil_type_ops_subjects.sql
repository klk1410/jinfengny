-- 油品种类、订单计量单位、运维角色与库存行规范化

CREATE TABLE IF NOT EXISTS biz_env_oil_type (
  oil_type_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  type_name             VARCHAR(100) NOT NULL,
  density_kg_per_liter  DECIMAL(10,4) NOT NULL COMMENT '密度 kg/L',
  liters_per_bucket     DECIMAL(12,4) NOT NULL DEFAULT 200 COMMENT '每桶折合升数',
  sort_order            INT DEFAULT 0,
  status                CHAR(1) DEFAULT '0',
  del_flag              CHAR(1) DEFAULT '0'
);

INSERT INTO biz_env_oil_type (oil_type_id, type_name, density_kg_per_liter, liters_per_bucket, sort_order, status, del_flag)
SELECT 1, '标准环保油', 0.8500, 200.0000, 10, '0', '0'
FROM (SELECT 1 AS _) z
WHERE NOT EXISTS (SELECT 1 FROM biz_env_oil_type WHERE oil_type_id = 1);

INSERT INTO biz_env_oil_type (type_name, density_kg_per_liter, liters_per_bucket, sort_order, status, del_flag)
SELECT '轻质调和油', 0.8200, 200.0000, 20, '0', '0'
FROM (SELECT 1 AS _) z
WHERE NOT EXISTS (SELECT 1 FROM biz_env_oil_type WHERE type_name = '轻质调和油' AND del_flag = '0');

ALTER TABLE biz_env_merchant ADD COLUMN oil_type_id BIGINT NULL COMMENT '主营油品' AFTER oil_unit_price;

ALTER TABLE biz_env_order ADD COLUMN oil_qty_unit CHAR(1) NOT NULL DEFAULT 'B' COMMENT 'B桶 J斤 L升' AFTER oil_bucket_count;

UPDATE biz_env_merchant SET oil_type_id = 1 WHERE oil_type_id IS NULL AND del_flag = '0';

UPDATE biz_env_agent_stock
SET stock_item_code = '1',
    stock_item_name = COALESCE(NULLIF(TRIM(stock_item_name), ''), '标准环保油')
WHERE stock_item_type = '1'
  AND del_flag = '0'
  AND (stock_item_code IS NULL OR TRIM(stock_item_code) = '');
