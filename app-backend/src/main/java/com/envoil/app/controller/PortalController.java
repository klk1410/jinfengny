package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.service.AppBizDataService;
import com.envoil.app.service.AppPortalJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal")
public class PortalController {

    private final AppPortalJdbcService appPortalJdbcService;
    private final AppBizDataService appBizDataService;

    public PortalController(AppPortalJdbcService appPortalJdbcService, AppBizDataService appBizDataService) {
        this.appPortalJdbcService = appPortalJdbcService;
        this.appBizDataService = appBizDataService;
    }

    @GetMapping("/modules")
    public ApiResponse<?> modules(@RequestParam String openid) {
        return ApiResponse.ok(appPortalJdbcService.portalModules(openid));
    }

    @GetMapping("/subjects")
    public ApiResponse<?> subjects() {
        return ApiResponse.ok(appPortalJdbcService.listSubjects());
    }

    /** 客户端首页右上角待办红泡：订单、工单、店铺审核、设备审核（按 openid 数据范围聚合）。 */
    @GetMapping("/pending-counts")
    public ApiResponse<?> pendingCounts(@RequestParam String openid) {
        return ApiResponse.ok(appBizDataService.pendingTodoCounts(openid));
    }
}
