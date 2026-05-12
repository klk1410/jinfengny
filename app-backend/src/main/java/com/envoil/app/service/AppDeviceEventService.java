package com.envoil.app.service;

import com.envoil.app.model.DeviceEventCreateRequest;
import com.envoil.app.model.OpenidBizScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppDeviceEventService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final AppOpenidBizScopeService scopeService;

    public AppDeviceEventService(JdbcTemplate jdbc, AppOpenidBizScopeService scopeService) {
        this.jdbc = jdbc;
        this.scopeService = scopeService;
    }

    public List<Map<String, Object>> listEvents(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT l.log_id, l.agent_id, l.merchant_id, m.merchant_name, l.device_no, ")
                .append("l.event_type, l.remark, l.operator_openid, l.create_time ")
                .append("FROM biz_env_device_event_log l ")
                .append("LEFT JOIN biz_env_merchant m ON m.merchant_id = l.merchant_id AND m.del_flag = '0' ")
                .append("WHERE l.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        char r = s.getUserRole();
        if (r == '1') {
            /* all */
        } else if (r == '2' || r == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND l.agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (r == '4') {
            if (s.getAgentId() == null || s.getMerchantId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND l.agent_id = ? AND (l.merchant_id IS NULL OR l.merchant_id = ?)");
                args.add(s.getAgentId());
                args.add(s.getMerchantId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        sql.append(" ORDER BY l.log_id DESC LIMIT 300");
        return jdbc.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("logId", rs.getLong("log_id"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("deviceNo", rs.getString("device_no"));
            row.put("eventTypeCode", rs.getString("event_type"));
            row.put("eventType", labelEvent(rs.getString("event_type")));
            row.put("remark", rs.getString("remark"));
            row.put("operatorOpenid", rs.getString("operator_openid"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            return row;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void createEvent(DeviceEventCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char role = s.getUserRole();
        if (role != '1' && role != '2' && role != '3') {
            throw new IllegalArgumentException("无权限登记设备事件");
        }
        long agentId;
        if (role == '1') {
            if (req.getAgentId() == null) {
                throw new IllegalArgumentException("主端请指定 agentId");
            }
            agentId = req.getAgentId();
        } else {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        }
        Long mid = req.getMerchantId();
        String no = req.getDeviceNo() == null ? "" : req.getDeviceNo().trim();
        if (no.isEmpty()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }

        String addModeTrim = null;
        if ("A".equals(req.getEventType())) {
            addModeTrim = req.getAddMode() == null ? "" : req.getAddMode().trim();
            if (!addModeTrim.isEmpty() && !"inbound".equals(addModeTrim)) {
                throw new IllegalArgumentException("新增设备仅支持入库（inbound）");
            }
            addModeTrim = "inbound";
            mid = null;
        }

        if (mid != null) {
            Integer ok = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM biz_env_merchant WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                    Integer.class,
                    mid,
                    agentId);
            if (ok == null || ok == 0) {
                throw new IllegalArgumentException("门店不属于该代理");
            }
        }

        if ("S".equals(req.getEventType())) {
            handleScrap(agentId, no, req.getRemark(), req.getOpenid());
            return;
        }

        if ("R".equals(req.getEventType())) {
            handleRemove(agentId, no, req.getRemark(), req.getOpenid());
            return;
        }

        if ("A".equals(req.getEventType())) {
            Integer dup = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM biz_env_device WHERE device_no = ? AND del_flag = '0'",
                    Integer.class,
                    no);
            if (dup != null && dup > 0) {
                throw new IllegalArgumentException("设备编号已存在");
            }
            jdbc.update(
                    "INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag) "
                            + "VALUES ('1',NULL,?,?,'0','0')",
                    agentId,
                    no);

            String logRemark = buildDeviceLogRemark(req.getRemark(), "A", addModeTrim);
            insertDeviceLog(agentId, null, no, "I", logRemark, req.getOpenid());
        }
    }

    /**
     * 转移商家订单完工：设备须在源门店、在店状态，更新为目标门店并记日志（事件 X）。
     */
    public void transferMerchantDevice(long agentId, String deviceNo, long fromMerchantId, Long toMerchantId, String operatorOpenid) {
        if (toMerchantId == null) {
            throw new IllegalArgumentException("订单缺少目标门店");
        }
        if (fromMerchantId == toMerchantId) {
            throw new IllegalArgumentException("源门店与目标门店不能相同");
        }
        String no = deviceNo == null ? "" : deviceNo.trim();
        if (no.isEmpty()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        Integer okFrom = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_env_merchant WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                fromMerchantId,
                agentId);
        if (okFrom == null || okFrom == 0) {
            throw new IllegalArgumentException("源门店不属于该代理");
        }
        Integer okTo = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_env_merchant WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                toMerchantId,
                agentId);
        if (okTo == null || okTo == 0) {
            throw new IllegalArgumentException("目标门店不属于该代理");
        }
        int u = jdbc.update(
                "UPDATE biz_env_device SET merchant_id = ? WHERE device_no = ? AND agent_id = ? AND merchant_id = ? "
                        + "AND device_status = '1' AND del_flag = '0'",
                toMerchantId,
                no,
                agentId,
                fromMerchantId);
        if (u == 0) {
            throw new IllegalArgumentException("设备不存在、不在源门店或非在店状态，无法完成转移");
        }
        String remark = String.format("【转移商家】门店#%d → #%d", fromMerchantId, toMerchantId);
        insertDeviceLog(agentId, toMerchantId, no, "X", remark, operatorOpenid);
    }

    private void insertDeviceLog(long agentId, Long merchantId, String deviceNo, String eventType, String remark, String openid) {
        jdbc.update(
                "INSERT INTO biz_env_device_event_log (agent_id, merchant_id, device_no, event_type, remark, operator_openid, del_flag) "
                        + "VALUES (?,?,?,?,?,?, '0')",
                agentId,
                merchantId,
                deviceNo,
                eventType,
                remark,
                openid);
    }

    private Map<String, Object> loadDeviceRow(String deviceNo, long agentId) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT device_id, merchant_id, device_status FROM biz_env_device WHERE device_no = ? AND agent_id = ? AND del_flag = '0'",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("deviceId", rs.getLong("device_id"));
                    m.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
                    m.put("deviceStatus", rs.getString("device_status"));
                    return m;
                },
                deviceNo,
                agentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void handleRemove(long agentId, String deviceNo, String remark, String openid) {
        Map<String, Object> dev = loadDeviceRow(deviceNo, agentId);
        if (dev == null) {
            throw new IllegalArgumentException("设备不存在或不属于该代理");
        }
        Long dm = (Long) dev.get("merchantId");
        String st = (String) dev.get("deviceStatus");
        if (dm == null) {
            throw new IllegalArgumentException("设备未绑定门店，仅可登记移除已在店（已装机）的设备");
        }
        if (!"1".equals(st)) {
            throw new IllegalArgumentException("仅「在店」状态的设备可登记移除");
        }
        long deviceId = ((Number) dev.get("deviceId")).longValue();
        int u = jdbc.update(
                "UPDATE biz_env_device SET merchant_id = NULL, device_status = '0' WHERE device_id = ? AND agent_id = ? AND del_flag = '0' "
                        + "AND merchant_id IS NOT NULL AND device_status = '1'",
                deviceId,
                agentId);
        if (u == 0) {
            throw new IllegalArgumentException("移除失败，设备状态或绑定已变更");
        }
        String r = remark == null ? "" : remark.trim();
        String logRemark = r.isEmpty() ? "【移除回库】" : "【移除回库】 " + r;
        insertDeviceLog(agentId, dm, deviceNo, "R", logRemark, openid);
    }

    private void handleScrap(long agentId, String deviceNo, String remark, String openid) {
        Map<String, Object> dev = loadDeviceRow(deviceNo, agentId);
        if (dev == null) {
            throw new IllegalArgumentException("设备不存在或不属于该代理");
        }
        Long dm = (Long) dev.get("merchantId");
        String st = (String) dev.get("deviceStatus");
        if (dm != null) {
            throw new IllegalArgumentException("仅对在库且未绑定门店的设备可报废");
        }
        if (!"0".equals(st)) {
            throw new IllegalArgumentException("仅「在库（可调拨）」状态的设备可报废");
        }
        long deviceId = ((Number) dev.get("deviceId")).longValue();
        int u = jdbc.update(
                "UPDATE biz_env_device SET device_status = '4' WHERE device_id = ? AND agent_id = ? AND del_flag = '0' "
                        + "AND merchant_id IS NULL AND device_status = '0'",
                deviceId,
                agentId);
        if (u == 0) {
            throw new IllegalArgumentException("报废失败，设备状态已变更");
        }
        String r = remark == null ? "" : remark.trim();
        String logRemark = r.isEmpty() ? "【报废】" : "【报废】 " + r;
        insertDeviceLog(agentId, null, deviceNo, "S", logRemark, openid);
    }

    private static String buildDeviceLogRemark(String remark, String eventType, String addModeTrim) {
        String rr = remark == null ? "" : remark.trim();
        if (!"A".equals(eventType) || addModeTrim == null || addModeTrim.isEmpty()) {
            return rr.isEmpty() ? null : rr;
        }
        String prefix;
        if ("inbound".equals(addModeTrim)) {
            prefix = "【入库】";
        } else {
            return rr.isEmpty() ? null : rr;
        }
        if (rr.isEmpty()) {
            return prefix;
        }
        return prefix + " " + rr;
    }

    private static String labelEvent(String code) {
        if ("A".equals(code)) {
            return "新增";
        }
        if ("I".equals(code)) {
            return "入库";
        }
        if ("T".equals(code)) {
            return "转移至商家";
        }
        if ("X".equals(code)) {
            return "转移商家";
        }
        if ("R".equals(code)) {
            return "移除";
        }
        if ("S".equals(code)) {
            return "报废";
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
