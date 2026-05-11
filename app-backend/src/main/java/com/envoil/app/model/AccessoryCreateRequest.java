package com.envoil.app.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AccessoryCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 主端创建时必填 */
    private Long agentId;

    private Long merchantId;

    @NotBlank(message = "配件名称不能为空")
    private String accessoryName;

    @NotNull(message = "数量不能为空")
    private Double qty;

    @NotNull(message = "单价不能为空")
    private Double unitPrice;

    private String remark;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getAccessoryName() {
        return accessoryName;
    }

    public void setAccessoryName(String accessoryName) {
        this.accessoryName = accessoryName;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

