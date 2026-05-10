package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.model.SharePermissionRequest;
import com.envoil.admin.service.AdminMockService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/permission")
public class AdminPermissionController {

    private final AdminMockService adminMockService;

    public AdminPermissionController(AdminMockService adminMockService) {
        this.adminMockService = adminMockService;
    }

    @GetMapping("/role-perms")
    public ApiResponse<?> rolePerms(@RequestParam(defaultValue = "env_agent") String roleKey) {
        return ApiResponse.ok(adminMockService.rolePerms(roleKey));
    }

    @GetMapping("/share-template")
    public ApiResponse<?> shareTemplate() {
        List<String> safePerms = adminMockService.shareSafePermTemplate();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateName", "agent_share_default");
        data.put("safePerms", safePerms);
        data.put("blockedPerms", new String[]{
                "env:order:export",
                "env:merchant:remove",
                "env:work:forceAssign",
                "env:permission:grant"
        });
        return ApiResponse.ok(data);
    }

    @PostMapping("/share-grant")
    public ApiResponse<?> shareGrant(@Validated @RequestBody SharePermissionRequest request) {
        List<String> safePerms = adminMockService.shareSafePermTemplate();
        for (String perm : request.getGrantedPerms()) {
            if (!safePerms.contains(perm)) {
                return ApiResponse.fail("非法授权权限：" + perm);
            }
        }
        return ApiResponse.ok(adminMockService.saveSharePerms(
                request.getOwnerOpenid(),
                request.getShareOpenid(),
                request.getGrantedPerms()
        ));
    }
}
