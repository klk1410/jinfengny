package com.envoil.app.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AccessoryCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    private Long merchantId;

    @NotNull(message = "配件种类不能为空")
    private Long typeId;

    @NotNull(message = "入库成本不能为空")
    @Min(value = 0, message = "入库成本不能小于0")
    private Double inboundCost;

    private String accCode;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量须大于0")
    private Double qty;

    /**
     * AGENT = 代理本人；SALESMAN:{id} = 业务员（须属于当前代理）。
     */
    @NotBlank(message = "入库操作人员不能为空")
    private String operatorKey;

    private String remark;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Double getInboundCost() {
        return inboundCost;
    }

    public void setInboundCost(Double inboundCost) {
        this.inboundCost = inboundCost;
    }

    public String getAccCode() {
        return accCode;
    }

    public void setAccCode(String accCode) {
        this.accCode = accCode;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public String getOperatorKey() {
        return operatorKey;
    }

    public void setOperatorKey(String operatorKey) {
        this.operatorKey = operatorKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
