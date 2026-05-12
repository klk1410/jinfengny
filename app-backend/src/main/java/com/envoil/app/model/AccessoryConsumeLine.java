package com.envoil.app.model;

import javax.validation.constraints.NotNull;

public class AccessoryConsumeLine {

    @NotNull(message = "配件种类不能为空")
    private Long typeId;

    /** 消耗数量，须大于 0 */
    @NotNull(message = "消耗数量不能为空")
    private Double qty;

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }
}
