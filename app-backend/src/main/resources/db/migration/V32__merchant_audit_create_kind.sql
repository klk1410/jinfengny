-- 新建店铺审核：merchant_id 可空；audit_kind 区分资料修改(U)与新建(C)

ALTER TABLE biz_env_merchant_audit
  MODIFY COLUMN merchant_id BIGINT NULL COMMENT '店铺ID；新建店铺审核通过前为空',
  ADD COLUMN audit_kind CHAR(1) NOT NULL DEFAULT 'U' COMMENT 'U=资料修改 C=新建店铺' AFTER merchant_id;

CREATE INDEX idx_merchant_audit_kind_status ON biz_env_merchant_audit (audit_kind, status, del_flag);
