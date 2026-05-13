package com.envoil.app.model;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class OilTypeCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotBlank(message = "油品名称不能为空")
    @Size(max = 100, message = "油品名称过长")
    private String typeName;

    @NotNull(message = "密度不能为空")
    @DecimalMin(value = "0.0001", message = "密度须大于 0")
    private BigDecimal densityKgPerLiter;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价须大于 0")
    private BigDecimal unitPricePerBucket;

    /** 每桶折合升数；不传则默认 200 */
    @DecimalMin(value = "0.01", message = "每桶升数须大于 0")
    private BigDecimal litersPerBucket;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public BigDecimal getDensityKgPerLiter() {
        return densityKgPerLiter;
    }

    public void setDensityKgPerLiter(BigDecimal densityKgPerLiter) {
        this.densityKgPerLiter = densityKgPerLiter;
    }

    public BigDecimal getUnitPricePerBucket() {
        return unitPricePerBucket;
    }

    public void setUnitPricePerBucket(BigDecimal unitPricePerBucket) {
        this.unitPricePerBucket = unitPricePerBucket;
    }

    public BigDecimal getLitersPerBucket() {
        return litersPerBucket;
    }

    public void setLitersPerBucket(BigDecimal litersPerBucket) {
        this.litersPerBucket = litersPerBucket;
    }
}
