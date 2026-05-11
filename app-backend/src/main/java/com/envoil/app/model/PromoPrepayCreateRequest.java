package com.envoil.app.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class PromoPrepayCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 主端可指定代理 */
    private Long agentId;

    private Long merchantId;

    @NotBlank(message = "摘要不能为空")
    private String title;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额须大于0")
    private Double amount;

    /** 1 入账 2 支出 */
    @NotBlank(message = "方向不能为空")
    private String direction;

    private String refNote;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getRefNote() {
        return refNote;
    }

    public void setRefNote(String refNote) {
        this.refNote = refNote;
    }
}
