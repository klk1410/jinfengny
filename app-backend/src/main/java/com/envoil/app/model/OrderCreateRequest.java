package com.envoil.app.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 非商家角色下单时必填；商家端由服务端绑定当前门店，可省略 */
    private Long merchantId;

    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    /** 可选；加油单服务端按门店油价计算，可不传 */
    private Double unitPrice;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少1桶")
    private Double bucketCount;

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

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }
}
