package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.model.DeviceEventCreateRequest;
import com.envoil.app.model.MerchantAuditReviewRequest;
import com.envoil.app.model.MerchantCreateRequest;
import com.envoil.app.model.MerchantUpdateRequest;
import com.envoil.app.model.AccountShareCreateRequest;
import com.envoil.app.model.AccessoryCreateRequest;
import com.envoil.app.model.AccessoryTypeCreateRequest;
import com.envoil.app.model.AgentCreateRequest;
import com.envoil.app.model.SalesmanCreateRequest;
import com.envoil.app.model.OpenidBizScope;
import com.envoil.app.service.AppBizDataService;
import com.envoil.app.service.AppDeviceEventAuditService;
import com.envoil.app.service.AppDeviceEventService;
import com.envoil.app.service.AppMerchantAuditService;
import com.envoil.app.service.AppOpenidBizScopeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/biz")
public class AppBizController {

    private final AppBizDataService bizDataService;
    private final AppDeviceEventService deviceEventService;
    private final AppDeviceEventAuditService deviceEventAuditService;
    private final AppMerchantAuditService merchantAuditService;
    private final AppOpenidBizScopeService openidBizScopeService;

    public AppBizController(
            AppBizDataService bizDataService,
            AppDeviceEventService deviceEventService,
            AppDeviceEventAuditService deviceEventAuditService,
            AppMerchantAuditService merchantAuditService,
            AppOpenidBizScopeService openidBizScopeService) {
        this.bizDataService = bizDataService;
        this.deviceEventService = deviceEventService;
        this.deviceEventAuditService = deviceEventAuditService;
        this.merchantAuditService = merchantAuditService;
        this.openidBizScopeService = openidBizScopeService;
    }

    @GetMapping("/merchants")
    public ApiResponse<?> merchants(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listMerchants(openid));
    }

    @PostMapping("/merchants")
    public ApiResponse<?> createMerchant(@Validated @RequestBody MerchantCreateRequest req) {
        OpenidBizScope s = openidBizScopeService.resolve(req.getOpenid());
        if (s.getUserRole() == '3' || s.getUserRole() == '4') {
            return ApiResponse.ok(merchantAuditService.submitCreateAudit(req));
        }
        return ApiResponse.ok(bizDataService.createMerchant(req));
    }

    @GetMapping("/merchants/detail")
    public ApiResponse<?> merchantDetail(@RequestParam String openid, @RequestParam long merchantId) {
        return ApiResponse.ok(bizDataService.getMerchantDetail(openid, merchantId));
    }

    @PutMapping("/merchants")
    public ApiResponse<?> updateMerchant(@Validated @RequestBody MerchantUpdateRequest req) {
        bizDataService.updateMerchant(req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/merchant-audits")
    public ApiResponse<?> merchantAudits(@RequestParam String openid) {
        return ApiResponse.ok(merchantAuditService.listAudits(openid));
    }

    @GetMapping("/merchant-audits/detail")
    public ApiResponse<?> merchantAuditDetail(@RequestParam String openid, @RequestParam long auditId) {
        return ApiResponse.ok(merchantAuditService.getAuditDetail(openid, auditId));
    }

    @PostMapping("/merchant-audits")
    public ApiResponse<?> merchantAuditSubmit(@Validated @RequestBody MerchantUpdateRequest req) {
        return ApiResponse.ok(merchantAuditService.submitAudit(req));
    }

    @PostMapping("/merchant-audits/{auditId}/approve")
    public ApiResponse<?> merchantAuditApprove(
            @PathVariable long auditId, @Validated @RequestBody MerchantAuditReviewRequest req) {
        merchantAuditService.approve(auditId, req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/merchant-audits/{auditId}/reject")
    public ApiResponse<?> merchantAuditReject(
            @PathVariable long auditId, @Validated @RequestBody MerchantAuditReviewRequest req) {
        merchantAuditService.reject(auditId, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/device-event-audits")
    public ApiResponse<?> deviceEventAudits(@RequestParam String openid) {
        return ApiResponse.ok(deviceEventAuditService.listAudits(openid));
    }

    @GetMapping("/device-event-audits/detail")
    public ApiResponse<?> deviceEventAuditDetail(@RequestParam String openid, @RequestParam long auditId) {
        return ApiResponse.ok(deviceEventAuditService.getAuditDetail(openid, auditId));
    }

    @PostMapping("/device-event-audits/{auditId}/approve")
    public ApiResponse<?> deviceEventAuditApprove(
            @PathVariable long auditId, @Validated @RequestBody MerchantAuditReviewRequest req) {
        deviceEventAuditService.approve(auditId, req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/device-event-audits/{auditId}/reject")
    public ApiResponse<?> deviceEventAuditReject(
            @PathVariable long auditId, @Validated @RequestBody MerchantAuditReviewRequest req) {
        deviceEventAuditService.reject(auditId, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/agents")
    public ApiResponse<?> agents(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listAgents(openid));
    }

    @PostMapping("/agents")
    public ApiResponse<?> createAgent(@Validated @RequestBody AgentCreateRequest req) {
        return ApiResponse.ok(bizDataService.createAgent(req));
    }

    @GetMapping("/salesmen")
    public ApiResponse<?> salesmen(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listSalesmen(openid));
    }

    @GetMapping("/salesmen/portal-account")
    public ApiResponse<?> salesmanPortalAccount(@RequestParam String openid, @RequestParam long salesmanId) {
        return ApiResponse.ok(bizDataService.getSalesmanPortalAccount(openid, salesmanId));
    }

    @PostMapping("/salesmen")
    public ApiResponse<?> createSalesman(@Validated @RequestBody SalesmanCreateRequest req) {
        return ApiResponse.ok(bizDataService.createSalesman(req));
    }

    @GetMapping("/devices")
    public ApiResponse<?> devices(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listDevices(openid));
    }

    @GetMapping("/accessories")
    public ApiResponse<?> accessories(@RequestParam String openid, @RequestParam(required = false) Long typeId) {
        if (typeId == null) {
            return ApiResponse.ok(bizDataService.listAccessorySummaryByType(openid));
        }
        return ApiResponse.ok(bizDataService.listAccessoryLinesByType(openid, typeId));
    }

    @GetMapping("/accessory-types")
    public ApiResponse<?> accessoryTypes() {
        return ApiResponse.ok(bizDataService.listAccessoryTypes());
    }

    @PostMapping("/accessory-types")
    public ApiResponse<?> createAccessoryType(@Validated @RequestBody AccessoryTypeCreateRequest req) {
        return ApiResponse.ok(bizDataService.createAccessoryType(req));
    }

    @GetMapping("/accessory-operators")
    public ApiResponse<?> accessoryOperators(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listAccessoryInboundOperators(openid));
    }

    @PostMapping("/accessories")
    public ApiResponse<?> createAccessory(@Validated @RequestBody AccessoryCreateRequest req) {
        bizDataService.createAccessory(req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/device-events")
    public ApiResponse<?> deviceEvents(@RequestParam String openid) {
        return ApiResponse.ok(deviceEventService.listEvents(openid));
    }

    @PostMapping("/device-events")
    public ApiResponse<?> deviceEventsCreate(@Validated @RequestBody DeviceEventCreateRequest req) {
        return ApiResponse.ok(deviceEventService.createEvent(req));
    }

    @GetMapping("/stock/summary")
    public ApiResponse<?> stockSummary(@RequestParam String openid, @RequestParam(required = false) Long agentId) {
        return ApiResponse.ok(bizDataService.listStockSummary(openid, agentId));
    }

    @GetMapping("/stock/flows")
    public ApiResponse<?> stockFlows(@RequestParam String openid, @RequestParam(required = false) Long agentId) {
        return ApiResponse.ok(bizDataService.listStockFlows(openid, agentId));
    }

    @PostMapping("/stock/inbound")
    public ApiResponse<?> stockInbound(
            @RequestParam String openid,
            @RequestParam double qty,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String remark) {
        bizDataService.inboundStock(openid, BigDecimal.valueOf(qty), agentId, remark);
        return ApiResponse.ok(null);
    }

    @GetMapping("/account/ledger")
    public ApiResponse<?> accountLedger(@RequestParam String openid, @RequestParam(required = false) Long agentId) {
        return ApiResponse.ok(bizDataService.listAccountLedger(openid, agentId));
    }

    @GetMapping("/account/profile")
    public ApiResponse<?> accountProfile(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.accountProfile(openid));
    }

    @GetMapping("/account/shares")
    public ApiResponse<?> accountShares(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listAccountShares(openid));
    }

    @PostMapping("/account/shares")
    public ApiResponse<?> addAccountShare(@Validated @RequestBody AccountShareCreateRequest req) {
        bizDataService.addAccountShare(req.getOpenid(), req.getSharedOpenid());
        return ApiResponse.ok(null);
    }

    @PostMapping("/account/shares/remove")
    public ApiResponse<?> removeAccountShare(@RequestParam String openid, @RequestParam String sharedOpenid) {
        bizDataService.removeAccountShare(openid, sharedOpenid);
        return ApiResponse.ok(null);
    }
}
