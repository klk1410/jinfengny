package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.service.AppMockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal")
public class PortalController {

    private final AppMockService appMockService;

    public PortalController(AppMockService appMockService) {
        this.appMockService = appMockService;
    }

    @GetMapping("/modules")
    public ApiResponse<?> modules(@RequestParam String openid) {
        return ApiResponse.ok(appMockService.portalModules(openid));
    }
}
