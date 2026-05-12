package com.envoil.app.service;

import com.envoil.app.model.OpenidBizScope;
import com.envoil.app.model.AccessoryConsumeLine;
import com.envoil.app.model.WorkOrderFinishRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppBizWorkOrderService {

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter WO_NO_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final AppOpenidBizScopeService scopeService;
    private final AppBizStockService stockService;
    private final AppBizAccountService accountService;
    private final AppBizDataService bizDataService;
    private final AppDeviceEventService deviceEventService;

    public AppBizWorkOrderService(
            JdbcTemplate jdbcTemplate,
            AppOpenidBizScopeService scopeService,
            @Lazy AppBizStockService stockService,
            AppBizAccountService accountService,
            @Lazy AppBizDataService bizDataService,
            @Lazy AppDeviceEventService deviceEventService) {
        this.jdbcTemplate = jdbcTemplate;
        this.scopeService = scopeService;
        this.stockService = stockService;
        this.accountService = accountService;
        this.bizDataService = bizDataService;
        this.deviceEventService = deviceEventService;
    }

    public List<Map<String, Object>> listWorkOrders(String openid) {
        OpenidBizScope scope = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT w.work_order_no, w.order_no, w.work_order_time, w.work_order_type, w.status, ")
                .append("w.agent_id, w.merchant_id, w.receive_salesman_id, w.accept_deadline, m.merchant_name, ")
                .append("rs.salesman_name AS receive_salesman_name, ord.to_merchant_id, tm.merchant_name AS to_merchant_name ")
                .append("FROM biz_env_work_order w ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = w.merchant_id AND m.del_flag = '0' ")
                .append("LEFT JOIN biz_env_order ord ON ord.order_no = w.order_no AND ord.del_flag = '0' ")
                .append("LEFT JOIN biz_env_merchant tm ON tm.merchant_id = ord.to_merchant_id AND tm.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman rs ON rs.salesman_id = w.receive_salesman_id AND rs.del_flag = '0' ")
                .append("WHERE w.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendWorkOrderScope(sql, args, scope);
        sql.append(" ORDER BY w.work_order_time DESC, w.work_order_id DESC");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workOrderNo", rs.getString("work_order_no"));
            row.put("orderNo", rs.getString("order_no"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("workOrderTypeCode", rs.getString("work_order_type"));
            row.put("workOrderType", labelWorkType(rs.getString("work_order_type")));
            row.put("status", labelWorkStatus(rs.getString("status")));
            row.put("statusCode", rs.getString("status"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("receiveSalesmanId", rs.getObject("receive_salesman_id") == null ? null : rs.getLong("receive_salesman_id"));
            row.put("receiveSalesmanName", rs.getString("receive_salesman_name"));
            row.put("workOrderTime", formatTs(rs.getTimestamp("work_order_time")));
            Timestamp adl = rs.getTimestamp("accept_deadline");
            row.put("acceptDeadline", formatTs(adl));
            long now = System.currentTimeMillis();
            boolean expired = adl != null && now > adl.getTime();
            row.put("grabExpired", expired);
            row.put("canForceAssign", expired);
            row.put("toMerchantId", rs.getObject("to_merchant_id") == null ? null : rs.getLong("to_merchant_id"));
            row.put("toMerchantName", rs.getString("to_merchant_name"));
            return row;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveWorkOrder(String openid, String workOrderNo) {
        OpenidBizScope scope = scopeService.resolve(openid);
        if (scope.getUserRole() != '3' || scope.getSalesmanId() == null || scope.getAgentId() == null) {
            throw new IllegalArgumentException("仅业务员可抢单");
        }
        Map<String, Object> wo = loadWorkOrder(workOrderNo);
        if (wo == null) {
            return null;
        }
        if (!canOperateWorkOrder(scope, wo)) {
            return null;
        }
        Timestamp adl = (Timestamp) wo.get("accept_deadline");
        if (adl != null && System.currentTimeMillis() > adl.getTime()) {
            throw new IllegalArgumentException("抢单已截止，请等待代理指派");
        }
        String st = String.valueOf(wo.get("status"));
        if (!"1".equals(st) && !"0".equals(st)) {
            throw new IllegalArgumentException("当前工单不可抢单");
        }
        if (wo.get("receive_salesman_id") != null) {
            throw new IllegalArgumentException("工单已被接单");
        }
        long agentId = ((Number) wo.get("agent_id")).longValue();
        if (agentId != scope.getAgentId()) {
            return null;
        }
        int n = jdbcTemplate.update(
                "UPDATE biz_env_work_order SET receive_salesman_id = ?, status = '2', work_start_time = CURRENT_TIMESTAMP "
                        + "WHERE work_order_no = ? AND del_flag = '0' AND status IN ('0','1') AND receive_salesman_id IS NULL",
                scope.getSalesmanId(),
                workOrderNo);
        if (n == 0) {
            throw new IllegalArgumentException("抢单失败，请重试");
        }
        String orderNo = (String) wo.get("order_no");
        if (orderNo != null && !orderNo.isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE biz_env_order SET receive_salesman_id = ?, status = '2' WHERE order_no = ? AND del_flag = '0'",
                    scope.getSalesmanId(),
                    orderNo);
        }
        return firstMap(openid, workOrderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> assignWorkOrder(String openid, String workOrderNo, long targetSalesmanId) {
        OpenidBizScope scope = scopeService.resolve(openid);
        if (scope.getUserRole() != '1' && scope.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可指派");
        }
        if (scope.getUserRole() == '2' && scope.getAgentId() == null) {
            return null;
        }
        Map<String, Object> wo = loadWorkOrder(workOrderNo);
        if (wo == null) {
            return null;
        }
        if (!canOperateWorkOrder(scope, wo)) {
            return null;
        }
        long agentId = ((Number) wo.get("agent_id")).longValue();
        if (scope.getUserRole() == '2' && scope.getAgentId() != agentId) {
            return null;
        }
        Timestamp adl = (Timestamp) wo.get("accept_deadline");
        if (scope.getUserRole() == '2' && adl != null && System.currentTimeMillis() < adl.getTime()) {
            throw new IllegalArgumentException("抢单进行中（5 分钟内），请到期后再指派");
        }
        String st = String.valueOf(wo.get("status"));
        if (!"1".equals(st) && !"0".equals(st)) {
            throw new IllegalArgumentException("仅待确认/待分配工单可指派");
        }
        if (wo.get("receive_salesman_id") != null) {
            throw new IllegalArgumentException("工单已有接单人，请使用完工或后续流程");
        }
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_salesman WHERE salesman_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                targetSalesmanId,
                agentId);
        if (cnt == null || cnt == 0) {
            throw new IllegalArgumentException("指派对象不是本代理业务员");
        }
        int n = jdbcTemplate.update(
                "UPDATE biz_env_work_order SET receive_salesman_id = ?, status = '2', work_start_time = CURRENT_TIMESTAMP "
                        + "WHERE work_order_no = ? AND del_flag = '0' AND status IN ('0','1') AND receive_salesman_id IS NULL",
                targetSalesmanId,
                workOrderNo);
        if (n == 0) {
            throw new IllegalArgumentException("指派失败");
        }
        String orderNo = (String) wo.get("order_no");
        if (orderNo != null && !orderNo.isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE biz_env_order SET receive_salesman_id = ?, status = '2' WHERE order_no = ? AND del_flag = '0'",
                    targetSalesmanId,
                    orderNo);
        }
        return firstMap(openid, workOrderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finishWorkOrder(String openid, String workOrderNo, WorkOrderFinishRequest req) {
        OpenidBizScope scope = scopeService.resolve(openid);
        Map<String, Object> wo = loadWorkOrder(workOrderNo);
        if (wo == null) {
            return null;
        }
        if (!canOperateWorkOrder(scope, wo)) {
            return null;
        }
        if (!"2".equals(String.valueOf(wo.get("status")))) {
            throw new IllegalArgumentException("仅已接收工单可完工");
        }
        Long recv = wo.get("receive_salesman_id") == null ? null : ((Number) wo.get("receive_salesman_id")).longValue();
        char r = scope.getUserRole();
        if (r == '3') {
            if (scope.getSalesmanId() == null || !scope.getSalesmanId().equals(recv)) {
                throw new IllegalArgumentException("仅接单人可完工");
            }
        } else if (r == '2') {
            if (scope.getAgentId() == null || ((Number) wo.get("agent_id")).longValue() != scope.getAgentId()) {
                return null;
            }
        } else if (r != '1') {
            throw new IllegalArgumentException("无权完工");
        }
        long agentIdWo = ((Number) wo.get("agent_id")).longValue();
        List<AccessoryConsumeLine> consumeLines = req == null ? null : req.getAccessoryConsumes();
        String orderNoEarly = (String) wo.get("order_no");
        String orderTypeEarly = null;
        if (orderNoEarly != null && !orderNoEarly.isEmpty()) {
            Map<String, Object> ordEarly = loadOrderForSettlement(orderNoEarly);
            if (ordEarly != null) {
                orderTypeEarly = String.valueOf(ordEarly.get("order_type"));
            }
        }
        if (r == '3') {
            if ("4".equals(orderTypeEarly)) {
                String dn0 = req == null || req.getDeviceNo() == null ? "" : req.getDeviceNo().trim();
                if (dn0.isEmpty()) {
                    throw new IllegalArgumentException("转移商家工单完工请填写设备编号");
                }
            } else if (consumeLines == null) {
                throw new IllegalArgumentException("结单请提交 JSON，例如 {\"accessoryConsumes\":[]} 或 [{\"typeId\":1,\"qty\":2}]");
            }
        }
        if (r == '3') {
            List<AccessoryConsumeLine> lines = consumeLines == null ? new ArrayList<>() : consumeLines;
            bizDataService.consumeAccessoriesForWorkOrder(agentIdWo, workOrderNo, '3', scope.getSalesmanId(), lines);
        } else if (consumeLines != null && !consumeLines.isEmpty()) {
            bizDataService.consumeAccessoriesForWorkOrder(agentIdWo, workOrderNo, '2', null, consumeLines);
        }
        String orderNo = (String) wo.get("order_no");
        if (orderNo != null && !orderNo.isEmpty()) {
            Map<String, Object> ord = loadOrderForSettlement(orderNo);
            if (ord == null) {
                throw new IllegalArgumentException("关联订单不存在");
            }
            long agId = ((Number) ord.get("agent_id")).longValue();
            long merId = ((Number) ord.get("merchant_id")).longValue();
            String ot = String.valueOf(ord.get("order_type"));
            BigDecimal buckets = ord.get("oil_bucket_count") == null
                    ? BigDecimal.ZERO
                    : ((BigDecimal) ord.get("oil_bucket_count")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pay = ord.get("amount_payable") == null
                    ? BigDecimal.ZERO
                    : ((BigDecimal) ord.get("amount_payable")).setScale(2, RoundingMode.HALF_UP);
            String payType = ord.get("pay_type") == null ? "" : String.valueOf(ord.get("pay_type")).trim();
            if ("1".equals(ot)) {
                stockService.finalizeOilDeduction(agId, orderNo, buckets);
            }
            if ("4".equals(ot)) {
                String dn = req == null || req.getDeviceNo() == null ? "" : req.getDeviceNo().trim();
                if (dn.isEmpty()) {
                    throw new IllegalArgumentException("转移商家完工须填写设备编号");
                }
                Long toMid = ord.get("to_merchant_id") == null ? null : ((Number) ord.get("to_merchant_id")).longValue();
                deviceEventService.transferMerchantDevice(agId, dn, merId, toMid, openid);
            }
            accountService.recordOrderCompleted(agId, merId, orderNo, pay, payType);
            int ou = jdbcTemplate.update(
                    "UPDATE biz_env_order SET status = '3', finish_time = CURRENT_TIMESTAMP WHERE order_no = ? AND del_flag = '0' AND status = '2'",
                    orderNo);
            if (ou == 0) {
                throw new IllegalArgumentException("订单状态已变更，无法完工");
            }
        }
        int wu = jdbcTemplate.update(
                "UPDATE biz_env_work_order SET status = '3', work_end_time = CURRENT_TIMESTAMP WHERE work_order_no = ? AND del_flag = '0' AND status = '2'",
                workOrderNo);
        if (wu == 0) {
            throw new IllegalArgumentException("完工失败");
        }
        return firstMap(openid, workOrderNo);
    }

    private Map<String, Object> loadOrderForSettlement(String orderNo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT agent_id, merchant_id, order_type, oil_bucket_count, amount_payable, to_merchant_id, pay_type "
                        + "FROM biz_env_order WHERE order_no = ? AND del_flag = '0'",
                orderNo);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> firstMap(String openid, String workOrderNo) {
        for (Map<String, Object> m : listWorkOrders(openid)) {
            if (workOrderNo.equals(m.get("workOrderNo"))) {
                return m;
            }
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> loadWorkOrder(String workOrderNo) {
        return jdbcTemplate.query(
                "SELECT work_order_no, order_no, merchant_id, agent_id, status, receive_salesman_id, work_order_type, accept_deadline "
                        + "FROM biz_env_work_order WHERE work_order_no = ? AND del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("work_order_no", rs.getString("work_order_no"));
                    m.put("order_no", rs.getString("order_no"));
                    m.put("merchant_id", rs.getLong("merchant_id"));
                    m.put("agent_id", rs.getLong("agent_id"));
                    m.put("status", rs.getString("status"));
                    m.put("receive_salesman_id", rs.getObject("receive_salesman_id") == null ? null : rs.getLong("receive_salesman_id"));
                    m.put("work_order_type", rs.getString("work_order_type"));
                    m.put("accept_deadline", rs.getTimestamp("accept_deadline"));
                    return m;
                },
                workOrderNo);
    }

    private boolean canOperateWorkOrder(OpenidBizScope scope, Map<String, Object> wo) {
        char r = scope.getUserRole();
        long aid = ((Number) wo.get("agent_id")).longValue();
        long mid = ((Number) wo.get("merchant_id")).longValue();
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
            return scope.getAgentId() != null && scope.getAgentId() == aid;
        }
        return false;
    }

    private void appendWorkOrderScope(StringBuilder sql, List<Object> args, OpenidBizScope scope) {
        char r = scope.getUserRole();
        if (r == '1') {
            return;
        }
        if (r == '2' && scope.getAgentId() != null) {
            sql.append(" AND w.agent_id = ?");
            args.add(scope.getAgentId());
            return;
        }
        if (r == '4' && scope.getMerchantId() != null) {
            sql.append(" AND w.merchant_id = ?");
            args.add(scope.getMerchantId());
            return;
        }
        if (r == '3' && scope.getAgentId() != null && scope.getSalesmanId() != null) {
            sql.append(" AND w.agent_id = ? AND (")
                    .append("(w.status = '1' AND w.receive_salesman_id IS NULL) ")
                    .append("OR w.receive_salesman_id = ? ")
                    .append("OR (m.salesman_id = ? AND m.agent_id = ?)")
                    .append(")");
            args.add(scope.getAgentId());
            args.add(scope.getSalesmanId());
            args.add(scope.getSalesmanId());
            args.add(scope.getAgentId());
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private static String labelWorkType(String code) {
        if ("1".equals(code)) {
            return "加油";
        }
        if ("2".equals(code)) {
            return "维护";
        }
        if ("3".equals(code)) {
            return "外出访问";
        }
        if ("4".equals(code)) {
            return "转移商家";
        }
        return code == null ? "" : code;
    }

    private static String labelWorkStatus(String code) {
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
            return "工单取消";
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

    public static String newWorkOrderNo() {
        return "WO" + LocalDateTime.now().format(WO_NO_TS) + String.format("%02d", (int) (Math.random() * 100));
    }
}
