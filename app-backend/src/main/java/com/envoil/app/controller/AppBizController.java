package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.service.AppBizDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
