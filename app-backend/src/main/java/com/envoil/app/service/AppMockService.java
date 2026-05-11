package com.envoil.app.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppMockService {

    public Map<String, Object> loginByOpenid(String openid) {
        if ("main-openid-001".equals(openid)) {
            return userResult(openid, "主端", true, Arrays.asList("账号信息", "环保油管理", "账号操作"));
        }
        if ("agent-openid-001".equals(openid)) {
            return userResult(openid, "代理", true, Arrays.asList("账号信息", "环保油管理", "账号操作"));
        }
        if ("sales-openid-001".equals(openid)) {
            return userResult(openid, "业务员", true, Arrays.asList("账号信息", "环保油管理", "账号操作"));
        }
        if ("merchant-openid-001".equals(openid)) {
            return userResult(openid, "商家", true, Arrays.asList("账号信息", "环保油管理", "账号操作"));
        }
        return userResult(openid, "未授权", false, Arrays.asList("账号信息", "账号操作"));
    }

    private Map<String, Object> userResult(String openid, String role, boolean hasBusinessAccess, List<String> modules) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("openid", openid);
        data.put("role", role);
        data.put("hasBusinessAccess", hasBusinessAccess);
        data.put("modules", modules);
        return data;
    }
}
