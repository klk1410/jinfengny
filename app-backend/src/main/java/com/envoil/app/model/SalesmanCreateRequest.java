package com.envoil.app.model;

import javax.validation.constraints.NotBlank;

public class SalesmanCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotBlank(message = "业务员姓名不能为空")
    private String salesmanName;

    private String phone;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getSalesmanName() {
        return salesmanName;
    }

    public void setSalesmanName(String salesmanName) {
        this.salesmanName = salesmanName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
