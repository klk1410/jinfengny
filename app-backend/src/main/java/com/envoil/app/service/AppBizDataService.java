package com.envoil.app.service;

import com.envoil.app.model.MerchantCreateRequest;
import com.envoil.app.model.OpenidBizScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppBizDataService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final AppOpenidBizScopeService scopeService;
    private final AppBizStockService stockService;

    public AppBizDataService(
            JdbcTemplate jdbcTemplate,
            AppOpenidBizScopeService scopeService,
            @Lazy AppBizStockService stockService) {
        this.jdbcTemplate = jdbcTemplate;
        this.scopeService = scopeService;
        this.stockService = stockService;
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

    /**
     * 主端、代理、业务员可新增店铺（商家端无此权限）。
     */
    public Map<String, Object> createMerchant(MerchantCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        long agentId;
        Long salesmanId = req.getSalesmanId();
        if (r == '1') {
            if (req.getAgentId() == null) {
                throw new IllegalArgumentException("主端创建店铺请指定 agentId");
            }
            agentId = req.getAgentId();
        } else if (r == '2') {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        } else if (r == '3') {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
            if (salesmanId == null) {
                salesmanId = s.getSalesmanId();
            }
        } else {
            throw new IllegalArgumentException("无权限新增店铺");
        }
        if (salesmanId != null) {
            ensureSalesmanBelongsToAgent(salesmanId, agentId);
        }
        if (req.getLinkedMerchantId() != null) {
            ensureLinkedMerchant(req.getLinkedMerchantId(), agentId);
        }
        if (req.getLongitude() == null || req.getLatitude() == null) {
            throw new IllegalArgumentException("请填写经纬度");
        }
        final Long insertSalesmanId = salesmanId;
        BigDecimal oil = BigDecimal.valueOf(req.getOilUnitPrice() == null ? 0 : req.getOilUnitPrice());
        BigDecimal comm = BigDecimal.valueOf(req.getMerchantCommission() == null ? 0 : req.getMerchantCommission());
        BigDecimal lon = BigDecimal.valueOf(req.getLongitude());
        BigDecimal lat = BigDecimal.valueOf(req.getLatitude());
        String img = req.getStoreImageUrl();
        if (img != null && img.length() > 8000) {
            throw new IllegalArgumentException("店铺图片数据过大，请压缩或使用外链地址");
        }
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_merchant (agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone, "
                            + "longitude, latitude, province, city, district, address_detail, oil_unit_price, merchant_commission, "
                            + "remark, store_image_url, linked_merchant_id, status, del_flag) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'0','0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, agentId);
            if (insertSalesmanId == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, insertSalesmanId);
            }
            ps.setString(3, req.getIndustryType());
            ps.setString(4, req.getMerchantName());
            ps.setString(5, req.getContactName());
            ps.setString(6, req.getContactPhone());
            ps.setBigDecimal(7, lon);
            ps.setBigDecimal(8, lat);
            ps.setString(9, req.getProvince());
            ps.setString(10, req.getCity());
            ps.setString(11, req.getDistrict());
            ps.setString(12, req.getAddressDetail());
            ps.setBigDecimal(13, oil);
            ps.setBigDecimal(14, comm);
            ps.setString(15, req.getRemark());
            ps.setString(16, img);
            if (req.getLinkedMerchantId() == null) {
                ps.setObject(17, null);
            } else {
                ps.setLong(17, req.getLinkedMerchantId());
            }
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("未能获取新店铺 ID");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("merchantId", key.longValue());
        return out;
    }

    private void ensureSalesmanBelongsToAgent(long salesmanId, long agentId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_salesman WHERE salesman_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                salesmanId,
                agentId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("业务员不属于当前代理");
        }
    }

    private void ensureLinkedMerchant(long linkedMerchantId, long agentId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_merchant WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                linkedMerchantId,
                agentId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("关联商家不存在或不属于当前代理");
        }
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

    /**
     * 库存汇总（按代理）；主端可查全部或指定 agentId。
     */
    public List<Map<String, Object>> listStockSummary(String openid, Long filterAgentId) {
        OpenidBizScope s = scopeService.resolve(openid);
        Long aid = resolveScopedAgentId(s, filterAgentId);
        if (s.getUserRole() == '1' && aid == null) {
            return jdbcTemplate.query(
                    "SELECT s.agent_id, a.agent_name, s.qty_on_hand, s.qty_reserved, "
                            + " (s.qty_on_hand - s.qty_reserved) AS qty_available "
                            + "FROM biz_env_agent_stock s "
                            + "JOIN biz_env_agent a ON a.agent_id = s.agent_id AND a.del_flag = '0' "
                            + "WHERE s.sku_code = '1' ORDER BY s.agent_id",
                    (rs, i) -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("agentId", rs.getLong("agent_id"));
                        row.put("agentName", rs.getString("agent_name"));
                        row.put("qtyOnHand", rs.getBigDecimal("qty_on_hand").doubleValue());
                        row.put("qtyReserved", rs.getBigDecimal("qty_reserved").doubleValue());
                        row.put("qtyAvailable", rs.getBigDecimal("qty_available").doubleValue());
                        return row;
                    });
        }
        if (aid == null) {
            return new ArrayList<>();
        }
        stockService.ensureAgentRow(aid);
        return jdbcTemplate.query(
                "SELECT s.agent_id, a.agent_name, s.qty_on_hand, s.qty_reserved, "
                        + " (s.qty_on_hand - s.qty_reserved) AS qty_available "
                        + "FROM biz_env_agent_stock s "
                        + "JOIN biz_env_agent a ON a.agent_id = s.agent_id AND a.del_flag = '0' "
                        + "WHERE s.agent_id = ? AND s.sku_code = '1'",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("agentId", rs.getLong("agent_id"));
                    row.put("agentName", rs.getString("agent_name"));
                    row.put("qtyOnHand", rs.getBigDecimal("qty_on_hand").doubleValue());
                    row.put("qtyReserved", rs.getBigDecimal("qty_reserved").doubleValue());
                    row.put("qtyAvailable", rs.getBigDecimal("qty_available").doubleValue());
                    return row;
                },
                aid);
    }

    public List<Map<String, Object>> listStockFlows(String openid, Long filterAgentId) {
        OpenidBizScope s = scopeService.resolve(openid);
        Long aid = resolveScopedAgentId(s, filterAgentId);
        if (s.getUserRole() == '1' && aid == null) {
            return jdbcTemplate.query(
                    "SELECT flow_id, agent_id, ref_type, ref_no, flow_kind, qty, remark, create_time "
                            + "FROM biz_env_stock_flow ORDER BY flow_id DESC LIMIT 200",
                    (rs, i) -> flowRow(rs));
        }
        if (aid == null) {
            return new ArrayList<>();
        }
        return jdbcTemplate.query(
                "SELECT flow_id, agent_id, ref_type, ref_no, flow_kind, qty, remark, create_time "
                        + "FROM biz_env_stock_flow WHERE agent_id = ? ORDER BY flow_id DESC LIMIT 200",
                (rs, i) -> flowRow(rs),
                aid);
    }

    public List<Map<String, Object>> listAccountLedger(String openid, Long filterAgentId) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        // 账目流水仅主端、代理可查；业务员与商家不可见
        if (r == '3' || r == '4') {
            return new ArrayList<>();
        }
        Long aid = resolveScopedAgentId(s, filterAgentId);
        if (r == '1' && aid == null) {
            return jdbcTemplate.query(
                    "SELECT ledger_id, agent_id, merchant_id, ref_type, ref_no, title, amount, direction, create_time "
                            + "FROM biz_env_account_ledger ORDER BY ledger_id DESC LIMIT 200",
                    (rs, i) -> ledgerRow(rs));
        }
        if (aid == null) {
            return new ArrayList<>();
        }
        return jdbcTemplate.query(
                "SELECT ledger_id, agent_id, merchant_id, ref_type, ref_no, title, amount, direction, create_time "
                        + "FROM biz_env_account_ledger WHERE agent_id = ? ORDER BY ledger_id DESC LIMIT 200",
                (rs, i) -> ledgerRow(rs),
                aid);
    }

    /** 账户信息（基础信息卡 + 组织信息卡） */
    public Map<String, Object> accountProfile(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("openid", openid);
        out.put("roleCode", String.valueOf(r));
        out.put("roleName", roleName(r));
        // env_mini_subject 首版无 create_time，这里预留字段，后续有库字段可补
        out.put("bindTime", "");

        Map<String, Object> org = new LinkedHashMap<>();
        if (r == '1') {
            org.put("platformName", "环保油平台");
            org.put("adminName", "平台管理员");
        } else if (r == '2') {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "SELECT agent_name, contact_name, contact_phone, province, city, district "
                            + "FROM biz_env_agent WHERE agent_id = ? AND del_flag = '0'",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("agentName", rs.getString("agent_name"));
                        m.put("contactName", rs.getString("contact_name"));
                        m.put("contactPhone", rs.getString("contact_phone"));
                        m.put("region", String.join("/",
                                nullSafe(rs.getString("province")),
                                nullSafe(rs.getString("city")),
                                nullSafe(rs.getString("district"))));
                        return m;
                    },
                    s.getAgentId());
            if (!rows.isEmpty()) {
                org.putAll(rows.get(0));
            }
        } else if (r == '3') {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "SELECT sm.salesman_name, sm.phone, a.agent_name "
                            + "FROM biz_env_salesman sm "
                            + "LEFT JOIN biz_env_agent a ON a.agent_id = sm.agent_id AND a.del_flag = '0' "
                            + "WHERE sm.salesman_id = ? AND sm.del_flag = '0'",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("salesmanName", rs.getString("salesman_name"));
                        m.put("phone", rs.getString("phone"));
                        m.put("agentName", rs.getString("agent_name"));
                        return m;
                    },
                    s.getSalesmanId());
            if (!rows.isEmpty()) {
                org.putAll(rows.get(0));
            }
        } else if (r == '4') {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "SELECT m.merchant_name, m.contact_name, m.contact_phone, "
                            + "a.agent_name, sm.salesman_name "
                            + "FROM biz_env_merchant m "
                            + "LEFT JOIN biz_env_agent a ON a.agent_id = m.agent_id AND a.del_flag = '0' "
                            + "LEFT JOIN biz_env_salesman sm ON sm.salesman_id = m.salesman_id AND sm.del_flag = '0' "
                            + "WHERE m.merchant_id = ? AND m.del_flag = '0'",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("merchantName", rs.getString("merchant_name"));
                        m.put("contactName", rs.getString("contact_name"));
                        m.put("contactPhone", rs.getString("contact_phone"));
                        m.put("agentName", rs.getString("agent_name"));
                        m.put("salesmanName", rs.getString("salesman_name"));
                        return m;
                    },
                    s.getMerchantId());
            if (!rows.isEmpty()) {
                org.putAll(rows.get(0));
            }
        }
        out.put("orgInfo", org);
        return out;
    }

    public void inboundStock(String openid, BigDecimal qty, Long agentIdForMain, String remark) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        if (r != '1' && r != '2') {
            throw new IllegalArgumentException("仅主端或代理可入库");
        }
        long agentId;
        if (r == '1') {
            if (agentIdForMain == null) {
                throw new IllegalArgumentException("主端入库请指定 agentId");
            }
            agentId = agentIdForMain;
        } else {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        }
        stockService.inboundOil(agentId, qty, remark);
    }

    private Long resolveScopedAgentId(OpenidBizScope s, Long filterAgentId) {
        char r = s.getUserRole();
        if (r == '1') {
            return filterAgentId;
        }
        if (r == '2' || r == '3') {
            return s.getAgentId();
        }
        if (r == '4') {
            if (s.getMerchantId() == null) {
                return null;
            }
            List<Long> aids = jdbcTemplate.query(
                    "SELECT agent_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                    (rs, rowNum) -> rs.getLong("agent_id"),
                    s.getMerchantId());
            return aids.isEmpty() ? null : aids.get(0);
        }
        return null;
    }

    private static Map<String, Object> flowRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("flowId", rs.getLong("flow_id"));
        row.put("agentId", rs.getLong("agent_id"));
        row.put("refType", rs.getString("ref_type"));
        row.put("refNo", rs.getString("ref_no"));
        row.put("flowKind", labelFlowKind(rs.getString("flow_kind")));
        row.put("flowKindCode", rs.getString("flow_kind"));
        row.put("qty", rs.getBigDecimal("qty").doubleValue());
        row.put("remark", rs.getString("remark"));
        row.put("createTime", formatTs(rs.getTimestamp("create_time")));
        return row;
    }

    private static Map<String, Object> ledgerRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ledgerId", rs.getLong("ledger_id"));
        row.put("agentId", rs.getObject("agent_id") == null ? null : rs.getLong("agent_id"));
        row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
        row.put("refType", rs.getString("ref_type"));
        row.put("refNo", rs.getString("ref_no"));
        row.put("title", rs.getString("title"));
        row.put("amount", rs.getBigDecimal("amount").doubleValue());
        row.put("direction", "1".equals(rs.getString("direction")) ? "收入" : "支出");
        row.put("directionCode", rs.getString("direction"));
        row.put("createTime", formatTs(rs.getTimestamp("create_time")));
        return row;
    }

    private static String labelFlowKind(String code) {
        if ("R".equals(code)) {
            return "预扣";
        }
        if ("D".equals(code)) {
            return "实扣";
        }
        if ("B".equals(code)) {
            return "回滚";
        }
        if ("I".equals(code)) {
            return "入库";
        }
        return code == null ? "" : code;
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        synchronized (TS) {
            return TS.format(ts);
        }
    }

    private static String roleName(char code) {
        if (code == '1') return "主端";
        if (code == '2') return "代理";
        if (code == '3') return "业务员";
        if (code == '4') return "商家";
        return String.valueOf(code);
    }

    private static String nullSafe(String v) {
        return v == null ? "" : v;
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
