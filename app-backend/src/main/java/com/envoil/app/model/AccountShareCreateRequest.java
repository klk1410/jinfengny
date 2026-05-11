package com.envoil.app.model;

import javax.validation.constraints.NotBlank;

public class AccountShareCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotBlank(message = "sharedOpenid不能为空")
    private String sharedOpenid;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getSharedOpenid() {
        return sharedOpenid;
    }

    public void setSharedOpenid(String sharedOpenid) {
        this.sharedOpenid = sharedOpenid;
    }
}

