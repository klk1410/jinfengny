package com.envoil.app.service;

import com.envoil.app.model.MerchantAuditReviewRequest;
import com.envoil.app.model.MerchantUpdateRequest;
import com.envoil.app.model.OpenidBizScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppMerchantAuditService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AppOpenidBizScopeService scopeService;
    private final AppBizDataService bizDataService;

    public AppMerchantAuditService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            AppOpenidBizScopeService scopeService,
            @Lazy AppBizDataService bizDataService) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.scopeService = scopeService;
        this.bizDataService = bizDataService;
    }

    public List<Map<String, Object>> listAudits(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        StringBuilder sql = new StringBuilder()
                .append("SELECT a.audit_id, a.merchant_id, m.merchant_name, a.agent_id, a.submitter_salesman_id, ")
                .append("sm.salesman_name AS submitter_salesman_name, a.status, a.submit_remark, a.review_remark, ")
                .append("a.create_time, a.review_time ")
                .append("FROM biz_env_merchant_audit a ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = a.merchant_id AND m.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman sm ON sm.salesman_id = a.submitter_salesman_id AND sm.del_flag = '0' ")
                .append("WHERE a.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (r == '1') {
            /* all */
        } else if (r == '2') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (r == '3') {
            if (s.getAgentId() == null || s.getSalesmanId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ? AND a.submitter_salesman_id = ?");
                args.add(s.getAgentId());
                args.add(s.getSalesmanId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        sql.append(" ORDER BY a.audit_id DESC LIMIT 500");
        return jdbc.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("auditId", rs.getLong("audit_id"));
            row.put("merchantId", rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("submitterSalesmanId", rs.getObject("submitter_salesman_id") == null ? null : rs.getLong("submitter_salesman_id"));
            row.put("submitterSalesmanName", rs.getString("submitter_salesman_name"));
            row.put("statusCode", rs.getString("status"));
            row.put("status", labelAuditStatus(rs.getString("status")));
            row.put("submitRemark", rs.getString("submit_remark"));
            row.put("reviewRemark", rs.getString("review_remark"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            row.put("reviewTime", formatTs(rs.getTimestamp("review_time")));
            return row;
        });
    }

    public Map<String, Object> getAuditDetail(String openid, long auditId) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        StringBuilder sql = new StringBuilder()
                .append("SELECT a.audit_id, a.merchant_id, m.merchant_name, a.agent_id, a.submitter_salesman_id, ")
                .append("sm.salesman_name AS submitter_salesman_name, a.status, a.submit_remark, a.review_remark, ")
                .append("a.create_time, a.review_time, a.payload_json ")
                .append("FROM biz_env_merchant_audit a ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = a.merchant_id AND m.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman sm ON sm.salesman_id = a.submitter_salesman_id AND sm.del_flag = '0' ")
                .append("WHERE a.audit_id = ? AND a.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        args.add(auditId);
        if (r == '1') {
            /* all */
        } else if (r == '2') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (r == '3') {
            if (s.getAgentId() == null || s.getSalesmanId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ? AND a.submitter_salesman_id = ?");
                args.add(s.getAgentId());
                args.add(s.getSalesmanId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        List<Map<String, Object>> rows = jdbc.query(
                sql.toString(),
                args.toArray(),
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("auditId", rs.getLong("audit_id"));
                    row.put("merchantId", rs.getLong("merchant_id"));
                    row.put("merchantName", rs.getString("merchant_name"));
                    row.put("agentId", rs.getLong("agent_id"));
                    row.put("submitterSalesmanId", rs.getObject("submitter_salesman_id") == null ? null : rs.getLong("submitter_salesman_id"));
                    row.put("submitterSalesmanName", rs.getString("submitter_salesman_name"));
                    row.put("statusCode", rs.getString("status"));
                    row.put("status", labelAuditStatus(rs.getString("status")));
                    row.put("submitRemark", rs.getString("submit_remark"));
                    row.put("reviewRemark", rs.getString("review_remark"));
                    row.put("createTime", formatTs(rs.getTimestamp("create_time")));
                    row.put("reviewTime", formatTs(rs.getTimestamp("review_time")));
                    row.put("payload", parsePayload(rs.getString("payload_json")));
                    return row;
                });
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("审核单不存在或无权查看");
        }
        Map<String, Object> row = rows.get(0);
        boolean canReview = (s.getUserRole() == '1' || s.getUserRole() == '2')
                && "0".equals(row.get("statusCode"));
        if (s.getUserRole() == '2' && s.getAgentId() != null) {
            canReview = canReview && ((Number) row.get("agentId")).longValue() == s.getAgentId().longValue();
        }
        row.put("canReview", canReview);
        return row;
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            JsonNode n = objectMapper.readTree(json);
            Map<String, Object> out = new LinkedHashMap<>();
            n.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v.isNull()) {
                    out.put(e.getKey(), null);
                } else if (v.isNumber()) {
                    out.put(e.getKey(), v.numberValue());
                } else if (v.isBoolean()) {
                    out.put(e.getKey(), v.booleanValue());
                } else {
                    out.put(e.getKey(), v.asText());
                }
            });
            return out;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitAudit(MerchantUpdateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '3' || s.getAgentId() == null || s.getSalesmanId() == null) {
            throw new IllegalArgumentException("仅业务员可发起店铺修改审核");
        }
        long merchantId = req.getMerchantId();
        Map<String, Object> m = bizDataService.getMerchantDetail(req.getOpenid(), merchantId);
        long agentId = ((Number) m.get("agentId")).longValue();
        if (agentId != s.getAgentId().longValue()) {
            throw new IllegalArgumentException("店铺不属于当前代理");
        }
        Long midSales = m.get("salesmanId") == null ? null : ((Number) m.get("salesmanId")).longValue();
        if (midSales == null || midSales.longValue() != s.getSalesmanId().longValue()) {
            throw new IllegalArgumentException("仅负责本店的业务员可发起审核");
        }
        Integer pend = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_env_merchant_audit WHERE merchant_id = ? AND status = '0' AND del_flag = '0'",
                Integer.class,
                merchantId);
        if (pend != null && pend > 0) {
            throw new IllegalArgumentException("该店铺已有待审修改单，请等待审批后再提交");
        }
        String payload = toPayloadJson(req);
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_merchant_audit (merchant_id, agent_id, submitter_salesman_id, submitter_openid, status, "
                            + "payload_json, submit_remark, del_flag) VALUES (?,?,?,?, '0', ?, ?, '0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, merchantId);
            ps.setLong(2, agentId);
            ps.setLong(3, s.getSalesmanId());
            ps.setString(4, req.getOpenid());
            ps.setString(5, payload);
            ps.setString(6, trimToNull(req.getSubmitRemark()));
            return ps;
        }, kh);
        Number id = kh.getKey();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("auditId", id == null ? null : id.longValue());
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(long auditId, MerchantAuditReviewRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '1' && s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可审批");
        }
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT merchant_id, agent_id, status, payload_json FROM biz_env_merchant_audit WHERE audit_id = ? AND del_flag = '0'",
                auditId);
        long agentId = ((Number) row.get("agent_id")).longValue();
        if (s.getUserRole() == '2') {
            if (s.getAgentId() == null || s.getAgentId().longValue() != agentId) {
                throw new IllegalArgumentException("无权审批该代理下的审核单");
            }
        }
        if (!"0".equals(String.valueOf(row.get("status")))) {
            throw new IllegalArgumentException("该审核单已处理");
        }
        long merchantId = ((Number) row.get("merchant_id")).longValue();
        String payloadJson = (String) row.get("payload_json");
        MerchantUpdateRequest patch = parsePayloadToRequest(payloadJson);
        patch.setMerchantId(merchantId);
        patch.setOpenid(req.getOpenid());
        bizDataService.applyMerchantUpdate(merchantId, agentId, patch);
        int n = jdbc.update(
                "UPDATE biz_env_merchant_audit SET status = '1', review_openid = ?, review_remark = ?, review_time = CURRENT_TIMESTAMP "
                        + "WHERE audit_id = ? AND status = '0' AND del_flag = '0'",
                req.getOpenid(),
                trimToNull(req.getReviewRemark()),
                auditId);
        if (n == 0) {
            throw new IllegalStateException("审核单状态已变更，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(long auditId, MerchantAuditReviewRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '1' && s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可审批");
        }
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT agent_id, status FROM biz_env_merchant_audit WHERE audit_id = ? AND del_flag = '0'", auditId);
        long agentId = ((Number) row.get("agent_id")).longValue();
        if (s.getUserRole() == '2') {
            if (s.getAgentId() == null || s.getAgentId().longValue() != agentId) {
                throw new IllegalArgumentException("无权审批该代理下的审核单");
            }
        }
        if (!"0".equals(String.valueOf(row.get("status")))) {
            throw new IllegalArgumentException("该审核单已处理");
        }
        int n = jdbc.update(
                "UPDATE biz_env_merchant_audit SET status = '2', review_openid = ?, review_remark = ?, review_time = CURRENT_TIMESTAMP "
                        + "WHERE audit_id = ? AND status = '0' AND del_flag = '0'",
                req.getOpenid(),
                trimToNull(req.getReviewRemark()),
                auditId);
        if (n == 0) {
            throw new IllegalStateException("审核单状态已变更，请刷新后重试");
        }
    }

    private MerchantUpdateRequest parsePayloadToRequest(String payloadJson) {
        try {
            JsonNode n = objectMapper.readTree(payloadJson);
            return objectMapper.treeToValue(n, MerchantUpdateRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("审核内容解析失败", e);
        }
    }

    private String toPayloadJson(MerchantUpdateRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("industryType", r.getIndustryType());
        m.put("merchantName", r.getMerchantName());
        m.put("contactName", r.getContactName());
        m.put("contactPhone", r.getContactPhone());
        m.put("province", r.getProvince());
        m.put("city", r.getCity());
        m.put("district", r.getDistrict());
        m.put("addressDetail", r.getAddressDetail());
        m.put("longitude", r.getLongitude());
        m.put("latitude", r.getLatitude());
        m.put("oilUnitPrice", r.getOilUnitPrice());
        m.put("merchantCommission", r.getMerchantCommission());
        m.put("salesmanId", r.getSalesmanId());
        m.put("linkedMerchantId", r.getLinkedMerchantId());
        m.put("remark", r.getRemark());
        m.put("storeImageUrl", r.getStoreImageUrl());
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("序列化审核内容失败", e);
        }
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String labelAuditStatus(String code) {
        if ("0".equals(code)) {
            return "待审";
        }
        if ("1".equals(code)) {
            return "已通过";
        }
        if ("2".equals(code)) {
            return "已驳回";
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
}
