package com.envoil.admin.model;

public class WorkOrderView {

    private String workOrderNo;
    private String orderNo;
    private String merchantName;
    private String workOrderType;
    private String status;
    private String receiveSalesmanName;
    private String workOrderTime;

    public String getWorkOrderNo() {
        return workOrderNo;
    }

    public void setWorkOrderNo(String workOrderNo) {
        this.workOrderNo = workOrderNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getWorkOrderType() {
        return workOrderType;
    }

    public void setWorkOrderType(String workOrderType) {
        this.workOrderType = workOrderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReceiveSalesmanName() {
        return receiveSalesmanName;
    }

    public void setReceiveSalesmanName(String receiveSalesmanName) {
        this.receiveSalesmanName = receiveSalesmanName;
    }

    public String getWorkOrderTime() {
        return workOrderTime;
    }

    public void setWorkOrderTime(String workOrderTime) {
        this.workOrderTime = workOrderTime;
    }
}
