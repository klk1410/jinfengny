package com.envoil.app.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class DeviceEventCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 主端指定代理 */
    private Long agentId;

    @NotBlank(message = "设备编号不能为空")
    private String deviceNo;

    /** A 新增 R 移除 S 报废（报废仅对在库设备） */
    @NotBlank(message = "事件类型不能为空")
    @Pattern(regexp = "^[ARS]$", message = "事件类型须为 A（新增）、R（移除）或 S（报废）")
    private String eventType;

    private Long merchantId;
    private String remark;

    /** 事件为新增（A）时可省略或仅填 inbound（入库在库）；不再支持商家新增。 */
    private String addMode;

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

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAddMode() {
        return addMode;
    }

    public void setAddMode(String addMode) {
        this.addMode = addMode;
    }
}
