package com.envoil.app.model;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 非商家角色下单时必填；商家端由服务端绑定当前门店，可省略 */
    private Long merchantId;

    /** 转移商家订单（orderType=4）：目标门店，须与 merchantId（源门店）同属代理且不相同 */
    private Long toMerchantId;

    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    /** 可选；加油单服务端按门店油价计算，可不传 */
    private Double unitPrice;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", inclusive = true, message = "数量须大于 0")
    private Double bucketCount;

    /**
     * 加油单数量对应的单位：桶 / 斤 / 升（或与 {@link com.envoil.app.util.OilQuantityConverter} 兼容的简写）。
     * 不传则按桶。
     */
    private String oilQtyUnit;

    @NotBlank(message = "支付方式不能为空")
    private String payType;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getToMerchantId() {
        return toMerchantId;
    }

    public void setToMerchantId(Long toMerchantId) {
        this.toMerchantId = toMerchantId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(Double bucketCount) {
        this.bucketCount = bucketCount;
    }

    public String getOilQtyUnit() {
        return oilQtyUnit;
    }

    public void setOilQtyUnit(String oilQtyUnit) {
        this.oilQtyUnit = oilQtyUnit;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }
}
