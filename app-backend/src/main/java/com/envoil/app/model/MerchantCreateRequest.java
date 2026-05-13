package com.envoil.app.model;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class MerchantCreateRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    /** 主端创建时必填 */
    private Long agentId;

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

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 单价（元/桶），未传则按 0 */
    private Double oilUnitPrice;

    /** 主营油品，默认后台为首条标准油 */
    private Long oilTypeId;

    private Double merchantCommission;

    /** 押金（元），必填，须 >= 0 */
    @NotNull(message = "请填写押金")
    @DecimalMin(value = "0.0", inclusive = true, message = "押金须大于等于0")
    private Double depositAmount;

    private Long salesmanId;

    /** 关联商家（同代理下已有门店，可选） */
    private Long linkedMerchantId;

    private String remark;
    private String storeImageUrl;

    /** 合同图片（Base64 data URL 或外链），新建店铺必填 */
    @NotBlank(message = "请上传合同图片")
    private String contractImageUrl;

    /** 地图定位说明（如浏览器定位精度、选点说明），新建店铺必填 */
    @NotBlank(message = "请完成地图定位")
    @Size(max = 500, message = "地图定位说明过长")
    private String mapLocationInfo;

    /** 提交审核时的说明（新建店铺走审核时使用，不落 biz_env_merchant.remark） */
    private String submitRemark;

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

    public Double getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(Double depositAmount) {
        this.depositAmount = depositAmount;
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

    public String getContractImageUrl() {
        return contractImageUrl;
    }

    public void setContractImageUrl(String contractImageUrl) {
        this.contractImageUrl = contractImageUrl;
    }

    public String getMapLocationInfo() {
        return mapLocationInfo;
    }

    public void setMapLocationInfo(String mapLocationInfo) {
        this.mapLocationInfo = mapLocationInfo;
    }

    public String getSubmitRemark() {
        return submitRemark;
    }

    public void setSubmitRemark(String submitRemark) {
        this.submitRemark = submitRemark;
    }
}
