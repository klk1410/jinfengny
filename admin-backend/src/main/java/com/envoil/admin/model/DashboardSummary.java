package com.envoil.admin.model;

public class DashboardSummary {

    private long agentCount;
    private long salesmanCount;
    private long merchantCount;
    private long orderPendingCount;
    private long workPendingCount;

    public long getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(long agentCount) {
        this.agentCount = agentCount;
    }

    public long getSalesmanCount() {
        return salesmanCount;
    }

    public void setSalesmanCount(long salesmanCount) {
        this.salesmanCount = salesmanCount;
    }

    public long getMerchantCount() {
        return merchantCount;
    }

    public void setMerchantCount(long merchantCount) {
        this.merchantCount = merchantCount;
    }

    public long getOrderPendingCount() {
        return orderPendingCount;
    }

    public void setOrderPendingCount(long orderPendingCount) {
        this.orderPendingCount = orderPendingCount;
    }

    public long getWorkPendingCount() {
        return workPendingCount;
    }

    public void setWorkPendingCount(long workPendingCount) {
        this.workPendingCount = workPendingCount;
    }
}
