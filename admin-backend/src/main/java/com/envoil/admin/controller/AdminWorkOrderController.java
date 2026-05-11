package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminBizJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/work-order")
public class AdminWorkOrderController {

    private final AdminBizJdbcService adminBizJdbcService;

    public AdminWorkOrderController(AdminBizJdbcService adminBizJdbcService) {
        this.adminBizJdbcService = adminBizJdbcService;
    }

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.ok(adminBizJdbcService.listWorkOrders());
    }
}
