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
            if (addModeTrim.isEmpty()) {
                throw new IllegalArgumentException("新增设备请选择入库或商家新增");
            }
            if (!"inbound".equals(addModeTrim) && !"merchant".equals(addModeTrim)) {
                throw new IllegalArgumentException("新增方式须为 inbound（入库）或 merchant（商家新增）");
            }
            if ("inbound".equals(addModeTrim)) {
                mid = null;
            } else if (mid == null) {
                throw new IllegalArgumentException("商家新增请选择商家");
            }
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

        if ("A".equals(req.getEventType())) {
            Integer dup = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM biz_env_device WHERE device_no = ? AND del_flag = '0'",
                    Integer.class,
                    no);
            if (dup != null && dup > 0) {
                throw new IllegalArgumentException("设备编号已存在");
            }
            String deviceStatus = "inbound".equals(addModeTrim) ? "0" : "1";
            Long merchantForDevice = "inbound".equals(addModeTrim) ? null : mid;
            jdbc.update(
                    "INSERT INTO biz_env_device (device_type, merchant_id, agent_id, device_no, device_status, del_flag) "
                            + "VALUES ('1',?,?,?,?,'0')",
                    merchantForDevice,
                    agentId,
                    no,
                    deviceStatus);
        }

        jdbc.update(
                "INSERT INTO biz_env_device_event_log (agent_id, merchant_id, device_no, event_type, remark, operator_openid, del_flag) "
                        + "VALUES (?,?,?,?,?,?, '0')",
                agentId,
                mid,
                no,
                req.getEventType(),
                req.getRemark(),
                req.getOpenid());
    }

    private static String labelEvent(String code) {
        if ("A".equals(code)) {
            return "新增";
        }
        if ("R".equals(code)) {
            return "移除";
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
