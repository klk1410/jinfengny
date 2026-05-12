package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 账目流水：订单完工时记商家支出、代理收入（首版等额）。
 * 支付方式为赊销（pay_type=2）时同步累加门店 {@code biz_env_merchant.arrears_amount}，供店铺管理展示欠费。
 */
@Service
public class AppBizAccountService {

    private final JdbcTemplate jdbcTemplate;

    public AppBizAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordOrderCompleted(
            long agentId, long merchantId, String orderNo, BigDecimal amountPayable, String payType) {
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
        if ("2".equals(payType)) {
            int n = jdbcTemplate.update(
                    "UPDATE biz_env_merchant SET arrears_amount = COALESCE(arrears_amount, 0) + ? "
                            + "WHERE merchant_id = ? AND del_flag = '0'",
                    amountPayable,
                    merchantId);
            if (n == 0) {
                throw new IllegalArgumentException("更新门店欠费失败，门店不存在或已删除");
            }
        }
    }
}
