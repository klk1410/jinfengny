package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.service.AppBizDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/biz")
public class AppBizController {

    private final AppBizDataService bizDataService;

    public AppBizController(AppBizDataService bizDataService) {
        this.bizDataService = bizDataService;
    }

    @GetMapping("/merchants")
    public ApiResponse<?> merchants(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listMerchants(openid));
    }

    @GetMapping("/agents")
    public ApiResponse<?> agents(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listAgents(openid));
    }

    @GetMapping("/salesmen")
    public ApiResponse<?> salesmen(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listSalesmen(openid));
    }

    @GetMapping("/devices")
    public ApiResponse<?> devices(@RequestParam String openid) {
        return ApiResponse.ok(bizDataService.listDevices(openid));
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
}
