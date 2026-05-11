package com.envoil.admin.model.dto;

import java.util.List;

public class MiniRolePermBatchRequest {

    /** 传 null 或 [] 表示清空该角色全部门户权限 */
    private List<String> permCodes;

    public List<String> getPermCodes() {
        return permCodes;
    }

    public void setPermCodes(List<String> permCodes) {
        this.permCodes = permCodes;
    }
}
