-- 油品仓储：由「桶当量」改为以「吨」计量（与 biz_env_oil_type 密度、每桶升数一致）

ALTER TABLE biz_env_agent_stock
  MODIFY COLUMN total_qty DECIMAL(16, 6) DEFAULT 0,
  MODIFY COLUMN lock_qty DECIMAL(16, 6) DEFAULT 0,
  MODIFY COLUMN available_qty DECIMAL(16, 6) DEFAULT 0;

ALTER TABLE biz_env_agent_stock_flow
  MODIFY COLUMN change_qty DECIMAL(16, 6) DEFAULT 0;

-- 吨 = 桶当量 × liters_per_bucket × density_kg_per_liter / 1000
UPDATE biz_env_agent_stock s
JOIN biz_env_oil_type ot
  ON ot.oil_type_id = CAST(NULLIF(TRIM(s.stock_item_code), '') AS UNSIGNED)
  AND ot.del_flag = '0'
SET s.total_qty = ROUND(s.total_qty * ot.liters_per_bucket * ot.density_kg_per_liter / 1000, 6),
    s.lock_qty = ROUND(s.lock_qty * ot.liters_per_bucket * ot.density_kg_per_liter / 1000, 6),
    s.available_qty = ROUND(s.available_qty * ot.liters_per_bucket * ot.density_kg_per_liter / 1000, 6),
    s.unit_name = '吨'
WHERE s.stock_item_type = '1'
  AND IFNULL(TRIM(s.stock_item_code), '') <> ''
  AND s.del_flag = '0';

UPDATE biz_env_agent_stock_flow f
JOIN biz_env_agent_stock s ON s.stock_id = f.stock_id AND s.del_flag = '0'
JOIN biz_env_oil_type ot
  ON ot.oil_type_id = CAST(NULLIF(TRIM(s.stock_item_code), '') AS UNSIGNED)
  AND ot.del_flag = '0'
SET f.change_qty = ROUND(f.change_qty * ot.liters_per_bucket * ot.density_kg_per_liter / 1000, 6)
WHERE s.stock_item_type = '1'
  AND IFNULL(TRIM(s.stock_item_code), '') <> '';
