package com.envoil.admin.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class MiniSubjectRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotNull(message = "roleId不能为空")
    private Long roleId;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
