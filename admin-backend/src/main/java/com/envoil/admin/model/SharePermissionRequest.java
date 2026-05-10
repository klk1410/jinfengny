package com.envoil.admin.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class SharePermissionRequest {

    @NotBlank(message = "主账号openid不能为空")
    private String ownerOpenid;

    @NotBlank(message = "共享openid不能为空")
    private String shareOpenid;

    @NotEmpty(message = "授权权限不能为空")
    private List<String> grantedPerms;

    public String getOwnerOpenid() {
        return ownerOpenid;
    }

    public void setOwnerOpenid(String ownerOpenid) {
        this.ownerOpenid = ownerOpenid;
    }

    public String getShareOpenid() {
        return shareOpenid;
    }

    public void setShareOpenid(String shareOpenid) {
        this.shareOpenid = shareOpenid;
    }

    public List<String> getGrantedPerms() {
        return grantedPerms;
    }

    public void setGrantedPerms(List<String> grantedPerms) {
        this.grantedPerms = grantedPerms;
    }
}
