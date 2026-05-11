package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 账目流水：订单完工时记商家支出、代理收入（首版等额）。
 */
@Service
public class AppBizAccountService {

    private final JdbcTemplate jdbcTemplate;

    public AppBizAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordOrderCompleted(long agentId, long merchantId, String orderNo, BigDecimal amountPayable) {
        if (amountPayable == null || amountPayable.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        amountPayable = amountPayable.setScale(2, RoundingMode.HALF_UP);
        jdbcTemplate.update(
                "INSERT INTO biz_env_account_ledger (agent_id, merchant_id, ref_type, ref_no, title, amount, direction) VALUES (?,?,?,?,?,?,?)",
                agentId,
                merchantId,
                "ORDER_FINISH",
                orderNo,
                "订单应付（商家）",
                amountPayable,
                "2");
        jdbcTemplate.update(
                "INSERT INTO biz_env_account_ledger (agent_id, merchant_id, ref_type, ref_no, title, amount, direction) VALUES (?,?,?,?,?,?,?)",
                agentId,
                merchantId,
                "ORDER_FINISH",
                orderNo,
                "订单收入（代理）",
                amountPayable,
                "1");
    }
}
