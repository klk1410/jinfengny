package com.envoil.admin.service;

import com.envoil.admin.model.DashboardSummary;
import com.envoil.admin.model.MerchantView;
import com.envoil.admin.model.OrderView;
import com.envoil.admin.model.WorkOrderView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminBizJdbcService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final RowMapper<MerchantView> MERCHANT_RM = (rs, rowNum) -> {
        MerchantView v = new MerchantView();
        v.setMerchantId(rs.getLong("merchant_id"));
        v.setMerchantName(rs.getString("merchant_name"));
        v.setContactName(rs.getString("contact_name"));
        v.setContactPhone(rs.getString("contact_phone"));
        v.setAgentName(rs.getString("agent_name"));
        v.setSalesmanName(rs.getString("salesman_name"));
        v.setStatus(rs.getString("status_label"));
        return v;
    };

    private static final RowMapper<WorkOrderView> WORK_ORDER_RM = (rs, rowNum) -> {
        WorkOrderView v = new WorkOrderView();
        v.setWorkOrderNo(rs.getString("work_order_no"));
        v.setOrderNo(rs.getString("order_no"));
        v.setMerchantName(rs.getString("merchant_name"));
        v.setWorkOrderType(rs.getString("work_order_type_label"));
        v.setStatus(rs.getString("status_label"));
        v.setReceiveSalesmanName(rs.getString("receive_salesman_name"));
        v.setWorkOrderTime(formatTs(rs.getTimestamp("work_order_time")));
        return v;
    };

    private static final RowMapper<OrderView> ORDER_RM = (rs, rowNum) -> {
        OrderView v = new OrderView();
        v.setOrderNo(rs.getString("order_no"));
        v.setMerchantName(rs.getString("merchant_name"));
        v.setOrderType(rs.getString("order_type_label"));
        v.setStatus(rs.getString("status_label"));
        v.setPayType(rs.getString("pay_type_label"));
        v.setAmountPayable(rs.getBigDecimal("amount_payable").doubleValue());
        v.setCreateTime(formatTs(rs.getTimestamp("order_time")));
        return v;
    };

    private final JdbcTemplate jdbcTemplate;

    public AdminBizJdbcService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary dashboard() {
        DashboardSummary s = new DashboardSummary();
        s.setAgentCount(count("SELECT COUNT(*) FROM biz_env_agent WHERE del_flag='0'"));
        s.setSalesmanCount(count("SELECT COUNT(*) FROM biz_env_salesman WHERE del_flag='0'"));
        s.setMerchantCount(count("SELECT COUNT(*) FROM biz_env_merchant WHERE del_flag='0'"));
        s.setOrderPendingCount(count(
                "SELECT COUNT(*) FROM biz_env_order WHERE del_flag='0' AND status IN ('0','1')"));
        s.setWorkPendingCount(count(
                "SELECT COUNT(*) FROM biz_env_work_order WHERE del_flag='0' AND status IN ('0','1')"));
        return s;
    }

    public List<MerchantView> listMerchants() {
        String sql = ""
                + "SELECT m.merchant_id, m.merchant_name, m.contact_name, m.contact_phone, "
                + "  a.agent_name, s.salesman_name, "
                + "  CASE m.status WHEN '0' THEN '正常' WHEN '1' THEN '停用' ELSE m.status END AS status_label "
                + "FROM biz_env_merchant m "
                + "JOIN biz_env_agent a ON a.agent_id = m.agent_id AND a.del_flag = '0' "
                + "LEFT JOIN biz_env_salesman s ON s.salesman_id = m.salesman_id AND s.del_flag = '0' "
                + "WHERE m.del_flag = '0' "
                + "ORDER BY m.merchant_id";
        return jdbcTemplate.query(sql, MERCHANT_RM);
    }

    public List<OrderView> listOrders() {
        String sql = ""
                + "SELECT o.order_no, m.merchant_name, o.amount_payable, o.order_time, "
                + "  CASE o.order_type WHEN '1' THEN '加油' WHEN '2' THEN '维护' ELSE o.order_type END AS order_type_label, "
                + "  CASE o.status "
                + "    WHEN '0' THEN '待确认' WHEN '1' THEN '待分配' WHEN '2' THEN '已接收' "
                + "    WHEN '3' THEN '已完成' WHEN '4' THEN '订单取消' ELSE o.status END AS status_label, "
                + "  CASE o.pay_type WHEN '1' THEN '微信支付' WHEN '2' THEN '赊销' ELSE o.pay_type END AS pay_type_label "
                + "FROM biz_env_order o "
                + "JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' "
                + "WHERE o.del_flag = '0' "
                + "ORDER BY o.order_time DESC, o.order_id DESC";
        return jdbcTemplate.query(sql, ORDER_RM);
    }

    public List<WorkOrderView> listWorkOrders() {
        String sql = ""
                + "SELECT w.work_order_no, w.order_no, m.merchant_name, w.work_order_time, "
                + "  CASE w.work_order_type WHEN '1' THEN '加油' WHEN '2' THEN '维护' WHEN '3' THEN '外出访问' ELSE w.work_order_type END AS work_order_type_label, "
                + "  CASE w.status "
                + "    WHEN '0' THEN '待确认' WHEN '1' THEN '待分配' WHEN '2' THEN '已接收' "
                + "    WHEN '3' THEN '已完成' WHEN '4' THEN '工单取消' ELSE w.status END AS status_label, "
                + "  rs.salesman_name AS receive_salesman_name "
                + "FROM biz_env_work_order w "
                + "JOIN biz_env_merchant m ON m.merchant_id = w.merchant_id AND m.del_flag = '0' "
                + "LEFT JOIN biz_env_salesman rs ON rs.salesman_id = w.receive_salesman_id AND rs.del_flag = '0' "
                + "WHERE w.del_flag = '0' "
                + "ORDER BY w.work_order_time DESC, w.work_order_id DESC";
        return jdbcTemplate.query(sql, WORK_ORDER_RM);
    }

    public List<Map<String, Object>> listStockInventory() {
        String sql = ""
                + "SELECT s.agent_id, a.agent_name, s.total_qty AS qty_on_hand, s.lock_qty AS qty_reserved, s.available_qty AS qty_available "
                + "FROM biz_env_agent_stock s "
                + "JOIN biz_env_agent a ON a.agent_id = s.agent_id AND a.del_flag = '0' "
                + "WHERE s.stock_item_type = '1' AND (s.stock_item_code IS NULL OR s.stock_item_code = '') AND s.del_flag = '0' "
                + "ORDER BY s.agent_id";
        return jdbcTemplate.query(sql, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agentId", rs.getLong("agent_id"));
            row.put("agentName", rs.getString("agent_name"));
            row.put("qtyOnHand", rs.getBigDecimal("qty_on_hand").doubleValue());
            row.put("qtyReserved", rs.getBigDecimal("qty_reserved").doubleValue());
            row.put("qtyAvailable", rs.getBigDecimal("qty_available").doubleValue());
            return row;
        });
    }

    public List<Map<String, Object>> listStockFlows() {
        String sql = ""
                + "SELECT f.flow_id, f.agent_id, f.change_type AS flow_kind_code, f.related_no AS ref_no, "
                + "f.change_qty AS qty, f.remark, f.create_time, CAST(NULL AS CHAR) AS ref_type "
                + "FROM biz_env_agent_stock_flow f "
                + "JOIN biz_env_agent_stock s ON s.stock_id = f.stock_id AND s.del_flag = '0' "
                + "WHERE s.stock_item_type = '1' AND (s.stock_item_code IS NULL OR s.stock_item_code = '') "
                + "ORDER BY f.flow_id DESC LIMIT 500";
        return jdbcTemplate.query(sql, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("flowId", rs.getLong("flow_id"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("refType", rs.getString("ref_type"));
            row.put("refNo", rs.getString("ref_no"));
            String kindCode = rs.getString("flow_kind_code");
            row.put("flowKind", labelStockFlowKind(kindCode));
            row.put("flowKindCode", kindCode);
            row.put("qty", rs.getBigDecimal("qty").doubleValue());
            row.put("remark", rs.getString("remark"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            return row;
        });
    }

    private static String labelStockFlowKind(String code) {
        if ("1".equals(code) || "I".equals(code)) {
            return "入库";
        }
        if ("2".equals(code) || "R".equals(code)) {
            return "预扣";
        }
        if ("3".equals(code) || "D".equals(code)) {
            return "实扣";
        }
        if ("4".equals(code) || "B".equals(code)) {
            return "回滚";
        }
        return code == null ? "" : code;
    }

    public List<Map<String, Object>> listAccountLedger() {
        String sql = ""
                + "SELECT ledger_id, agent_id, merchant_id, ref_type, ref_no, title, amount, direction, create_time "
                + "FROM biz_env_account_ledger ORDER BY ledger_id DESC LIMIT 500";
        return jdbcTemplate.query(sql, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ledgerId", rs.getLong("ledger_id"));
            row.put("agentId", rs.getObject("agent_id") == null ? null : rs.getLong("agent_id"));
            row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
            row.put("refType", rs.getString("ref_type"));
            row.put("refNo", rs.getString("ref_no"));
            row.put("title", rs.getString("title"));
            row.put("amount", rs.getBigDecimal("amount").doubleValue());
            row.put("direction", rs.getString("direction"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            return row;
        });
    }

    public List<Map<String, Object>> listAccessoryTypes() {
        return jdbcTemplate.query(
                "SELECT type_id, type_name, sort_order FROM biz_env_accessory_type WHERE del_flag = '0' ORDER BY sort_order, type_id",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("typeId", rs.getLong("type_id"));
                    row.put("typeName", rs.getString("type_name"));
                    row.put("sortOrder", rs.getInt("sort_order"));
                    return row;
                });
    }

    public long createAccessoryType(String typeName, int sortOrder) {
        if (typeName == null || typeName.isEmpty()) {
            throw new IllegalArgumentException("种类名称不能为空");
        }
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_accessory_type (type_name, sort_order, del_flag) VALUES (?,?, '0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, typeName);
            ps.setInt(2, sortOrder);
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("未能获取新种类 ID");
        }
        return key.longValue();
    }

    public void updateAccessoryType(long typeId, String typeName, int sortOrder) {
        if (typeName == null || typeName.isEmpty()) {
            throw new IllegalArgumentException("种类名称不能为空");
        }
        int n = jdbcTemplate.update(
                "UPDATE biz_env_accessory_type SET type_name = ?, sort_order = ? WHERE type_id = ? AND del_flag = '0'",
                typeName,
                sortOrder,
                typeId);
        if (n == 0) {
            throw new IllegalArgumentException("种类不存在或已删除");
        }
    }

    public void softDeleteAccessoryType(long typeId) {
        jdbcTemplate.update("UPDATE biz_env_accessory_type SET del_flag = '1' WHERE type_id = ?", typeId);
    }

    private long count(String sql) {
        Long n = jdbcTemplate.queryForObject(sql, Long.class);
        return n == null ? 0L : n;
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        synchronized (TS) {
            return TS.format(ts);
        }
    }
}
