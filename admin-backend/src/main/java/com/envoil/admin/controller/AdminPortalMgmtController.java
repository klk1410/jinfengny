package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.model.dto.PortalFuncRequest;
import com.envoil.admin.model.dto.PortalGroupRequest;
import com.envoil.admin.portal.PortalJdbcService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/portal")
public class AdminPortalMgmtController {

    private final PortalJdbcService portalJdbcService;

    public AdminPortalMgmtController(PortalJdbcService portalJdbcService) {
        this.portalJdbcService = portalJdbcService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<Map<String, Object>>> tree() {
        return ApiResponse.ok(portalJdbcService.loadPortalTree());
    }

    @PostMapping("/groups")
    public ApiResponse<Map<String, Object>> createGroup(@Validated @RequestBody PortalGroupRequest req) {
        Long id = portalJdbcService.createGroup(req.getTitle(), req.getSortOrder());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        return ApiResponse.ok(m);
    }

    @PutMapping("/groups/{id}")
    public ApiResponse<Void> updateGroup(@PathVariable("id") long id, @Validated @RequestBody PortalGroupRequest req) {
        portalJdbcService.updateGroup(id, req.getTitle(), req.getSortOrder());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/groups/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable("id") long id) {
        portalJdbcService.softDeleteGroup(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/functions")
    public ApiResponse<Map<String, Object>> createFunc(@Validated @RequestBody PortalFuncRequest req) {
        Long id = portalJdbcService.createFunction(
                req.getGroupId(),
                req.getPermCode(),
                req.getLabel(),
                req.getIcon(),
                req.getRoutePath(),
                req.getSortOrder());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        return ApiResponse.ok(m);
    }

    @PutMapping("/functions/{id}")
    public ApiResponse<Void> updateFunc(@PathVariable("id") long id, @Validated @RequestBody PortalFuncRequest req) {
        portalJdbcService.updateFunction(
                id,
                req.getGroupId(),
                req.getPermCode(),
                req.getLabel(),
                req.getIcon(),
                req.getRoutePath(),
                req.getSortOrder());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/functions/{id}")
    public ApiResponse<Void> deleteFunc(@PathVariable("id") long id) {
        portalJdbcService.softDeleteFunction(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/perm-options")
    public ApiResponse<List<String>> permOptions() {
        return ApiResponse.ok(portalJdbcService.allActivePermCodes());
    }
}
