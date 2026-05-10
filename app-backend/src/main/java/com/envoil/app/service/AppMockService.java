package com.envoil.app.service;

import com.envoil.app.model.OrderCreateRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AppMockService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final List<Map<String, Object>> orderStore = new CopyOnWriteArrayList<>();

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

    public Map<String, Object> portalModules(String openid) {
        Map<String, Object> loginResult = loginByOpenid(openid);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("openid", openid);
        data.put("role", loginResult.get("role"));
        data.put("hasBusinessAccess", loginResult.get("hasBusinessAccess"));
        data.put("modules", loginResult.get("modules"));
        data.put("gridEntries", Arrays.asList(
                "下单", "订单查询", "工单查询",
                "设备查询", "账目流水", "收益统计",
                "商家信息", "业务员信息", "通知中心"
        ));
        return data;
    }

    public Map<String, Object> createOrder(OrderCreateRequest request) {
        BigDecimal amount = BigDecimal.valueOf(request.getUnitPrice())
                .multiply(BigDecimal.valueOf(request.getBucketCount()))
                .setScale(2, RoundingMode.HALF_UP);

        String orderNo = "EO" + LocalDateTime.now().format(FORMATTER);
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderNo", orderNo);
        order.put("openid", request.getOpenid());
        order.put("merchantId", request.getMerchantId());
        order.put("orderType", request.getOrderType());
        order.put("unitPrice", request.getUnitPrice());
        order.put("bucketCount", request.getBucketCount());
        order.put("amountPayable", amount);
        order.put("status", "待确认");
        order.put("payType", request.getPayType());
        order.put("createTime", LocalDateTime.now().toString());
        orderStore.add(order);
        return order;
    }

    public List<Map<String, Object>> listOrders(String openid) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> order : orderStore) {
            if (openid.equals(order.get("openid"))) {
                result.add(order);
            }
        }
        return result;
    }

    public Map<String, Object> cancelOrder(String openid, String orderNo) {
        for (Map<String, Object> order : orderStore) {
            if (orderNo.equals(order.get("orderNo")) && openid.equals(order.get("openid"))) {
                order.put("status", "订单取消");
                return order;
            }
        }
        return null;
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
