package com.envoil.app.service;

import com.envoil.app.model.OpenidBizScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppBizDataService {

    private final JdbcTemplate jdbcTemplate;
    private final AppOpenidBizScopeService scopeService;

    public AppBizDataService(JdbcTemplate jdbcTemplate, AppOpenidBizScopeService scopeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.scopeService = scopeService;
    }

    public List<Map<String, Object>> listMerchants(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT m.merchant_id, m.merchant_name, m.contact_name, m.contact_phone, m.city, ")
                .append("m.oil_unit_price, m.arrears_amount, a.agent_name, sm.salesman_name, m.status ")
                .append("FROM biz_env_merchant m ")
                .append("JOIN biz_env_agent a ON a.agent_id = m.agent_id AND a.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman sm ON sm.salesman_id = m.salesman_id AND sm.del_flag = '0' ")
                .append("WHERE m.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendMerchantScope(sql, args, s);
        sql.append(" ORDER BY m.merchant_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("merchantId", rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("contactName", rs.getString("contact_name"));
            row.put("contactPhone", rs.getString("contact_phone"));
            row.put("city", rs.getString("city"));
            row.put("oilUnitPrice", rs.getBigDecimal("oil_unit_price").doubleValue());
            row.put("arrearsAmount", rs.getBigDecimal("arrears_amount").doubleValue());
            row.put("agentName", rs.getString("agent_name"));
            row.put("salesmanName", rs.getString("salesman_name"));
            row.put("status", labelMerchantStatus(rs.getString("status")));
            return row;
        });
    }

    public List<Map<String, Object>> listAgents(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT agent_id, agent_name, contact_name, contact_phone, province, city, status ")
                .append("FROM biz_env_agent WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (s.getUserRole() == '2' || s.getUserRole() == '3' || s.getUserRole() == '4') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND agent_id = ?");
                args.add(s.getAgentId());
            }
        }
        sql.append(" ORDER BY agent_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agentId", rs.getLong("agent_id"));
            row.put("agentName", rs.getString("agent_name"));
            row.put("contactName", rs.getString("contact_name"));
            row.put("contactPhone", rs.getString("contact_phone"));
            row.put("province", rs.getString("province"));
            row.put("city", rs.getString("city"));
            row.put("status", labelAgentStatus(rs.getString("status")));
            return row;
        });
    }

    public List<Map<String, Object>> listSalesmen(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT salesman_id, salesman_name, phone, agent_id, status ")
                .append("FROM biz_env_salesman WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (s.getUserRole() == '2' || s.getUserRole() == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND agent_id = ?");
                args.add(s.getAgentId());
            }
            if (s.getUserRole() == '3' && s.getSalesmanId() != null) {
                sql.append(" AND salesman_id = ?");
                args.add(s.getSalesmanId());
            }
        } else if (s.getUserRole() == '4') {
            Long sid = null;
            if (s.getMerchantId() != null) {
                sid = jdbcTemplate.query(
                        "SELECT salesman_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            long v = rs.getLong("salesman_id");
                            return rs.wasNull() ? null : v;
                        },
                        s.getMerchantId());
            }
            if (sid == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND salesman_id = ?");
                args.add(sid);
            }
        }
        sql.append(" ORDER BY salesman_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("salesmanId", rs.getLong("salesman_id"));
            row.put("salesmanName", rs.getString("salesman_name"));
            row.put("phone", rs.getString("phone"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("status", labelSalesmanStatus(rs.getString("status")));
            return row;
        });
    }

    public List<Map<String, Object>> listDevices(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT d.device_id, d.device_no, d.device_type, d.device_status, d.merchant_id, m.merchant_name, d.agent_id ")
                .append("FROM biz_env_device d ")
                .append("LEFT JOIN biz_env_merchant m ON m.merchant_id = d.merchant_id AND m.del_flag = '0' ")
                .append("WHERE d.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (s.getUserRole() == '1') {
            // no filter
        } else if (s.getUserRole() == '2' || s.getUserRole() == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND d.agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (s.getUserRole() == '4') {
            if (s.getMerchantId() == null || s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND d.agent_id = ? AND (d.merchant_id = ? OR d.merchant_id IS NULL)");
                args.add(s.getAgentId());
                args.add(s.getMerchantId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        sql.append(" ORDER BY d.device_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deviceId", rs.getLong("device_id"));
            row.put("deviceNo", rs.getString("device_no"));
            row.put("deviceType", labelDeviceType(rs.getString("device_type")));
            row.put("deviceStatus", labelDeviceStatus(rs.getString("device_status")));
            row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("agentId", rs.getLong("agent_id"));
            return row;
        });
    }

    private void appendMerchantScope(StringBuilder sql, List<Object> args, OpenidBizScope s) {
        char r = s.getUserRole();
        if (r == '1') {
            return;
        }
        if (r == '2' && s.getAgentId() != null) {
            sql.append(" AND m.agent_id = ?");
            args.add(s.getAgentId());
            return;
        }
        if (r == '3' && s.getAgentId() != null && s.getSalesmanId() != null) {
            sql.append(" AND m.agent_id = ? AND m.salesman_id = ?");
            args.add(s.getAgentId());
            args.add(s.getSalesmanId());
            return;
        }
        if (r == '4' && s.getMerchantId() != null) {
            sql.append(" AND m.merchant_id = ?");
            args.add(s.getMerchantId());
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private static String labelMerchantStatus(String code) {
        if ("0".equals(code)) {
            return "正常";
        }
        if ("1".equals(code)) {
            return "停用";
        }
        return code == null ? "" : code;
    }

    private static String labelAgentStatus(String code) {
        return labelMerchantStatus(code);
    }

    private static String labelSalesmanStatus(String code) {
        return labelMerchantStatus(code);
    }

    private static String labelDeviceType(String code) {
        if ("1".equals(code)) {
            return "油机";
        }
        if ("2".equals(code)) {
            return "其它";
        }
        return code == null ? "" : code;
    }

    private static String labelDeviceStatus(String code) {
        if ("0".equals(code)) {
            return "停用";
        }
        if ("1".equals(code)) {
            return "正常";
        }
        return code == null ? "" : code;
    }
}
