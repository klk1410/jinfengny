package com.envoil.app.model;

import javax.validation.constraints.NotBlank;

public class MerchantAuditReviewRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    private String reviewRemark;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getReviewRemark() {
        return reviewRemark;
    }

    public void setReviewRemark(String reviewRemark) {
        this.reviewRemark = reviewRemark;
    }
}
