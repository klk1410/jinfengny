package com.envoil.app.model;

import javax.validation.constraints.NotBlank;

public class WechatLoginRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }
}
