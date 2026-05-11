package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 代理仓储：加油订单预扣 reserved、完工实扣 on_hand、取消回滚。
 * sku_code '1' = 油（桶）。
 */
@Service
public class AppBizStockService {

    public static final String SKU_OIL = "1";

    private final JdbcTemplate jdbcTemplate;

    public AppBizStockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureAgentRow(long agentId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_agent_stock WHERE agent_id = ? AND sku_code = ?",
                Integer.class,
                agentId,
                SKU_OIL);
        if (n == null || n == 0) {
            jdbcTemplate.update(
                    "INSERT INTO biz_env_agent_stock (agent_id, sku_code, qty_on_hand, qty_reserved) VALUES (?, ?, 0, 0)",
                    agentId,
                    SKU_OIL);
        }
    }

    /**
     * 订单确认后预扣：available = on_hand - reserved，不够则失败。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reserveForOilOrder(long agentId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(2, RoundingMode.HALF_UP);
        ensureAgentRow(agentId);
        BigDecimal[] avail = loadStock(agentId);
        BigDecimal onHand = avail[0];
        BigDecimal reserved = avail[1];
        BigDecimal available = onHand.subtract(reserved);
        if (available.compareTo(bucketQty) < 0) {
            throw new IllegalArgumentException("仓储可用库存不足，当前可用 "
                    + available.stripTrailingZeros().toPlainString() + " 桶");
        }
        int u = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET qty_reserved = qty_reserved + ? WHERE agent_id = ? AND sku_code = ? "
                        + "AND qty_on_hand - qty_reserved >= ?",
                bucketQty,
                agentId,
                SKU_OIL,
                bucketQty);
        if (u == 0) {
            throw new IllegalArgumentException("仓储预扣失败，请重试");
        }
        jdbcTemplate.update(
                "INSERT INTO biz_env_stock_flow (agent_id, sku_code, ref_type, ref_no, flow_kind, qty, remark) VALUES (?,?,?,?,?,?,?)",
                agentId,
                SKU_OIL,
                "ORDER",
                orderNo,
                "R",
                bucketQty,
                "订单确认预扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackReserveForOilOrder(long agentId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(2, RoundingMode.HALF_UP);
        int u = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET qty_reserved = qty_reserved - ? WHERE agent_id = ? AND sku_code = ? AND qty_reserved >= ?",
                bucketQty,
                agentId,
                SKU_OIL,
                bucketQty);
        if (u == 0) {
            throw new IllegalArgumentException("仓储预扣回滚失败");
        }
        jdbcTemplate.update(
                "INSERT INTO biz_env_stock_flow (agent_id, sku_code, ref_type, ref_no, flow_kind, qty, remark) VALUES (?,?,?,?,?,?,?)",
                agentId,
                SKU_OIL,
                "ORDER",
                orderNo,
                "B",
                bucketQty,
                "订单取消回滚预扣");
    }

    /**
     * 完工：从预扣与现货同时扣减。
     */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeOilDeduction(long agentId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(2, RoundingMode.HALF_UP);
        int u = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET qty_reserved = qty_reserved - ?, qty_on_hand = qty_on_hand - ? "
                        + "WHERE agent_id = ? AND sku_code = ? AND qty_reserved >= ? AND qty_on_hand >= ?",
                bucketQty,
                bucketQty,
                agentId,
                SKU_OIL,
                bucketQty,
                bucketQty);
        if (u == 0) {
            throw new IllegalArgumentException("仓储实扣失败，库存数据异常");
        }
        jdbcTemplate.update(
                "INSERT INTO biz_env_stock_flow (agent_id, sku_code, ref_type, ref_no, flow_kind, qty, remark) VALUES (?,?,?,?,?,?,?)",
                agentId,
                SKU_OIL,
                "ORDER",
                orderNo,
                "D",
                bucketQty,
                "订单完工实扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void inboundOil(long agentId, BigDecimal bucketQty, String remark) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入库数量须大于 0");
        }
        bucketQty = bucketQty.setScale(2, RoundingMode.HALF_UP);
        ensureAgentRow(agentId);
        jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET qty_on_hand = qty_on_hand + ? WHERE agent_id = ? AND sku_code = ?",
                bucketQty,
                agentId,
                SKU_OIL);
        jdbcTemplate.update(
                "INSERT INTO biz_env_stock_flow (agent_id, sku_code, ref_type, ref_no, flow_kind, qty, remark) VALUES (?,?,?,?,?,?,?)",
                agentId,
                SKU_OIL,
                "INBOUND",
                null,
                "I",
                bucketQty,
                remark == null ? "手工入库" : remark);
    }

    private BigDecimal[] loadStock(long agentId) {
        return jdbcTemplate.query(
                "SELECT qty_on_hand, qty_reserved FROM biz_env_agent_stock WHERE agent_id = ? AND sku_code = ?",
                rs -> {
                    if (!rs.next()) {
                        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
                    }
                    return new BigDecimal[]{rs.getBigDecimal("qty_on_hand"), rs.getBigDecimal("qty_reserved")};
                },
                agentId,
                SKU_OIL);
    }
}
