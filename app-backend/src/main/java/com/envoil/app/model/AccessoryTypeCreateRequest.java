package com.envoil.app.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AccessoryTypeCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotBlank(message = "种类名称不能为空")
    @Size(max = 120, message = "种类名称过长")
    private String typeName;

    private Integer sortOrder;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
