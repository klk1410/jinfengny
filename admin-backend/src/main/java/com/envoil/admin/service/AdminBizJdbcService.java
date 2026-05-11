package com.envoil.admin.service;

import com.envoil.admin.model.DashboardSummary;
import com.envoil.admin.model.MerchantView;
import com.envoil.admin.model.OrderView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

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
                + "  CASE o.pay_type WHEN '1' THEN '现结' WHEN '2' THEN '赊欠' ELSE o.pay_type END AS pay_type_label "
                + "FROM biz_env_order o "
                + "JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' "
                + "WHERE o.del_flag = '0' "
                + "ORDER BY o.order_time DESC, o.order_id DESC";
        return jdbcTemplate.query(sql, ORDER_RM);
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
