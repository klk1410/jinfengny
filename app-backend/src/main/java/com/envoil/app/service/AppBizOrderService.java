package com.envoil.app.service;

import com.envoil.app.model.OpenidBizScope;
import com.envoil.app.model.OrderCreateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppBizOrderService {

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ORDER_NO_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final AppOpenidBizScopeService scopeService;

    public AppBizOrderService(JdbcTemplate jdbcTemplate, AppOpenidBizScopeService scopeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.scopeService = scopeService;
    }

    public Map<String, Object> createOrder(OrderCreateRequest request) {
        OpenidBizScope scope = scopeService.resolve(request.getOpenid());
        Map<String, Object> merchant = loadMerchant(request.getMerchantId());
        if (merchant == null) {
            throw new IllegalArgumentException("商家不存在");
        }
        if (!canAccessMerchant(scope, merchant)) {
            throw new IllegalArgumentException("无权为该商家下单");
        }
        char orderType = parseOrderType(request.getOrderType());
        char payType = parsePayType(request.getPayType());
        BigDecimal unit = BigDecimal.valueOf(request.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal buckets = BigDecimal.valueOf(request.getBucketCount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountTotal = unit.multiply(buckets).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountPayable = amountTotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        long agentId = ((Number) merchant.get("agent_id")).longValue();
        String orderNo = "EO" + LocalDateTime.now().format(ORDER_NO_TS)
                + String.format("%02d", (int) (Math.random() * 100));

        jdbcTemplate.update(
                "INSERT INTO biz_env_order (order_no, order_time, merchant_id, order_type, oil_unit_price, "
                        + "oil_bucket_count, amount_total, discount_amount, amount_payable, status, agent_id, pay_type, del_flag) "
                        + "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, '0', ?, ?, '0')",
                orderNo,
                request.getMerchantId(),
                String.valueOf(orderType),
                unit,
                buckets,
                amountTotal,
                discount,
                amountPayable,
                agentId,
                String.valueOf(payType));

        return loadOrderMap(request.getOpenid(), orderNo);
    }

    public List<Map<String, Object>> listOrders(String openid) {
        OpenidBizScope scope = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT o.order_no, m.merchant_name, o.order_type, o.status, o.pay_type, ")
                .append("o.amount_payable, o.order_time ")
                .append("FROM biz_env_order o ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' ")
                .append("WHERE o.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendOrderScope(sql, args, scope);
        sql.append(" ORDER BY o.order_time DESC, o.order_id DESC");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNo", rs.getString("order_no"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("orderType", labelOrderType(rs.getString("order_type")));
            row.put("status", labelOrderStatus(rs.getString("status")));
            row.put("payType", labelPayType(rs.getString("pay_type")));
            row.put("amountPayable", rs.getBigDecimal("amount_payable").doubleValue());
            row.put("createTime", formatTs(rs.getTimestamp("order_time")));
            return row;
        });
    }

    public Map<String, Object> cancelOrder(String openid, String orderNo) {
        OpenidBizScope scope = scopeService.resolve(openid);
        Map<String, Object> row = jdbcTemplate.query(
                "SELECT o.order_no, o.status, o.agent_id, o.merchant_id, m.salesman_id, o.receive_salesman_id "
                        + "FROM biz_env_order o "
                        + "JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' "
                        + "WHERE o.order_no = ? AND o.del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("order_no", rs.getString("order_no"));
                    m.put("status", rs.getString("status"));
                    m.put("agent_id", rs.getLong("agent_id"));
                    m.put("merchant_id", rs.getLong("merchant_id"));
                    m.put("salesman_id", rs.getObject("salesman_id") == null ? null : rs.getLong("salesman_id"));
                    m.put("receive_salesman_id", rs.getObject("receive_salesman_id") == null ? null : rs.getLong("receive_salesman_id"));
                    return m;
                },
                orderNo);
        if (row == null) {
            return null;
        }
        if (!canSeeOrder(scope, row)) {
            return null;
        }
        String st = String.valueOf(row.get("status"));
        if ("3".equals(st) || "4".equals(st)) {
            throw new IllegalArgumentException("当前状态不可取消");
        }
        int n = jdbcTemplate.update(
                "UPDATE biz_env_order SET status = '4', cancel_time = CURRENT_TIMESTAMP WHERE order_no = ? AND del_flag = '0'",
                orderNo);
        if (n == 0) {
            return null;
        }
        return loadOrderMap(openid, orderNo);
    }

    private Map<String, Object> loadOrderMap(String openid, String orderNo) {
        List<Map<String, Object>> list = listOrders(openid);
        for (Map<String, Object> o : list) {
            if (orderNo.equals(o.get("orderNo"))) {
                return o;
            }
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> loadMerchant(long merchantId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT merchant_id, agent_id, salesman_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                merchantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean canAccessMerchant(OpenidBizScope scope, Map<String, Object> merchant) {
        char r = scope.getUserRole();
        long mid = ((Number) merchant.get("merchant_id")).longValue();
        long aid = ((Number) merchant.get("agent_id")).longValue();
        Long ms = merchant.get("salesman_id") == null ? null : ((Number) merchant.get("salesman_id")).longValue();
        if (r == '1') {
            return true;
        }
        if (r == '2') {
            return scope.getAgentId() != null && scope.getAgentId() == aid;
        }
        if (r == '3') {
            return scope.getAgentId() != null && scope.getAgentId() == aid
                    && scope.getSalesmanId() != null && scope.getSalesmanId().equals(ms);
        }
        if (r == '4') {
            return scope.getMerchantId() != null && scope.getMerchantId() == mid;
        }
        return false;
    }

    private void appendOrderScope(StringBuilder sql, List<Object> args, OpenidBizScope scope) {
        char r = scope.getUserRole();
        if (r == '1') {
            return;
        }
        if (r == '2' && scope.getAgentId() != null) {
            sql.append(" AND o.agent_id = ?");
            args.add(scope.getAgentId());
            return;
        }
        if (r == '4' && scope.getMerchantId() != null) {
            sql.append(" AND o.merchant_id = ?");
            args.add(scope.getMerchantId());
            return;
        }
        if (r == '3' && scope.getAgentId() != null && scope.getSalesmanId() != null) {
            sql.append(" AND o.agent_id = ? AND (m.salesman_id = ? OR o.receive_salesman_id = ?)");
            args.add(scope.getAgentId());
            args.add(scope.getSalesmanId());
            args.add(scope.getSalesmanId());
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private boolean canSeeOrder(OpenidBizScope scope, Map<String, Object> orderRow) {
        char r = scope.getUserRole();
        long aid = ((Number) orderRow.get("agent_id")).longValue();
        long mid = ((Number) orderRow.get("merchant_id")).longValue();
        Long ms = orderRow.get("salesman_id") == null ? null : ((Number) orderRow.get("salesman_id")).longValue();
        Long recv = orderRow.get("receive_salesman_id") == null ? null : ((Number) orderRow.get("receive_salesman_id")).longValue();
        if (r == '1') {
            return true;
        }
        if (r == '2') {
            return scope.getAgentId() != null && scope.getAgentId() == aid;
        }
        if (r == '4') {
            return scope.getMerchantId() != null && scope.getMerchantId() == mid;
        }
        if (r == '3') {
            return scope.getAgentId() != null && scope.getAgentId() == aid
                    && scope.getSalesmanId() != null
                    && (scope.getSalesmanId().equals(ms) || scope.getSalesmanId().equals(recv));
        }
        return false;
    }

    private static char parseOrderType(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("订单类型无效");
        }
        String s = raw.trim();
        if ("1".equals(s) || "加油".equals(s)) {
            return '1';
        }
        if ("2".equals(s) || "维护".equals(s)) {
            return '2';
        }
        throw new IllegalArgumentException("订单类型无效");
    }

    private static char parsePayType(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("支付方式无效");
        }
        String s = raw.trim();
        if ("1".equals(s) || "现结".equals(s)) {
            return '1';
        }
        if ("2".equals(s) || "赊欠".equals(s)) {
            return '2';
        }
        throw new IllegalArgumentException("支付方式无效");
    }

    private static String labelOrderType(String code) {
        if ("1".equals(code)) {
            return "加油";
        }
        if ("2".equals(code)) {
            return "维护";
        }
        return code == null ? "" : code;
    }

    private static String labelOrderStatus(String code) {
        if ("0".equals(code)) {
            return "待确认";
        }
        if ("1".equals(code)) {
            return "待分配";
        }
        if ("2".equals(code)) {
            return "已接收";
        }
        if ("3".equals(code)) {
            return "已完成";
        }
        if ("4".equals(code)) {
            return "订单取消";
        }
        return code == null ? "" : code;
    }

    private static String labelPayType(String code) {
        if ("1".equals(code)) {
            return "现结";
        }
        if ("2".equals(code)) {
            return "赊欠";
        }
        return code == null ? "" : code;
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        synchronized (TS_FMT) {
            return TS_FMT.format(ts);
        }
    }
}
