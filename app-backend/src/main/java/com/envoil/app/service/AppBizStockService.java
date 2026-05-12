package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 代理仓储：与《环保油数据库设计》biz_env_agent_stock / biz_env_agent_stock_flow 对齐。
 * stock_item_type = '1' 且 stock_item_code 为空 表示「桶装环保油」汇总行。
 */
@Service
public class AppBizStockService {

    public static final String STOCK_ITEM_TYPE_OIL = "1";

    private static final String WHERE_OIL_ROW =
            "agent_id = ? AND stock_item_type = ? AND (stock_item_code IS NULL OR stock_item_code = '') AND del_flag = '0'";

    /** 流水：1手工入库 2订单预扣 3订单完成扣减 4订单取消回滚 */
    private static final String FLOW_INBOUND = "1";
    private static final String FLOW_RESERVE = "2";
    private static final String FLOW_DEDUCT = "3";
    private static final String FLOW_ROLLBACK = "4";

    private final JdbcTemplate jdbcTemplate;

    public AppBizStockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureAgentRow(long agentId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_agent_stock WHERE " + WHERE_OIL_ROW,
                Integer.class,
                agentId,
                STOCK_ITEM_TYPE_OIL);
        if (n == null || n == 0) {
            jdbcTemplate.update(
                    "INSERT INTO biz_env_agent_stock (agent_id, stock_item_type, stock_item_code, stock_item_name, unit_name, "
                            + "total_qty, lock_qty, available_qty, status, del_flag) "
                            + "VALUES (?, ?, NULL, '环保油', '桶', 0, 0, 0, '0', '0')",
                    agentId,
                    STOCK_ITEM_TYPE_OIL);
        }
    }

    private long loadOilStockId(long agentId) {
        ensureAgentRow(agentId);
        Long id = jdbcTemplate.queryForObject(
                "SELECT stock_id FROM biz_env_agent_stock WHERE " + WHERE_OIL_ROW,
                Long.class,
                agentId,
                STOCK_ITEM_TYPE_OIL);
        if (id == null) {
            throw new IllegalStateException("未能定位代理库存行");
        }
        return id;
    }

    private void insertStockFlow(long stockId, long agentId, String changeType, String relatedNo, BigDecimal qty, String remark) {
        jdbcTemplate.update(
                "INSERT INTO biz_env_agent_stock_flow (stock_id, agent_id, change_type, related_no, change_qty, remark) "
                        + "VALUES (?,?,?,?,?,?)",
                stockId,
                agentId,
                changeType,
                relatedNo,
                qty,
                remark);
    }

    /**
     * 订单确认后预扣：可用 = total_qty - lock_qty。
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
                "UPDATE biz_env_agent_stock SET lock_qty = lock_qty + ?, available_qty = available_qty - ? "
                        + "WHERE " + WHERE_OIL_ROW + " AND available_qty >= ?",
                bucketQty,
                bucketQty,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                bucketQty);
        if (u == 0) {
            throw new IllegalArgumentException("仓储预扣失败，请重试");
        }
        insertStockFlow(loadOilStockId(agentId), agentId, FLOW_RESERVE, orderNo, bucketQty, "订单确认预扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackReserveForOilOrder(long agentId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(2, RoundingMode.HALF_UP);
        int u = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET lock_qty = lock_qty - ?, available_qty = available_qty + ? "
                        + "WHERE " + WHERE_OIL_ROW + " AND lock_qty >= ?",
                bucketQty,
                bucketQty,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                bucketQty);
        if (u == 0) {
            throw new IllegalArgumentException("仓储预扣回滚失败");
        }
        insertStockFlow(loadOilStockId(agentId), agentId, FLOW_ROLLBACK, orderNo, bucketQty, "订单取消回滚预扣");
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
                "UPDATE biz_env_agent_stock SET total_qty = total_qty - ?, lock_qty = lock_qty - ?, "
                        + "available_qty = total_qty - lock_qty "
                        + "WHERE " + WHERE_OIL_ROW + " AND lock_qty >= ? AND total_qty >= ?",
                bucketQty,
                bucketQty,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                bucketQty,
                bucketQty);
        if (u == 0) {
            throw new IllegalArgumentException("仓储实扣失败，库存数据异常");
        }
        insertStockFlow(loadOilStockId(agentId), agentId, FLOW_DEDUCT, orderNo, bucketQty, "订单完工实扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void inboundOil(long agentId, BigDecimal bucketQty, String remark) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入库数量须大于 0");
        }
        bucketQty = bucketQty.setScale(2, RoundingMode.HALF_UP);
        ensureAgentRow(agentId);
        jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET total_qty = total_qty + ?, available_qty = available_qty + ? "
                        + "WHERE " + WHERE_OIL_ROW,
                bucketQty,
                bucketQty,
                agentId,
                STOCK_ITEM_TYPE_OIL);
        insertStockFlow(
                loadOilStockId(agentId),
                agentId,
                FLOW_INBOUND,
                null,
                bucketQty,
                remark == null ? "手工入库" : remark);
    }

    private BigDecimal[] loadStock(long agentId) {
        return jdbcTemplate.query(
                "SELECT total_qty, lock_qty FROM biz_env_agent_stock WHERE " + WHERE_OIL_ROW,
                rs -> {
                    if (!rs.next()) {
                        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
                    }
                    return new BigDecimal[]{rs.getBigDecimal("total_qty"), rs.getBigDecimal("lock_qty")};
                },
                agentId,
                STOCK_ITEM_TYPE_OIL);
    }
}
