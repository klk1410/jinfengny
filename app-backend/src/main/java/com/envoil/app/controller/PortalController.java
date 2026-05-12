package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.service.AppPortalJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal")
public class PortalController {

    private final AppPortalJdbcService appPortalJdbcService;

    public PortalController(AppPortalJdbcService appPortalJdbcService) {
        this.appPortalJdbcService = appPortalJdbcService;
    }

    @GetMapping("/modules")
    public ApiResponse<?> modules(@RequestParam String openid) {
        return ApiResponse.ok(appPortalJdbcService.portalModules(openid));
    }

    @GetMapping("/subjects")
    public ApiResponse<?> subjects() {
        return ApiResponse.ok(appPortalJdbcService.listSubjects());
    }
}
