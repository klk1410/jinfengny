package com.envoil.app.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class MerchantCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 主端创建时必填 */
    private Long agentId;

    @NotBlank(message = "店铺名称不能为空")
    private String merchantName;

    private String contactName;
    private String contactPhone;
    private String industryType;
    private String province;
    private String city;
    private String district;
    private String addressDetail;

    @NotNull(message = "单价不能为空")
    private Double oilUnitPrice;

    private Double merchantCommission;
    private Long salesmanId;

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

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
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

    public String getIndustryType() {
        return industryType;
    }

    public void setIndustryType(String industryType) {
        this.industryType = industryType;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public Double getOilUnitPrice() {
        return oilUnitPrice;
    }

    public void setOilUnitPrice(Double oilUnitPrice) {
        this.oilUnitPrice = oilUnitPrice;
    }

    public Double getMerchantCommission() {
        return merchantCommission;
    }

    public void setMerchantCommission(Double merchantCommission) {
        this.merchantCommission = merchantCommission;
    }

    public Long getSalesmanId() {
        return salesmanId;
    }

    public void setSalesmanId(Long salesmanId) {
        this.salesmanId = salesmanId;
    }
}
