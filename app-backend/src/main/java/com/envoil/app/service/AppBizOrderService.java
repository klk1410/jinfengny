package com.envoil.app.service;

import com.envoil.app.model.OpenidBizScope;
import com.envoil.app.model.OrderCreateRequest;
import com.envoil.app.util.OilQuantityConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.ResultSetExtractor;

@Service
public class AppBizOrderService {

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ORDER_NO_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final AppOpenidBizScopeService scopeService;
    private final AppBizStockService stockService;
    private final AppOrderProcessLogService orderProcessLogService;

    public AppBizOrderService(
            JdbcTemplate jdbcTemplate,
            AppOpenidBizScopeService scopeService,
            @Lazy AppBizStockService stockService,
            AppOrderProcessLogService orderProcessLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.scopeService = scopeService;
        this.stockService = stockService;
        this.orderProcessLogService = orderProcessLogService;
    }

    public Map<String, Object> createOrder(OrderCreateRequest request) {
        OpenidBizScope scope = scopeService.resolve(request.getOpenid());
        long merchantId;
        if (scope.getUserRole() == '4') {
            if (scope.getMerchantId() == null) {
                throw new IllegalArgumentException("未绑定商家");
            }
            merchantId = scope.getMerchantId();
        } else {
            if (request.getMerchantId() == null) {
                throw new IllegalArgumentException("请选择商家");
            }
            merchantId = request.getMerchantId();
        }
        Map<String, Object> merchant = loadMerchant(merchantId);
        if (merchant == null) {
            throw new IllegalArgumentException("商家不存在");
        }
        if (!canAccessMerchant(scope, merchant)) {
            throw new IllegalArgumentException("无权为该商家下单");
        }
        char orderType = parseOrderType(request.getOrderType());
        char payType = parsePayType(request.getPayType());
        char qtyUnitChar = OilQuantityConverter.normalizeOilQtyUnit(request.getOilQtyUnit());
        BigDecimal buckets;
        BigDecimal unit;
        if (orderType == '1') {
            BigDecimal density = merchant.get("density_kg_per_liter") == null
                    ? new BigDecimal("0.8500")
                    : new BigDecimal(merchant.get("density_kg_per_liter").toString());
            BigDecimal litersPerBucket = merchant.get("liters_per_bucket") == null
                    ? new BigDecimal("200")
                    : new BigDecimal(merchant.get("liters_per_bucket").toString());
            BigDecimal rawQty = BigDecimal.valueOf(request.getBucketCount());
            buckets = OilQuantityConverter.toBuckets(rawQty, qtyUnitChar, density, litersPerBucket)
                    .setScale(4, RoundingMode.HALF_UP);
            Object ou = merchant.get("oil_unit_price");
            unit = ou == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : new BigDecimal(ou.toString()).setScale(2, RoundingMode.HALF_UP);
            if (request.getUnitPrice() != null && scope.getUserRole() != '4') {
                unit = BigDecimal.valueOf(request.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            buckets = BigDecimal.valueOf(request.getBucketCount()).setScale(2, RoundingMode.HALF_UP);
            unit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (buckets.compareTo(BigDecimal.ZERO) <= 0) {
                buckets = BigDecimal.ONE;
            }
        }
        String oilQtyUnitDb = orderType == '1' ? String.valueOf(Character.toUpperCase(qtyUnitChar)) : "B";
        Long toMerchantIdRow = null;
        if (orderType == '4') {
            if (request.getToMerchantId() == null) {
                throw new IllegalArgumentException("转移商家请选择目标门店");
            }
            if (request.getToMerchantId().longValue() == merchantId) {
                throw new IllegalArgumentException("源门店与目标门店不能相同");
            }
            Map<String, Object> toMer = loadMerchant(request.getToMerchantId());
            if (toMer == null) {
                throw new IllegalArgumentException("目标门店不存在");
            }
            if (!canAccessMerchant(scope, toMer)) {
                throw new IllegalArgumentException("无权选择该目标门店");
            }
            long aidSrc = ((Number) merchant.get("agent_id")).longValue();
            long aidTo = ((Number) toMer.get("agent_id")).longValue();
            if (aidSrc != aidTo) {
                throw new IllegalArgumentException("源门店与目标门店须属同一代理");
            }
            toMerchantIdRow = request.getToMerchantId();
        }
        BigDecimal amountTotal = unit.multiply(buckets).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountPayable = amountTotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        long agentId = ((Number) merchant.get("agent_id")).longValue();
        String orderNo = "EO" + LocalDateTime.now().format(ORDER_NO_TS)
                + String.format("%02d", (int) (Math.random() * 100));

        jdbcTemplate.update(
                "INSERT INTO biz_env_order (order_no, order_time, merchant_id, to_merchant_id, order_type, oil_unit_price, "
                        + "oil_bucket_count, oil_qty_unit, amount_total, discount_amount, amount_payable, status, agent_id, pay_type, del_flag) "
                        + "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0', ?, ?, '0')",
                orderNo,
                merchantId,
                toMerchantIdRow,
                String.valueOf(orderType),
                unit,
                buckets,
                oilQtyUnitDb,
                amountTotal,
                discount,
                amountPayable,
                agentId,
                String.valueOf(payType));

        Long orderId = jdbcTemplate.queryForObject(
                "SELECT order_id FROM biz_env_order WHERE order_no = ? AND del_flag = '0'",
                Long.class,
                orderNo);
        String merchantName = jdbcTemplate.queryForObject(
                "SELECT merchant_name FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                String.class,
                merchantId);
        char actorRole = scope.getUserRole();
        Long actorRef = null;
        if (actorRole == '4') {
            actorRef = merchantId;
        } else if (actorRole == '2') {
            actorRef = scope.getAgentId();
        } else if (actorRole == '3') {
            actorRef = scope.getSalesmanId();
        }
        orderProcessLogService.append(
                orderId,
                orderNo,
                "order_create",
                "商家【" + merchantName + "】提交订单",
                actorRole,
                actorRef);

        return loadOrderMap(request.getOpenid(), orderNo);
    }

    public List<Map<String, Object>> listOrders(String openid) {
        OpenidBizScope scope = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT o.order_no, m.merchant_name, tm.merchant_name AS to_merchant_name, o.order_type, o.status AS st, o.pay_type, ")
                .append("o.amount_payable, o.order_time, o.work_order_no, o.estimated_work_hours, o.oil_bucket_count, o.oil_qty_unit, o.to_merchant_id ")
                .append("FROM biz_env_order o ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' ")
                .append("LEFT JOIN biz_env_merchant tm ON tm.merchant_id = o.to_merchant_id AND tm.del_flag = '0' ")
                .append("WHERE o.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendOrderScope(sql, args, scope);
        sql.append(" ORDER BY o.order_time DESC, o.order_id DESC");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNo", rs.getString("order_no"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("orderType", labelOrderType(rs.getString("order_type")));
            row.put("statusCode", rs.getString("st"));
            row.put("status", labelOrderStatus(rs.getString("st")));
            row.put("payType", labelPayType(rs.getString("pay_type")));
            row.put("amountPayable", rs.getBigDecimal("amount_payable").doubleValue());
            row.put("bucketCount", rs.getBigDecimal("oil_bucket_count").doubleValue());
            row.put("oilQtyUnit", rs.getString("oil_qty_unit"));
            row.put("orderTypeCode", rs.getString("order_type"));
            row.put("createTime", formatTs(rs.getTimestamp("order_time")));
            row.put("workOrderNo", rs.getString("work_order_no"));
            BigDecimal estH = rs.getBigDecimal("estimated_work_hours");
            row.put("estimatedWorkHours", estH == null ? null : estH.doubleValue());
            row.put("toMerchantId", rs.getObject("to_merchant_id") == null ? null : rs.getLong("to_merchant_id"));
            row.put("toMerchantName", rs.getString("to_merchant_name"));
            return row;
        });
    }

    /** 订单统计（按当前角色的数据范围） */
    public Map<String, Object> orderStats(String openid) {
        OpenidBizScope scope = scopeService.resolve(openid);
        StringBuilder baseSql = new StringBuilder()
                .append("FROM biz_env_order o ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' ")
                .append("WHERE o.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendOrderScope(baseSql, args, scope);

        Map<String, Object> summary = jdbcTemplate.query(
                "SELECT COUNT(*) AS cnt, COALESCE(SUM(o.amount_payable),0) AS amt " + baseSql,
                rs -> {
                    if (!rs.next()) {
                        return new LinkedHashMap<>();
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderCount", rs.getLong("cnt"));
                    m.put("amountTotal", rs.getBigDecimal("amt").doubleValue());
                    return m;
                },
                args.toArray());
        if (summary == null || summary.isEmpty()) {
            summary = new LinkedHashMap<>();
            summary.put("orderCount", 0L);
            summary.put("amountTotal", 0D);
        }
        summary.put("roleCode", roleCode(scope.getUserRole()));
        summary.put("roleName", roleName(scope.getUserRole()));

        List<Map<String, Object>> byStatus = jdbcTemplate.query(
                "SELECT o.status AS st, COUNT(*) AS cnt, COALESCE(SUM(o.amount_payable),0) AS amt "
                        + baseSql + " GROUP BY o.status ORDER BY o.status",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("statusCode", rs.getString("st"));
                    m.put("status", labelOrderStatus(rs.getString("st")));
                    m.put("orderCount", rs.getLong("cnt"));
                    m.put("amountTotal", rs.getBigDecimal("amt").doubleValue());
                    return m;
                },
                args.toArray());
        summary.put("byStatus", byStatus);

        char role = scope.getUserRole();
        if (role == '2' || role == '3') {
            List<Map<String, Object>> byMerchant = jdbcTemplate.query(
                    "SELECT m.merchant_id AS mid, m.merchant_name AS mname, COUNT(*) AS cnt, COALESCE(SUM(o.amount_payable),0) AS amt "
                            + baseSql + " GROUP BY m.merchant_id, m.merchant_name ORDER BY cnt DESC, m.merchant_id",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("merchantId", rs.getLong("mid"));
                        m.put("merchantName", rs.getString("mname"));
                        m.put("orderCount", rs.getLong("cnt"));
                        m.put("amountTotal", rs.getBigDecimal("amt").doubleValue());
                        return m;
                    },
                    args.toArray());
            summary.put("byMerchant", byMerchant);
        } else if (role == '1') {
            List<Map<String, Object>> byAgent = jdbcTemplate.query(
                    "SELECT o.agent_id AS aid, COUNT(*) AS cnt, COALESCE(SUM(o.amount_payable),0) AS amt "
                            + baseSql + " GROUP BY o.agent_id ORDER BY cnt DESC, o.agent_id",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("agentId", rs.getLong("aid"));
                        m.put("orderCount", rs.getLong("cnt"));
                        m.put("amountTotal", rs.getBigDecimal("amt").doubleValue());
                        return m;
                    },
                    args.toArray());
            summary.put("byAgent", byAgent);
        }
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmOrder(String openid, String orderNo, Double estimatedWorkHours) {
        OpenidBizScope scope = scopeService.resolve(openid);
        if (scope.getUserRole() != '1' && scope.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可确认订单");
        }
        BigDecimal hoursBd = null;
        if (estimatedWorkHours != null && !estimatedWorkHours.isNaN() && !estimatedWorkHours.isInfinite()) {
            hoursBd = BigDecimal.valueOf(estimatedWorkHours).setScale(2, RoundingMode.HALF_UP);
        }
        if (scope.getUserRole() == '2') {
            if (hoursBd == null || hoursBd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("请填写预计工作时间（小时）");
            }
            if (hoursBd.compareTo(BigDecimal.valueOf(168)) > 0) {
                throw new IllegalArgumentException("预计工作时间不能超过168小时");
            }
        }
        Map<String, Object> row = jdbcTemplate.query(
                "SELECT o.order_id, o.order_no, o.status, o.agent_id, o.merchant_id, o.order_type, o.oil_bucket_count "
                        + "FROM biz_env_order o WHERE o.order_no = ? AND o.del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("order_id", rs.getLong("order_id"));
                    m.put("order_no", rs.getString("order_no"));
                    m.put("status", rs.getString("status"));
                    m.put("agent_id", rs.getLong("agent_id"));
                    m.put("merchant_id", rs.getLong("merchant_id"));
                    m.put("order_type", rs.getString("order_type"));
                    m.put("oil_bucket_count", rs.getBigDecimal("oil_bucket_count"));
                    return m;
                },
                orderNo);
        if (row == null) {
            return null;
        }
        if (!canConfirmOrder(scope, row)) {
            return null;
        }
        if (!"0".equals(String.valueOf(row.get("status")))) {
            throw new IllegalArgumentException("仅待确认订单可确认");
        }
        long merchantId = ((Number) row.get("merchant_id")).longValue();
        long agentId = ((Number) row.get("agent_id")).longValue();
        long orderId = ((Number) row.get("order_id")).longValue();
        String orderType = String.valueOf(row.get("order_type"));
        BigDecimal buckets = row.get("oil_bucket_count") == null
                ? BigDecimal.ZERO
                : ((BigDecimal) row.get("oil_bucket_count")).setScale(2, RoundingMode.HALF_UP);
        if ("1".equals(orderType)) {
            long oilTypeId = resolveMerchantOilTypeId(merchantId);
            stockService.reserveForOilOrder(agentId, oilTypeId, orderNo, buckets);
        }
        String woNo = AppBizWorkOrderService.newWorkOrderNo();
        Timestamp acceptDeadline = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));
        jdbcTemplate.update(
                "INSERT INTO biz_env_work_order (work_order_no, order_id, order_no, merchant_id, work_order_type, status, agent_id, accept_deadline, assign_type, del_flag) "
                        + "VALUES (?, ?, ?, ?, ?, '1', ?, ?, '1', '0')",
                woNo,
                orderId,
                orderNo,
                merchantId,
                orderType,
                agentId,
                acceptDeadline);
        int n = jdbcTemplate.update(
                "UPDATE biz_env_order SET status = '1', work_order_no = ?, estimated_work_hours = ? "
                        + "WHERE order_no = ? AND del_flag = '0' AND status = '0'",
                woNo,
                hoursBd,
                orderNo);
        if (n == 0) {
            throw new IllegalArgumentException("确认失败，订单状态已变更");
        }
        String confirmTitle;
        Long actorRef = null;
        if (scope.getUserRole() == '1') {
            confirmTitle = "主端确认订单，生成待分配工单";
            actorRef = null;
        } else {
            String agentName = jdbcTemplate.queryForObject(
                    "SELECT agent_name FROM biz_env_agent WHERE agent_id = ? AND del_flag = '0'",
                    String.class,
                    scope.getAgentId());
            confirmTitle = "代理【"
                    + agentName
                    + "】确认订单（预计 "
                    + formatWorkHoursLabel(hoursBd)
                    + " 小时），生成待分配工单";
            actorRef = scope.getAgentId();
        }
        orderProcessLogService.append(
                orderId,
                orderNo,
                "order_confirm",
                confirmTitle,
                scope.getUserRole(),
                actorRef);

        return loadOrderMap(openid, orderNo);
    }

    private boolean canConfirmOrder(OpenidBizScope scope, Map<String, Object> orderRow) {
        char r = scope.getUserRole();
        long aid = ((Number) orderRow.get("agent_id")).longValue();
        if (r == '1') {
            return true;
        }
        if (r == '2') {
            return scope.getAgentId() != null && scope.getAgentId() == aid;
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelOrder(String openid, String orderNo) {
        OpenidBizScope scope = scopeService.resolve(openid);
        Map<String, Object> row = jdbcTemplate.query(
                "SELECT o.order_id, o.order_no, o.status, o.agent_id, o.merchant_id, m.salesman_id, o.receive_salesman_id, "
                        + "o.order_type, o.oil_bucket_count "
                        + "FROM biz_env_order o "
                        + "JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' "
                        + "WHERE o.order_no = ? AND o.del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("order_id", rs.getLong("order_id"));
                    m.put("order_no", rs.getString("order_no"));
                    m.put("status", rs.getString("status"));
                    m.put("agent_id", rs.getLong("agent_id"));
                    m.put("merchant_id", rs.getLong("merchant_id"));
                    m.put("salesman_id", rs.getObject("salesman_id") == null ? null : rs.getLong("salesman_id"));
                    m.put("receive_salesman_id", rs.getObject("receive_salesman_id") == null ? null : rs.getLong("receive_salesman_id"));
                    m.put("order_type", rs.getString("order_type"));
                    m.put("oil_bucket_count", rs.getBigDecimal("oil_bucket_count"));
                    return m;
                },
                orderNo);
        if (row == null) {
            return null;
        }
        if (!canSeeOrder(scope, row)) {
            return null;
        }
        char ur = scope.getUserRole();
        if (ur == '3') {
            throw new IllegalArgumentException("业务员不可取消订单");
        }
        String st = String.valueOf(row.get("status"));
        if ("3".equals(st) || "4".equals(st)) {
            throw new IllegalArgumentException("当前状态不可取消");
        }
        if (ur == '4' && !"0".equals(st) && !"1".equals(st)) {
            throw new IllegalArgumentException("商家仅可在待确认或待分配阶段取消订单");
        }
        String orderType = String.valueOf(row.get("order_type"));
        BigDecimal buckets = row.get("oil_bucket_count") == null
                ? BigDecimal.ZERO
                : ((BigDecimal) row.get("oil_bucket_count")).setScale(2, RoundingMode.HALF_UP);
        long aid = ((Number) row.get("agent_id")).longValue();
        if ("1".equals(orderType) && ("1".equals(st) || "2".equals(st))) {
            long merchantIdForOil = ((Number) row.get("merchant_id")).longValue();
            stockService.rollbackReserveForOilOrder(aid, resolveMerchantOilTypeId(merchantIdForOil), orderNo, buckets);
        }
        jdbcTemplate.update(
                "UPDATE biz_env_work_order SET status = '4' WHERE order_no = ? AND del_flag = '0' AND status IN ('0','1','2')",
                orderNo);
        int n = jdbcTemplate.update(
                "UPDATE biz_env_order SET status = '4', cancel_time = CURRENT_TIMESTAMP WHERE order_no = ? AND del_flag = '0'",
                orderNo);
        if (n == 0) {
            return null;
        }
        long orderId = ((Number) row.get("order_id")).longValue();
        String cancelTitle;
        Long actorRef = null;
        if (ur == '4') {
            cancelTitle = "商家发起取消，订单已关闭";
            actorRef = scope.getMerchantId();
        } else if (ur == '1') {
            cancelTitle = "主端取消订单";
        } else if (ur == '2') {
            cancelTitle = "代理取消订单";
            actorRef = scope.getAgentId();
        } else {
            cancelTitle = "订单已取消";
        }
        orderProcessLogService.append(orderId, orderNo, "order_cancel", cancelTitle, ur, actorRef);

        return loadOrderMap(openid, orderNo);
    }

    /**
     * 订单流程时间轴（主端/代理等可见性与订单列表一致）。优先返回持久化日志；无日志时对历史单做推断。
     */
    public Map<String, Object> getOrderTimeline(String openid, String orderNo) {
        Map<String, Object> orderRow = jdbcTemplate.query(
                "SELECT o.order_id, o.order_no, o.agent_id, o.merchant_id, m.salesman_id, o.receive_salesman_id "
                        + "FROM biz_env_order o "
                        + "JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' "
                        + "WHERE o.order_no = ? AND o.del_flag = '0'",
                new ResultSetExtractor<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> extractData(java.sql.ResultSet rs) throws java.sql.SQLException {
                        if (!rs.next()) {
                            return null;
                        }
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("order_id", rs.getLong("order_id"));
                        m.put("order_no", rs.getString("order_no"));
                        m.put("agent_id", rs.getLong("agent_id"));
                        m.put("merchant_id", rs.getLong("merchant_id"));
                        m.put("salesman_id", rs.getObject("salesman_id") == null ? null : rs.getLong("salesman_id"));
                        m.put("receive_salesman_id", rs.getObject("receive_salesman_id") == null ? null : rs.getLong("receive_salesman_id"));
                        return m;
                    }
                },
                orderNo);
        if (orderRow == null) {
            return null;
        }
        OpenidBizScope scope = scopeService.resolve(openid);
        if (!canSeeOrder(scope, orderRow)) {
            return null;
        }

        List<Map<String, Object>> logged = orderProcessLogService.listByOrderNo(orderNo);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderNo", orderNo);
        if (!logged.isEmpty()) {
            out.put("source", "log");
            out.put(
                    "steps",
                    logged.stream()
                            .map(r -> {
                                Map<String, Object> step = new LinkedHashMap<>();
                                step.put("eventCode", r.get("eventCode"));
                                step.put("title", r.get("title"));
                                step.put("operationTime", r.get("operationTime"));
                                return step;
                            })
                            .collect(Collectors.toList()));
            return out;
        }

        List<Map<String, Object>> inferred = buildInferredTimeline(orderNo);
        out.put("source", "inferred");
        out.put("steps", inferred);
        return out;
    }

    private List<Map<String, Object>> buildInferredTimeline(String orderNo) {
        List<Map<String, Object>> steps = new ArrayList<>();
        jdbcTemplate.query(
                "SELECT o.order_time, o.status AS ost, o.finish_time, o.cancel_time, m.merchant_name, "
                        + "w.work_order_time, w.work_start_time, w.assign_type, w.receive_salesman_id, "
                        + "COALESCE(sm.salesman_name, '') AS recv_name "
                        + "FROM biz_env_order o "
                        + "JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' "
                        + "LEFT JOIN biz_env_work_order w ON w.order_no = o.order_no AND w.del_flag = '0' "
                        + "LEFT JOIN biz_env_salesman sm ON sm.salesman_id = w.receive_salesman_id AND sm.del_flag = '0' "
                        + "WHERE o.order_no = ? AND o.del_flag = '0'",
                new ResultSetExtractor<Void>() {
                    @Override
                    public Void extractData(java.sql.ResultSet rs) throws java.sql.SQLException {
                        if (!rs.next()) {
                            return null;
                        }
                        addTimelineStep(
                                steps,
                                "order_create",
                                "商家【" + rs.getString("merchant_name") + "】提交订单（历史推断）",
                                rs.getTimestamp("order_time"));
                        String ost = rs.getString("ost");
                        Timestamp wot = rs.getTimestamp("work_order_time");
                        if (wot != null && ost != null && !"0".equals(ost)) {
                            addTimelineStep(
                                    steps,
                                    "order_confirm",
                                    "订单已确认并生成工单（历史推断）",
                                    wot);
                        }
                        Long recvId = rs.getObject("receive_salesman_id") == null ? null : rs.getLong("receive_salesman_id");
                        Timestamp wst = rs.getTimestamp("work_start_time");
                        String recvName = rs.getString("recv_name");
                        String assignType = rs.getString("assign_type");
                        if (recvId != null && wst != null && recvName != null && !recvName.isEmpty()) {
                            String t = "2".equals(assignType)
                                    ? "业务员【" + recvName + "】经指派接单（历史推断）"
                                    : "业务员【" + recvName + "】抢单成功（历史推断）";
                            addTimelineStep(steps, "work_receive", t, wst);
                        }
                        Timestamp ft = rs.getTimestamp("finish_time");
                        if ("3".equals(ost) && ft != null) {
                            addTimelineStep(steps, "work_finish", "订单已完成（历史推断）", ft);
                        }
                        Timestamp ct = rs.getTimestamp("cancel_time");
                        if ("4".equals(ost) && ct != null) {
                            addTimelineStep(steps, "order_cancel", "订单已取消（历史推断）", ct);
                        }
                        return null;
                    }
                },
                orderNo);
        steps.sort(Comparator.comparing(m -> String.valueOf(m.get("operationTime"))));
        return steps;
    }

    private static void addTimelineStep(List<Map<String, Object>> steps, String code, String title, Timestamp ts) {
        if (ts == null) {
            return;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventCode", code);
        m.put("title", title);
        m.put("operationTime", formatTs(ts));
        steps.add(m);
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
                "SELECT m.merchant_id, m.agent_id, m.salesman_id, m.oil_unit_price, COALESCE(m.oil_type_id, 1) AS oil_type_id, "
                        + "ot.density_kg_per_liter, ot.liters_per_bucket "
                        + "FROM biz_env_merchant m "
                        + "LEFT JOIN biz_env_oil_type ot ON ot.oil_type_id = COALESCE(m.oil_type_id, 1) AND ot.del_flag = '0' "
                        + "WHERE m.merchant_id = ? AND m.del_flag = '0'",
                merchantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private long resolveMerchantOilTypeId(long merchantId) {
        Number n = jdbcTemplate.query(
                "SELECT COALESCE(oil_type_id, 1) AS oid FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return (Number) rs.getObject("oid");
                },
                merchantId);
        return n == null ? 1L : n.longValue();
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
        if ("4".equals(s) || "转移商家".equals(s)) {
            return '4';
        }
        throw new IllegalArgumentException("订单类型无效");
    }

    private static char parsePayType(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("支付方式无效");
        }
        String s = raw.trim();
        if ("1".equals(s) || "现结".equals(s) || "微信支付".equals(s)) {
            return '1';
        }
        if ("2".equals(s) || "赊欠".equals(s) || "赊销".equals(s)) {
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
        if ("4".equals(code)) {
            return "转移商家";
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
            return "微信支付";
        }
        if ("2".equals(code)) {
            return "赊销";
        }
        return code == null ? "" : code;
    }

    private static String roleName(char code) {
        if (code == '1') return "主端";
        if (code == '2') return "代理";
        if (code == '3') return "业务员";
        if (code == '4') return "商家";
        return String.valueOf(code);
    }

    private static String roleCode(char code) {
        if (code == '1') return "main";
        if (code == '2') return "agent";
        if (code == '3') return "sales";
        if (code == '4') return "merchant";
        return String.valueOf(code);
    }

    private static String formatWorkHoursLabel(BigDecimal hours) {
        if (hours == null) {
            return "—";
        }
        return hours.stripTrailingZeros().toPlainString();
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
