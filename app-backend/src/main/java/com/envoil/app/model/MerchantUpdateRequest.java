package com.envoil.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 店铺资料修改（代理/主端直改；业务员改走审核，提交时同结构并带 submitRemark）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantUpdateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotNull(message = "店铺ID不能为空")
    private Long merchantId;

    @NotBlank(message = "所属行业不能为空")
    private String industryType;

    @NotBlank(message = "店铺名称不能为空")
    private String merchantName;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @NotBlank(message = "省不能为空")
    private String province;

    @NotBlank(message = "市不能为空")
    private String city;

    @NotBlank(message = "区不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String addressDetail;

    @NotNull(message = "经度不能为空")
    private Double longitude;

    @NotNull(message = "纬度不能为空")
    private Double latitude;

    private Double oilUnitPrice;
    private Long oilTypeId;
    private Double merchantCommission;
    private Long salesmanId;
    private Long linkedMerchantId;
    private String remark;
    private String storeImageUrl;

    /** 业务员发起审核时的说明（直改接口可忽略） */
    private String submitRemark;

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

    public String getIndustryType() {
        return industryType;
    }

    public void setIndustryType(String industryType) {
        this.industryType = industryType;
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

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getOilUnitPrice() {
        return oilUnitPrice;
    }

    public void setOilUnitPrice(Double oilUnitPrice) {
        this.oilUnitPrice = oilUnitPrice;
    }

    public Long getOilTypeId() {
        return oilTypeId;
    }

    public void setOilTypeId(Long oilTypeId) {
        this.oilTypeId = oilTypeId;
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

    public Long getLinkedMerchantId() {
        return linkedMerchantId;
    }

    public void setLinkedMerchantId(Long linkedMerchantId) {
        this.linkedMerchantId = linkedMerchantId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStoreImageUrl() {
        return storeImageUrl;
    }

    public void setStoreImageUrl(String storeImageUrl) {
        this.storeImageUrl = storeImageUrl;
    }

    public String getSubmitRemark() {
        return submitRemark;
    }

    public void setSubmitRemark(String submitRemark) {
        this.submitRemark = submitRemark;
    }
}
