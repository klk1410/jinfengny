package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminMockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    private final AdminMockService adminMockService;

    public AdminOrderController(AdminMockService adminMockService) {
        this.adminMockService = adminMockService;
    }

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.ok(adminMockService.orders());
    }
}
