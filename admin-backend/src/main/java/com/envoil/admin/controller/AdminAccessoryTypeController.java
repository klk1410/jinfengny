package com.envoil.admin.controller;

import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.service.AdminBizJdbcService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/biz/accessory-types")
public class AdminAccessoryTypeController {

    private final AdminBizJdbcService adminBizJdbcService;

    public AdminAccessoryTypeController(AdminBizJdbcService adminBizJdbcService) {
        this.adminBizJdbcService = adminBizJdbcService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(adminBizJdbcService.listAccessoryTypes());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String name = body.get("typeName") == null ? "" : String.valueOf(body.get("typeName")).trim();
        int sort = 0;
        if (body.get("sortOrder") != null) {
            sort = ((Number) body.get("sortOrder")).intValue();
        }
        long id = adminBizJdbcService.createAccessoryType(name, sort);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("typeId", id);
        return ApiResponse.ok(m);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable("id") long id, @RequestBody Map<String, Object> body) {
        String name = body.get("typeName") == null ? "" : String.valueOf(body.get("typeName")).trim();
        int sort = 0;
        if (body.get("sortOrder") != null) {
            sort = ((Number) body.get("sortOrder")).intValue();
        }
        adminBizJdbcService.updateAccessoryType(id, name, sort);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") long id) {
        adminBizJdbcService.softDeleteAccessoryType(id);
        return ApiResponse.ok(null);
    }
}
