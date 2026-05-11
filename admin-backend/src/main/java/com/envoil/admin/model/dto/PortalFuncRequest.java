package com.envoil.admin.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PortalFuncRequest {

    @NotNull(message = "groupId不能为空")
    private Long groupId;

    @NotBlank(message = "permCode不能为空")
    private String permCode;

    @NotBlank(message = "名称不能为空")
    private String label;

    private String icon;

    private String routePath;

    private Integer sortOrder;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
