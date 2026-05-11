package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminBizJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/biz")
public class AdminStockLedgerController {

    private final AdminBizJdbcService adminBizJdbcService;

    public AdminStockLedgerController(AdminBizJdbcService adminBizJdbcService) {
        this.adminBizJdbcService = adminBizJdbcService;
    }

    @GetMapping("/stock/inventory")
    public ApiResponse<?> stockInventory() {
        return ApiResponse.ok(adminBizJdbcService.listStockInventory());
    }

    @GetMapping("/stock/flows")
    public ApiResponse<?> stockFlows() {
        return ApiResponse.ok(adminBizJdbcService.listStockFlows());
    }

    @GetMapping("/account/ledger")
    public ApiResponse<?> accountLedger() {
        return ApiResponse.ok(adminBizJdbcService.listAccountLedger());
    }
}
