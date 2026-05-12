package com.envoil.app.model;

import java.util.List;

/**
 * 工单完工请求。业务员结单必填 accessoryConsumes（无消耗传空列表）。
 */
public class WorkOrderFinishRequest {

    private List<AccessoryConsumeLine> accessoryConsumes;

    /** 转移商家工单（订单类型 4）完工时必填：要转移的设备编号 */
    private String deviceNo;

    public List<AccessoryConsumeLine> getAccessoryConsumes() {
        return accessoryConsumes;
    }

    public void setAccessoryConsumes(List<AccessoryConsumeLine> accessoryConsumes) {
        this.accessoryConsumes = accessoryConsumes;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }
}
