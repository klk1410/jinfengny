package com.envoil.app.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class PromoWithdrawApplyRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额须大于0")
    private Double amount;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
