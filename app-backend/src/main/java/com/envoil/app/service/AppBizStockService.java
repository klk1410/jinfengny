package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.envoil.app.util.OilQuantityConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 代理仓储：biz_env_agent_stock / biz_env_agent_stock_flow。
 * stock_item_type = '1' 表示油品；stock_item_code = 油品类型 ID 字符串。
 * 油品在库数量以「吨」计量（与订单侧的桶当量通过油品密度、每桶升数换算一致）。
 */
@Service
public class AppBizStockService {

    public static final String STOCK_ITEM_TYPE_OIL = "1";

    private static final String FLOW_INBOUND = "1";
    private static final String FLOW_RESERVE = "2";
    private static final String FLOW_DEDUCT = "3";
    private static final String FLOW_ROLLBACK = "4";

    private final JdbcTemplate jdbcTemplate;

    public AppBizStockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String whereOilRow() {
        return "agent_id = ? AND stock_item_type = ? AND stock_item_code = ? AND del_flag = '0'";
    }

    public void ensureAgentRow(long agentId, long oilTypeId) {
        String code = String.valueOf(oilTypeId);
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_agent_stock WHERE " + whereOilRow(),
                Integer.class,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code);
        if (n == null || n == 0) {
            String typeName = jdbcTemplate.query(
                    "SELECT type_name FROM biz_env_oil_type WHERE oil_type_id = ? AND del_flag = '0'",
                    rs -> rs.next() ? rs.getString(1) : "油品",
                    oilTypeId);
            jdbcTemplate.update(
                    "INSERT INTO biz_env_agent_stock (agent_id, stock_item_type, stock_item_code, stock_item_name, unit_name, "
                            + "total_qty, lock_qty, available_qty, status, del_flag) "
                            + "VALUES (?, ?, ?, ?, '吨', 0, 0, 0, '0', '0')",
                    agentId,
                    STOCK_ITEM_TYPE_OIL,
                    code,
                    typeName == null ? "油品" : typeName);
        }
    }

    private long loadOilStockId(long agentId, long oilTypeId) {
        ensureAgentRow(agentId, oilTypeId);
        String code = String.valueOf(oilTypeId);
        Long id = jdbcTemplate.queryForObject(
                "SELECT stock_id FROM biz_env_agent_stock WHERE " + whereOilRow(),
                Long.class,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code);
        if (id == null) {
            throw new IllegalStateException("未能定位代理库存行");
        }
        return id;
    }

    private BigDecimal[] loadOilDensityLitersPerBucket(long oilTypeId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT density_kg_per_liter, liters_per_bucket FROM biz_env_oil_type WHERE oil_type_id = ? AND del_flag = '0'",
                oilTypeId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("油品类型不存在");
        }
        Map<String, Object> ot = rows.get(0);
        BigDecimal density = new BigDecimal(ot.get("density_kg_per_liter").toString());
        BigDecimal lp = new BigDecimal(ot.get("liters_per_bucket").toString());
        return new BigDecimal[]{density, lp};
    }

    /** 订单等场景传入的「桶」当量 → 仓储吨数。 */
    private BigDecimal bucketEquivToTons(long oilTypeId, BigDecimal bucketEquiv) {
        BigDecimal[] spec = loadOilDensityLitersPerBucket(oilTypeId);
        return OilQuantityConverter.bucketEquivalentToTons(bucketEquiv, spec[0], spec[1]);
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
     * 订单确认时预扣油品库存。
     *
     * @param bucketQty 订单侧「桶」当量（与计费一致），内部按油品密度与每桶升数换算为吨后扣减可用量
     */
    @Transactional(rollbackFor = Exception.class)
    public void reserveForOilOrder(long agentId, long oilTypeId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(4, RoundingMode.HALF_UP);
        BigDecimal tons = bucketEquivToTons(oilTypeId, bucketQty).setScale(6, RoundingMode.HALF_UP);
        ensureAgentRow(agentId, oilTypeId);
        BigDecimal[] avail = loadStock(agentId, oilTypeId);
        BigDecimal onHand = avail[0];
        BigDecimal reserved = avail[1];
        BigDecimal available = onHand.subtract(reserved);
        if (available.compareTo(tons) < 0) {
            throw new IllegalArgumentException("仓储可用库存不足，当前可用 "
                    + available.stripTrailingZeros().toPlainString() + " 吨");
        }
        String code = String.valueOf(oilTypeId);
        int rows = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET lock_qty = lock_qty + ?, available_qty = available_qty - ? "
                        + "WHERE " + whereOilRow() + " AND available_qty >= ?",
                tons,
                tons,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code,
                tons);
        if (rows == 0) {
            throw new IllegalArgumentException("仓储预扣失败，请重试");
        }
        insertStockFlow(loadOilStockId(agentId, oilTypeId), agentId, FLOW_RESERVE, orderNo, tons, "订单确认预扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackReserveForOilOrder(long agentId, long oilTypeId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(4, RoundingMode.HALF_UP);
        BigDecimal tons = bucketEquivToTons(oilTypeId, bucketQty).setScale(6, RoundingMode.HALF_UP);
        String code = String.valueOf(oilTypeId);
        int rows = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET lock_qty = lock_qty - ?, available_qty = available_qty + ? "
                        + "WHERE " + whereOilRow() + " AND lock_qty >= ?",
                tons,
                tons,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code,
                tons);
        if (rows == 0) {
            throw new IllegalArgumentException("仓储预扣回滚失败");
        }
        insertStockFlow(loadOilStockId(agentId, oilTypeId), agentId, FLOW_ROLLBACK, orderNo, tons, "订单取消回滚预扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalizeOilDeduction(long agentId, long oilTypeId, String orderNo, BigDecimal bucketQty) {
        if (bucketQty == null || bucketQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        bucketQty = bucketQty.setScale(4, RoundingMode.HALF_UP);
        BigDecimal tons = bucketEquivToTons(oilTypeId, bucketQty).setScale(6, RoundingMode.HALF_UP);
        String code = String.valueOf(oilTypeId);
        int rows = jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET total_qty = total_qty - ?, lock_qty = lock_qty - ?, "
                        + "available_qty = total_qty - lock_qty "
                        + "WHERE " + whereOilRow() + " AND lock_qty >= ? AND total_qty >= ?",
                tons,
                tons,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code,
                tons,
                tons);
        if (rows == 0) {
            throw new IllegalArgumentException("仓储实扣失败，库存数据异常");
        }
        insertStockFlow(loadOilStockId(agentId, oilTypeId), agentId, FLOW_DEDUCT, orderNo, tons, "订单完工实扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void inboundOil(long agentId, long oilTypeId, BigDecimal oilMassTons, String remark) {
        if (oilMassTons == null || oilMassTons.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入库数量须大于 0");
        }
        oilMassTons = oilMassTons.setScale(6, RoundingMode.HALF_UP);
        ensureAgentRow(agentId, oilTypeId);
        String code = String.valueOf(oilTypeId);
        jdbcTemplate.update(
                "UPDATE biz_env_agent_stock SET total_qty = total_qty + ?, available_qty = available_qty + ? "
                        + "WHERE " + whereOilRow(),
                oilMassTons,
                oilMassTons,
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code);
        insertStockFlow(
                loadOilStockId(agentId, oilTypeId),
                agentId,
                FLOW_INBOUND,
                null,
                oilMassTons,
                remark == null ? "手工入库" : remark);
    }

    private BigDecimal[] loadStock(long agentId, long oilTypeId) {
        String code = String.valueOf(oilTypeId);
        return jdbcTemplate.query(
                "SELECT total_qty, lock_qty FROM biz_env_agent_stock WHERE " + whereOilRow(),
                rs -> {
                    if (!rs.next()) {
                        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
                    }
                    return new BigDecimal[]{rs.getBigDecimal("total_qty"), rs.getBigDecimal("lock_qty")};
                },
                agentId,
                STOCK_ITEM_TYPE_OIL,
                code);
    }
}
