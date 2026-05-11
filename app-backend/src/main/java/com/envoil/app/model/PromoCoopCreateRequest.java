package com.envoil.app.model;

import javax.validation.constraints.NotBlank;

public class PromoCoopCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 主端指定代理；其它角色忽略 */
    private Long agentId;

    @NotBlank(message = "合作方名称不能为空")
    private String partnerName;

    private String contactName;
    private String contactPhone;
    private String remark;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
