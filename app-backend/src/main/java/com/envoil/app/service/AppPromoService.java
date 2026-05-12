package com.envoil.app.service;

import com.envoil.app.model.OpenidBizScope;
import com.envoil.app.model.PromoCoopCreateRequest;
import com.envoil.app.model.PromoPrepayCreateRequest;
import com.envoil.app.model.PromoWithdrawApplyRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppPromoService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final AppOpenidBizScopeService scopeService;

    public AppPromoService(JdbcTemplate jdbc, AppOpenidBizScopeService scopeService) {
        this.jdbc = jdbc;
        this.scopeService = scopeService;
    }

    public List<Map<String, Object>> listCoops(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT coop_id, agent_id, partner_name, contact_name, contact_phone, remark, status, create_time ")
                .append("FROM biz_env_promo_coop WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendAgentScope(sql, args, s, true);
        sql.append(" ORDER BY coop_id DESC");
        return jdbc.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("coopId", rs.getLong("coop_id"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("partnerName", rs.getString("partner_name"));
            row.put("contactName", rs.getString("contact_name"));
            row.put("contactPhone", rs.getString("contact_phone"));
            row.put("remark", rs.getString("remark"));
            row.put("status", labelCoopStatus(rs.getString("status")));
            row.put("statusCode", rs.getString("status"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            return row;
        });
    }

    public void createCoop(PromoCoopCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        long agentId;
        if (r == '1') {
            if (req.getAgentId() == null) {
                throw new IllegalArgumentException("主端新增合作请指定 agentId");
            }
            agentId = req.getAgentId();
        } else if (r == '2' || r == '3') {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        } else {
            throw new IllegalArgumentException("无权限新增合作");
        }
        jdbc.update(
                "INSERT INTO biz_env_promo_coop (agent_id, partner_name, contact_name, contact_phone, remark, status, del_flag) "
                        + "VALUES (?,?,?,?,?,'0','0')",
                agentId,
                req.getPartnerName(),
                req.getContactName(),
                req.getContactPhone(),
                req.getRemark());
    }

    public List<Map<String, Object>> listWithdraws(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT withdraw_id, agent_id, applicant_openid, amount, status, audit_remark, create_time ")
                .append("FROM biz_env_promo_withdraw WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        char ur = s.getUserRole();
        if (ur == '1') {
            // 全部
        } else if (ur == '2') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND agent_id = ?");
                args.add(s.getAgentId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        sql.append(" ORDER BY withdraw_id DESC LIMIT 200");
        return jdbc.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("withdrawId", rs.getLong("withdraw_id"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("applicantOpenid", rs.getString("applicant_openid"));
            row.put("amount", rs.getBigDecimal("amount").doubleValue());
            row.put("status", labelWithdrawStatus(rs.getString("status")));
            row.put("statusCode", rs.getString("status"));
            row.put("auditRemark", rs.getString("audit_remark"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            return row;
        });
    }

    public void applyWithdraw(PromoWithdrawApplyRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅代理可申请提现");
        }
        if (s.getAgentId() == null) {
            throw new IllegalArgumentException("未绑定代理");
        }
        jdbc.update(
                "INSERT INTO biz_env_promo_withdraw (agent_id, applicant_openid, amount, status, del_flag) VALUES (?,?,?,'0','0')",
                s.getAgentId(),
                req.getOpenid(),
                BigDecimal.valueOf(req.getAmount()));
    }

    public void approveWithdraw(String openid, long withdrawId) {
        OpenidBizScope s = scopeService.resolve(openid);
        if (s.getUserRole() != '1' && s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可审核");
        }
        Long rowAgent = jdbc.query(
                "SELECT agent_id FROM biz_env_promo_withdraw WHERE withdraw_id = ? AND del_flag = '0'",
                rs -> rs.next() ? rs.getLong(1) : null,
                withdrawId);
        if (rowAgent == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        if (s.getUserRole() == '2' && (s.getAgentId() == null || s.getAgentId().longValue() != rowAgent)) {
            throw new IllegalArgumentException("无权限审核该笔提现");
        }
        int n = jdbc.update(
                "UPDATE biz_env_promo_withdraw SET status = '1', audit_remark = NULL WHERE withdraw_id = ? AND status = '0' AND del_flag = '0'",
                withdrawId);
        if (n == 0) {
            throw new IllegalArgumentException("仅待审核状态可通过");
        }
    }

    public void rejectWithdraw(String openid, long withdrawId, String remark) {
        OpenidBizScope s = scopeService.resolve(openid);
        if (s.getUserRole() != '1' && s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅主端或代理可审核");
        }
        Long rowAgent = jdbc.query(
                "SELECT agent_id FROM biz_env_promo_withdraw WHERE withdraw_id = ? AND del_flag = '0'",
                rs -> rs.next() ? rs.getLong(1) : null,
                withdrawId);
        if (rowAgent == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        if (s.getUserRole() == '2' && (s.getAgentId() == null || s.getAgentId().longValue() != rowAgent)) {
            throw new IllegalArgumentException("无权限审核该笔提现");
        }
        int n = jdbc.update(
                "UPDATE biz_env_promo_withdraw SET status = '2', audit_remark = ? WHERE withdraw_id = ? AND status = '0' AND del_flag = '0'",
                remark == null ? "" : remark,
                withdrawId);
        if (n == 0) {
            throw new IllegalArgumentException("仅待审核状态可驳回");
        }
    }

    public List<Map<String, Object>> listPrepaids(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT prepay_id, agent_id, merchant_id, title, amount, direction, ref_note, create_time ")
                .append("FROM biz_env_promo_prepay WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        char ur = s.getUserRole();
        if (ur == '1') {
            // all
        } else if (ur == '2' || ur == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (ur == '4') {
            if (s.getMerchantId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND merchant_id = ?");
                args.add(s.getMerchantId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        sql.append(" ORDER BY prepay_id DESC LIMIT 200");
        return jdbc.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("prepayId", rs.getLong("prepay_id"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
            row.put("title", rs.getString("title"));
            row.put("amount", rs.getBigDecimal("amount").doubleValue());
            row.put("direction", "1".equals(rs.getString("direction")) ? "入账" : "支出");
            row.put("directionCode", rs.getString("direction"));
            row.put("refNote", rs.getString("ref_note"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            return row;
        });
    }

    public void createPrepay(PromoPrepayCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        long agentId;
        if (r == '1') {
            if (req.getAgentId() == null) {
                throw new IllegalArgumentException("主端登记请指定 agentId");
            }
            agentId = req.getAgentId();
        } else if (r == '2') {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        } else {
            throw new IllegalArgumentException("无权限登记预付款");
        }
        String dir = req.getDirection();
        if (!"1".equals(dir) && !"2".equals(dir)) {
            throw new IllegalArgumentException("direction 须为 1 入账 或 2 支出");
        }
        Long mid = req.getMerchantId();
        if (mid != null) {
            Long mAgent = jdbc.query(
                    "SELECT agent_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    mid);
            if (mAgent == null || mAgent.longValue() != agentId) {
                throw new IllegalArgumentException("门店不属于该代理");
            }
        }
        jdbc.update(
                "INSERT INTO biz_env_promo_prepay (agent_id, merchant_id, title, amount, direction, ref_note, del_flag) VALUES (?,?,?,?,?,?,'0')",
                agentId,
                mid,
                req.getTitle(),
                BigDecimal.valueOf(req.getAmount()),
                dir,
                req.getRefNote());
    }

    private void appendAgentScope(StringBuilder sql, List<Object> args, OpenidBizScope s, boolean allowMainAll) {
        char ur = s.getUserRole();
        if (ur == '1' && allowMainAll) {
            return;
        }
        if ((ur == '2' || ur == '3') && s.getAgentId() != null) {
            sql.append(" AND agent_id = ?");
            args.add(s.getAgentId());
            return;
        }
        if (ur == '1' && !allowMainAll) {
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private static String labelCoopStatus(String code) {
        if ("0".equals(code)) {
            return "跟进中";
        }
        if ("1".equals(code)) {
            return "已签约";
        }
        return code == null ? "" : code;
    }

    private static String labelWithdrawStatus(String code) {
        if ("0".equals(code)) {
            return "待审核";
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
