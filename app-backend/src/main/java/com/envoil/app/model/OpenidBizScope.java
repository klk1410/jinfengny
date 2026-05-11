package com.envoil.app.model;

/**
 * openid 业务数据范围（来自 env_openid_biz_scope 或测试账号回退）。
 * userRole: 1 主端 2 代理 3 业务员 4 商家
 */
public class OpenidBizScope {

    private final char userRole;
    private final Long agentId;
    private final Long merchantId;
    private final Long salesmanId;

    public OpenidBizScope(char userRole, Long agentId, Long merchantId, Long salesmanId) {
        this.userRole = userRole;
        this.agentId = agentId;
        this.merchantId = merchantId;
        this.salesmanId = salesmanId;
    }

    public char getUserRole() {
        return userRole;
    }

    public Long getAgentId() {
        return agentId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public Long getSalesmanId() {
        return salesmanId;
    }
}
