package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.model.dto.MiniRolePermBatchRequest;
import com.envoil.admin.model.dto.MiniSubjectRequest;
import com.envoil.admin.portal.PortalJdbcService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/mini")
public class AdminMiniAccessController {

    private final PortalJdbcService portalJdbcService;

    public AdminMiniAccessController(PortalJdbcService portalJdbcService) {
        this.portalJdbcService = portalJdbcService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.ok(portalJdbcService.listRolesWithPerms());
    }

    @PutMapping("/roles/{roleId}/perms")
    public ApiResponse<Void> updateRolePerms(
            @PathVariable("roleId") long roleId,
            @RequestBody(required = false) MiniRolePermBatchRequest body) {
        java.util.List<String> codes = body == null ? java.util.Collections.emptyList() : body.getPermCodes();
        portalJdbcService.replaceRolePerms(roleId, codes);
        return ApiResponse.ok(null);
    }

    @GetMapping("/subjects")
    public ApiResponse<List<Map<String, Object>>> subjects() {
        return ApiResponse.ok(portalJdbcService.listSubjects());
    }

    @PutMapping("/subjects")
    public ApiResponse<Void> upsertSubject(@Validated @RequestBody MiniSubjectRequest body) {
        portalJdbcService.upsertSubject(body.getOpenid(), body.getRoleId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/subjects/{openid}")
    public ApiResponse<Void> deleteSubject(@PathVariable("openid") String openid) {
        portalJdbcService.deleteSubject(openid);
        return ApiResponse.ok(null);
    }
}
