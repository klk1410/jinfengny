package com.envoil.app.service;

import com.envoil.app.model.DeviceEventCreateRequest;
import com.envoil.app.model.MerchantAuditReviewRequest;
import com.envoil.app.model.OpenidBizScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppDeviceEventAuditService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AppOpenidBizScopeService scopeService;
    private final AppDeviceEventService deviceEventService;

    public AppDeviceEventAuditService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            AppOpenidBizScopeService scopeService,
            @Lazy AppDeviceEventService deviceEventService) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.scopeService = scopeService;
        this.deviceEventService = deviceEventService;
    }

    public List<Map<String, Object>> listAudits(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        StringBuilder sql = new StringBuilder()
                .append("SELECT a.audit_id, a.agent_id, a.device_no, a.event_type, sm.salesman_name AS submitter_salesman_name, ")
                .append("a.submitter_salesman_id, a.status, a.submit_remark, a.review_remark, a.create_time, a.review_time ")
                .append("FROM biz_env_device_event_audit a ")
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
            row.put("agentId", rs.getLong("agent_id"));
            row.put("deviceNo", rs.getString("device_no"));
            row.put("eventTypeCode", rs.getString("event_type"));
            row.put("eventType", labelDeviceEventType(rs.getString("event_type")));
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
                .append("SELECT a.audit_id, a.agent_id, a.device_no, a.event_type, a.status, a.submit_remark, a.review_remark, ")
                .append("a.create_time, a.review_time, a.payload_json, sm.salesman_name AS submitter_salesman_name, ")
                .append("a.submitter_salesman_id ")
                .append("FROM biz_env_device_event_audit a ")
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
                    row.put("agentId", rs.getLong("agent_id"));
                    row.put("deviceNo", rs.getString("device_no"));
                    row.put("eventTypeCode", rs.getString("event_type"));
                    row.put("eventType", labelDeviceEventType(rs.getString("event_type")));
                    row.put("statusCode", rs.getString("status"));
                    row.put("status", labelAuditStatus(rs.getString("status")));
                    row.put("submitRemark", rs.getString("submit_remark"));
                    row.put("reviewRemark", rs.getString("review_remark"));
                    row.put("createTime", formatTs(rs.getTimestamp("create_time")));
                    row.put("reviewTime", formatTs(rs.getTimestamp("review_time")));
                    row.put("submitterSalesmanName", rs.getString("submitter_salesman_name"));
                    row.put("submitterSalesmanId", rs.getObject("submitter_salesman_id") == null ? null : rs.getLong("submitter_salesman_id"));
                    row.put("payload", parsePayloadJson(rs.getString("payload_json")));
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

    private Map<String, Object> parsePayloadJson(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(json, Map.class);
            return m == null ? new LinkedHashMap<>() : new LinkedHashMap<>(m);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitAudit(DeviceEventCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '3' || s.getAgentId() == null || s.getSalesmanId() == null) {
            throw new IllegalArgumentException("仅业务员可提交设备操作审核");
        }
        long agentId = s.getAgentId();
        deviceEventService.preflightDeviceEvent(req, agentId);
        String no = req.getDeviceNo() == null ? "" : req.getDeviceNo().trim();
        Integer pend = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_env_device_event_audit WHERE agent_id = ? AND device_no = ? AND status = '0' AND del_flag = '0'",
                Integer.class,
                agentId,
                no);
        if (pend != null && pend > 0) {
            throw new IllegalArgumentException("该设备已有待审核操作，请等待审批后再提交");
        }
        String payloadJson = toPayloadJson(req);
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_device_event_audit (agent_id, submitter_salesman_id, submitter_openid, event_type, "
                            + "device_no, status, payload_json, submit_remark, del_flag) VALUES (?,?,?,?,?, '0', ?, ?, '0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, agentId);
            ps.setLong(2, s.getSalesmanId());
            ps.setString(3, req.getOpenid());
            ps.setString(4, req.getEventType());
            ps.setString(5, no);
            ps.setString(6, payloadJson);
            ps.setString(7, trimToNull(req.getRemark()));
            return ps;
        }, kh);
        Number id = kh.getKey();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("auditId", id == null ? null : id.longValue());
        out.put("pendingReview", Boolean.TRUE);
        return out;
    }

    private String toPayloadJson(DeviceEventCreateRequest req) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("openid", req.getOpenid());
            m.put("agentId", req.getAgentId());
            m.put("deviceNo", req.getDeviceNo());
            m.put("eventType", req.getEventType());
            m.put("merchantId", req.getMerchantId());
            m.put("remark", req.getRemark());
            m.put("addMode", req.getAddMode());
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("序列化审核内容失败", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(long auditId, MerchantAuditReviewRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '1' && s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可审批");
        }
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT agent_id, status, payload_json, submitter_openid FROM biz_env_device_event_audit WHERE audit_id = ? AND del_flag = '0'",
                auditId);
        long agentIdRow = ((Number) row.get("agent_id")).longValue();
        if (s.getUserRole() == '2') {
            if (s.getAgentId() == null || s.getAgentId().longValue() != agentIdRow) {
                throw new IllegalArgumentException("无权审批该代理下的审核单");
            }
        }
        if (!"0".equals(String.valueOf(row.get("status")))) {
            throw new IllegalArgumentException("该审核单已处理");
        }
        String payloadJson = (String) row.get("payload_json");
        String submitterOpenid = (String) row.get("submitter_openid");
        DeviceEventCreateRequest patch;
        try {
            patch = objectMapper.readValue(payloadJson, DeviceEventCreateRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("审核内容解析失败", e);
        }
        patch.setOpenid(submitterOpenid);
        deviceEventService.applyDeviceEvent(patch, submitterOpenid);
        int n = jdbc.update(
                "UPDATE biz_env_device_event_audit SET status = '1', review_openid = ?, review_remark = ?, review_time = CURRENT_TIMESTAMP "
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
                "SELECT agent_id, status FROM biz_env_device_event_audit WHERE audit_id = ? AND del_flag = '0'", auditId);
        long agentIdRow = ((Number) row.get("agent_id")).longValue();
        if (s.getUserRole() == '2') {
            if (s.getAgentId() == null || s.getAgentId().longValue() != agentIdRow) {
                throw new IllegalArgumentException("无权审批该代理下的审核单");
            }
        }
        if (!"0".equals(String.valueOf(row.get("status")))) {
            throw new IllegalArgumentException("该审核单已处理");
        }
        int n = jdbc.update(
                "UPDATE biz_env_device_event_audit SET status = '2', review_openid = ?, review_remark = ?, review_time = CURRENT_TIMESTAMP "
                        + "WHERE audit_id = ? AND status = '0' AND del_flag = '0'",
                req.getOpenid(),
                trimToNull(req.getReviewRemark()),
                auditId);
        if (n == 0) {
            throw new IllegalStateException("审核单状态已变更，请刷新后重试");
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

    private static String labelDeviceEventType(String code) {
        if ("A".equals(code)) {
            return "新增入库";
        }
        if ("R".equals(code)) {
            return "移除回库";
        }
        if ("S".equals(code)) {
            return "报废";
        }
        if ("T".equals(code)) {
            return "转移至商家";
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
