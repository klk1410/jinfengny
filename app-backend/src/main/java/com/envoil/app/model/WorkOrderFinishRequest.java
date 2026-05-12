package com.envoil.app.model;

import java.util.List;

/**
 * 工单完工请求。业务员结单必填 accessoryConsumes（无消耗传空列表）。
 */
public class WorkOrderFinishRequest {

    private List<AccessoryConsumeLine> accessoryConsumes;

    public List<AccessoryConsumeLine> getAccessoryConsumes() {
        return accessoryConsumes;
    }

    public void setAccessoryConsumes(List<AccessoryConsumeLine> accessoryConsumes) {
        this.accessoryConsumes = accessoryConsumes;
    }
}
