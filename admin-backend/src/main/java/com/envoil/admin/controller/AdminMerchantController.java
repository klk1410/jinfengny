package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminBizJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    private final AdminBizJdbcService adminBizJdbcService;

    public AdminMerchantController(AdminBizJdbcService adminBizJdbcService) {
        this.adminBizJdbcService = adminBizJdbcService;
    }

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.ok(adminBizJdbcService.listMerchants());
    }
}
