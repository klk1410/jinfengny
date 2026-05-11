package com.envoil.admin.model.dto;

import javax.validation.constraints.NotBlank;

public class PortalGroupRequest {

    @NotBlank(message = "分组标题不能为空")
    private String title;

    private Integer sortOrder;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
