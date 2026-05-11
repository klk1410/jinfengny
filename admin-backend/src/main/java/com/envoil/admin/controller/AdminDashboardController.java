package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminBizJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminBizJdbcService adminBizJdbcService;

    public AdminDashboardController(AdminBizJdbcService adminBizJdbcService) {
        this.adminBizJdbcService = adminBizJdbcService;
    }

    @GetMapping("/summary")
    public ApiResponse<?> summary() {
        return ApiResponse.ok(adminBizJdbcService.dashboard());
    }

    @GetMapping("/entry")
    public ApiResponse<?> entry() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "环保油管理后台");
        data.put("modules", new String[]{"商家管理", "订单管理", "工单管理", "设备管理", "仓储管理", "共享权限管理"});
        return ApiResponse.ok(data);
    }
}
