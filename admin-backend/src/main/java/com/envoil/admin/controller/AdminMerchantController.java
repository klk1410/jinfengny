package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminMockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    private final AdminMockService adminMockService;

    public AdminMerchantController(AdminMockService adminMockService) {
        this.adminMockService = adminMockService;
    }

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.ok(adminMockService.merchants());
    }
}
