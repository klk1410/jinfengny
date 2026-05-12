package com.envoil.app.service;

import com.envoil.app.model.MerchantCreateRequest;
import com.envoil.app.util.OilQuantityConverter;
import com.envoil.app.model.MerchantUpdateRequest;
import com.envoil.app.model.OpenidBizScope;
import com.envoil.app.model.AccessoryCreateRequest;
import com.envoil.app.model.AccessoryTypeCreateRequest;
import com.envoil.app.model.AccessoryConsumeLine;
import com.envoil.app.model.AgentCreateRequest;
import com.envoil.app.model.SalesmanCreateRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AppBizDataService {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final AppOpenidBizScopeService scopeService;
    private final AppBizStockService stockService;

    public AppBizDataService(
            JdbcTemplate jdbcTemplate,
            AppOpenidBizScopeService scopeService,
            @Lazy AppBizStockService stockService) {
        this.jdbcTemplate = jdbcTemplate;
        this.scopeService = scopeService;
        this.stockService = stockService;
    }

    public List<Map<String, Object>> listMerchants(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT m.merchant_id, m.merchant_name, m.contact_name, m.contact_phone, m.city, ")
                .append("m.oil_unit_price, COALESCE(m.oil_type_id, 1) AS oil_type_id, ot.type_name AS oil_type_name, ")
                .append("COALESCE(ot.density_kg_per_liter, 0.85) AS density_kg_per_liter, ")
                .append("COALESCE(ot.liters_per_bucket, 200) AS liters_per_bucket, ")
                .append("m.arrears_amount, a.agent_name, sm.salesman_name, m.status ")
                .append("FROM biz_env_merchant m ")
                .append("JOIN biz_env_agent a ON a.agent_id = m.agent_id AND a.del_flag = '0' ")
                .append("LEFT JOIN biz_env_oil_type ot ON ot.oil_type_id = COALESCE(m.oil_type_id, 1) AND ot.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman sm ON sm.salesman_id = m.salesman_id AND sm.del_flag = '0' ")
                .append("WHERE m.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendMerchantScope(sql, args, s);
        sql.append(" ORDER BY m.merchant_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("merchantId", rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("contactName", rs.getString("contact_name"));
            row.put("contactPhone", rs.getString("contact_phone"));
            row.put("city", rs.getString("city"));
            row.put("oilUnitPrice", rs.getBigDecimal("oil_unit_price").doubleValue());
            row.put("oilTypeId", rs.getLong("oil_type_id"));
            row.put("oilTypeName", rs.getString("oil_type_name"));
            row.put("densityKgPerLiter", rs.getBigDecimal("density_kg_per_liter").doubleValue());
            row.put("litersPerBucket", rs.getBigDecimal("liters_per_bucket").doubleValue());
            row.put("arrearsAmount", rs.getBigDecimal("arrears_amount").doubleValue());
            row.put("agentName", rs.getString("agent_name"));
            row.put("salesmanName", rs.getString("salesman_name"));
            row.put("status", labelMerchantStatus(rs.getString("status")));
            row.put("statusCode", rs.getString("status"));
            return row;
        });
    }

    /**
     * 主端、代理可直接新增店铺；业务员与商家需走审核（见 {@link com.envoil.app.service.AppMerchantAuditService#submitCreateAudit}）。
     */
    public Map<String, Object> createMerchant(MerchantCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        long agentId;
        Long salesmanId = req.getSalesmanId();
        if (r == '1') {
            if (req.getAgentId() == null) {
                throw new IllegalArgumentException("主端创建店铺请指定 agentId");
            }
            agentId = req.getAgentId();
        } else if (r == '2') {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        } else {
            throw new IllegalArgumentException("无权限直接新增店铺，请提交审核");
        }
        return insertMerchantRow(agentId, salesmanId, req);
    }

    /**
     * 校验新建门店请求（归属、经纬度、图片大小等），不落库。
     */
    public void assertCanInsertMerchantRow(long agentId, Long salesmanId, MerchantCreateRequest req) {
        if (salesmanId != null) {
            ensureSalesmanBelongsToAgent(salesmanId, agentId);
        }
        if (req.getLinkedMerchantId() != null) {
            ensureLinkedMerchant(req.getLinkedMerchantId(), agentId);
        }
        if (req.getLongitude() == null || req.getLatitude() == null) {
            throw new IllegalArgumentException("请填写经纬度");
        }
        String img = req.getStoreImageUrl();
        if (img != null && img.length() > 8000) {
            throw new IllegalArgumentException("店铺图片数据过大，请压缩或使用外链地址");
        }
        String contract = req.getContractImageUrl();
        if (contract == null || contract.trim().isEmpty()) {
            throw new IllegalArgumentException("请上传合同图片");
        }
        if (contract.length() > 8000) {
            throw new IllegalArgumentException("合同图片数据过大，请压缩或使用外链地址");
        }
        String locInfo = req.getMapLocationInfo();
        if (locInfo == null || locInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("请完成地图定位");
        }
        if (locInfo.length() > 500) {
            throw new IllegalArgumentException("地图定位说明过长");
        }
    }

    /**
     * 写入新门店（主端/代理直建或审核通过时调用；不做角色校验）。
     */
    public Map<String, Object> insertMerchantRow(long agentId, Long salesmanId, MerchantCreateRequest req) {
        assertCanInsertMerchantRow(agentId, salesmanId, req);
        final Long insertSalesmanId = salesmanId;
        BigDecimal oil = BigDecimal.valueOf(req.getOilUnitPrice() == null ? 0 : req.getOilUnitPrice());
        BigDecimal comm = BigDecimal.valueOf(req.getMerchantCommission() == null ? 0 : req.getMerchantCommission());
        BigDecimal lon = BigDecimal.valueOf(req.getLongitude());
        BigDecimal lat = BigDecimal.valueOf(req.getLatitude());
        String img = req.getStoreImageUrl();
        long oilTypeIdIns = req.getOilTypeId() == null ? 1L : req.getOilTypeId().longValue();
        String contractImg = req.getContractImageUrl();
        String mapLoc = req.getMapLocationInfo();
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_merchant (agent_id, salesman_id, industry_type, merchant_name, contact_name, contact_phone, "
                            + "longitude, latitude, province, city, district, address_detail, oil_unit_price, oil_type_id, merchant_commission, "
                            + "remark, store_image_url, contract_image_url, map_location_info, linked_merchant_id, status, del_flag) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, '0','0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, agentId);
            if (insertSalesmanId == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, insertSalesmanId);
            }
            ps.setString(3, req.getIndustryType());
            ps.setString(4, req.getMerchantName());
            ps.setString(5, req.getContactName());
            ps.setString(6, req.getContactPhone());
            ps.setBigDecimal(7, lon);
            ps.setBigDecimal(8, lat);
            ps.setString(9, req.getProvince());
            ps.setString(10, req.getCity());
            ps.setString(11, req.getDistrict());
            ps.setString(12, req.getAddressDetail());
            ps.setBigDecimal(13, oil);
            ps.setLong(14, oilTypeIdIns);
            ps.setBigDecimal(15, comm);
            ps.setString(16, req.getRemark());
            ps.setString(17, img);
            ps.setString(18, contractImg);
            ps.setString(19, mapLoc);
            if (req.getLinkedMerchantId() == null) {
                ps.setObject(20, null);
            } else {
                ps.setLong(20, req.getLinkedMerchantId());
            }
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("未能获取新店铺 ID");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("merchantId", key.longValue());
        return out;
    }

    /**
     * 店铺详情（主端/代理/业务员/商家按数据范围可见）。
     */
    public Map<String, Object> getMerchantDetail(String openid, long merchantId) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT m.merchant_id, m.agent_id, m.salesman_id, m.industry_type, m.merchant_name, m.contact_name, ")
                .append("m.contact_phone, m.longitude, m.latitude, m.province, m.city, m.district, m.address_detail, ")
                .append("m.oil_unit_price, COALESCE(m.oil_type_id, 1) AS oil_type_id, ot.type_name AS oil_type_name, ")
                .append("m.merchant_commission, m.arrears_amount, m.device_count, m.status, ")
                .append("m.remark, m.store_image_url, m.contract_image_url, m.map_location_info, m.linked_merchant_id, a.agent_name, sm.salesman_name ")
                .append("FROM biz_env_merchant m ")
                .append("JOIN biz_env_agent a ON a.agent_id = m.agent_id AND a.del_flag = '0' ")
                .append("LEFT JOIN biz_env_oil_type ot ON ot.oil_type_id = COALESCE(m.oil_type_id, 1) AND ot.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman sm ON sm.salesman_id = m.salesman_id AND sm.del_flag = '0' ")
                .append("WHERE m.merchant_id = ? AND m.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        appendMerchantScope(sql, args, s);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                sql.toString(),
                args.toArray(),
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("merchantId", rs.getLong("merchant_id"));
                    row.put("agentId", rs.getLong("agent_id"));
                    row.put("salesmanId", rs.getObject("salesman_id") == null ? null : rs.getLong("salesman_id"));
                    row.put("industryType", rs.getString("industry_type"));
                    row.put("merchantName", rs.getString("merchant_name"));
                    row.put("contactName", rs.getString("contact_name"));
                    row.put("contactPhone", rs.getString("contact_phone"));
                    row.put("longitude", rs.getBigDecimal("longitude") == null ? null : rs.getBigDecimal("longitude").doubleValue());
                    row.put("latitude", rs.getBigDecimal("latitude") == null ? null : rs.getBigDecimal("latitude").doubleValue());
                    row.put("province", rs.getString("province"));
                    row.put("city", rs.getString("city"));
                    row.put("district", rs.getString("district"));
                    row.put("addressDetail", rs.getString("address_detail"));
                    row.put("oilUnitPrice", rs.getBigDecimal("oil_unit_price") == null ? 0 : rs.getBigDecimal("oil_unit_price").doubleValue());
                    row.put("oilTypeId", rs.getLong("oil_type_id"));
                    row.put("oilTypeName", rs.getString("oil_type_name"));
                    row.put("merchantCommission", rs.getBigDecimal("merchant_commission") == null
                            ? 0
                            : rs.getBigDecimal("merchant_commission").doubleValue());
                    row.put("arrearsAmount", rs.getBigDecimal("arrears_amount") == null ? 0 : rs.getBigDecimal("arrears_amount").doubleValue());
                    row.put("deviceCount", rs.getInt("device_count"));
                    row.put("status", labelMerchantStatus(rs.getString("status")));
                    row.put("statusCode", rs.getString("status"));
                    row.put("remark", rs.getString("remark"));
                    row.put("storeImageUrl", rs.getString("store_image_url"));
                    row.put("contractImageUrl", rs.getString("contract_image_url"));
                    row.put("mapLocationInfo", rs.getString("map_location_info"));
                    row.put("linkedMerchantId", rs.getObject("linked_merchant_id") == null ? null : rs.getLong("linked_merchant_id"));
                    row.put("agentName", rs.getString("agent_name"));
                    row.put("salesmanName", rs.getString("salesman_name"));
                    return row;
                });
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("店铺不存在或无权查看");
        }
        Map<String, Object> row = rows.get(0);
        char r = s.getUserRole();
        row.put("canDirectEdit", r == '1' || r == '2');
        row.put("canSubmitAudit", r == '3');
        if (r == '3' && s.getSalesmanId() != null) {
            Integer pend = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_env_merchant_audit WHERE merchant_id = ? AND status = '0' AND del_flag = '0' "
                            + "AND submitter_salesman_id = ?",
                    Integer.class,
                    merchantId,
                    s.getSalesmanId());
            row.put("hasPendingMyAudit", pend != null && pend > 0);
        } else {
            row.put("hasPendingMyAudit", false);
        }
        return row;
    }

    /**
     * 主端、代理可直接修改店铺资料（不经审核）。
     */
    public void updateMerchant(MerchantUpdateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        if (r != '1' && r != '2') {
            throw new IllegalArgumentException("仅主端或代理可直接修改店铺");
        }
        long merchantId = req.getMerchantId();
        Map<String, Object> cur = getMerchantDetail(req.getOpenid(), merchantId);
        long agentId = ((Number) cur.get("agentId")).longValue();
        if (r == '2' && (s.getAgentId() == null || s.getAgentId().longValue() != agentId)) {
            throw new IllegalArgumentException("无权修改该店铺");
        }
        applyMerchantUpdate(merchantId, agentId, req);
    }

    /**
     * 审核通过后写入店铺（由审核服务调用）。
     */
    public void applyMerchantUpdate(long merchantId, long agentId, MerchantUpdateRequest req) {
        if (req.getLongitude() == null || req.getLatitude() == null) {
            throw new IllegalArgumentException("请填写经纬度");
        }
        String img = req.getStoreImageUrl();
        if (img != null && img.length() > 8000) {
            throw new IllegalArgumentException("店铺图片数据过大，请压缩或使用外链地址");
        }
        String contractImg = req.getContractImageUrl();
        if (contractImg != null && contractImg.length() > 8000) {
            throw new IllegalArgumentException("合同图片数据过大，请压缩或使用外链地址");
        }
        String mapLoc = req.getMapLocationInfo();
        if (mapLoc != null && mapLoc.length() > 500) {
            throw new IllegalArgumentException("地图定位说明过长");
        }
        Long salesmanId = req.getSalesmanId();
        if (salesmanId != null) {
            ensureSalesmanBelongsToAgent(salesmanId, agentId);
        }
        if (req.getLinkedMerchantId() != null) {
            ensureLinkedMerchant(req.getLinkedMerchantId(), agentId);
            if (req.getLinkedMerchantId().longValue() == merchantId) {
                throw new IllegalArgumentException("关联商家不能为自身");
            }
        }
        BigDecimal oil = BigDecimal.valueOf(req.getOilUnitPrice() == null ? 0 : req.getOilUnitPrice());
        BigDecimal comm = BigDecimal.valueOf(req.getMerchantCommission() == null ? 0 : req.getMerchantCommission());
        BigDecimal lon = BigDecimal.valueOf(req.getLongitude());
        BigDecimal lat = BigDecimal.valueOf(req.getLatitude());
        Long oilTypeIdUp = req.getOilTypeId() == null ? 1L : req.getOilTypeId();
        int n = jdbcTemplate.update(
                "UPDATE biz_env_merchant SET industry_type = ?, merchant_name = ?, contact_name = ?, contact_phone = ?, "
                        + "longitude = ?, latitude = ?, province = ?, city = ?, district = ?, address_detail = ?, "
                        + "oil_unit_price = ?, oil_type_id = ?, merchant_commission = ?, salesman_id = ?, linked_merchant_id = ?, remark = ?, "
                        + "store_image_url = ?, contract_image_url = COALESCE(?, contract_image_url), map_location_info = COALESCE(?, map_location_info) "
                        + "WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                req.getIndustryType(),
                req.getMerchantName(),
                req.getContactName(),
                req.getContactPhone(),
                lon,
                lat,
                req.getProvince(),
                req.getCity(),
                req.getDistrict(),
                req.getAddressDetail(),
                oil,
                oilTypeIdUp,
                comm,
                salesmanId,
                req.getLinkedMerchantId(),
                req.getRemark(),
                img,
                contractImg,
                mapLoc,
                merchantId,
                agentId);
        if (n == 0) {
            throw new IllegalStateException("店铺更新失败，请刷新后重试");
        }
    }

    private void ensureSalesmanBelongsToAgent(long salesmanId, long agentId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_salesman WHERE salesman_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                salesmanId,
                agentId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("业务员不属于当前代理");
        }
    }

    private void ensureLinkedMerchant(long linkedMerchantId, long agentId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_merchant WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                Integer.class,
                linkedMerchantId,
                agentId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("关联商家不存在或不属于当前代理");
        }
    }

    public List<Map<String, Object>> listAgents(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT agent_id, agent_name, contact_name, contact_phone, province, city, status ")
                .append("FROM biz_env_agent WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (s.getUserRole() == '2' || s.getUserRole() == '3' || s.getUserRole() == '4') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND agent_id = ?");
                args.add(s.getAgentId());
            }
        }
        sql.append(" ORDER BY agent_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agentId", rs.getLong("agent_id"));
            row.put("agentName", rs.getString("agent_name"));
            row.put("contactName", rs.getString("contact_name"));
            row.put("contactPhone", rs.getString("contact_phone"));
            row.put("province", rs.getString("province"));
            row.put("city", rs.getString("city"));
            row.put("status", labelAgentStatus(rs.getString("status")));
            row.put("statusCode", rs.getString("status"));
            return row;
        });
    }

    public List<Map<String, Object>> listSalesmen(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT salesman_id, salesman_name, phone, agent_id, status ")
                .append("FROM biz_env_salesman WHERE del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (s.getUserRole() == '2' || s.getUserRole() == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND agent_id = ?");
                args.add(s.getAgentId());
            }
            if (s.getUserRole() == '3' && s.getSalesmanId() != null) {
                sql.append(" AND salesman_id = ?");
                args.add(s.getSalesmanId());
            }
        } else if (s.getUserRole() == '4') {
            Long sid = null;
            if (s.getMerchantId() != null) {
                sid = jdbcTemplate.query(
                        "SELECT salesman_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            long v = rs.getLong("salesman_id");
                            return rs.wasNull() ? null : v;
                        },
                        s.getMerchantId());
            }
            if (sid == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND salesman_id = ?");
                args.add(sid);
            }
        }
        sql.append(" ORDER BY salesman_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("salesmanId", rs.getLong("salesman_id"));
            row.put("salesmanName", rs.getString("salesman_name"));
            row.put("phone", rs.getString("phone"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("status", labelSalesmanStatus(rs.getString("status")));
            row.put("statusCode", rs.getString("status"));
            return row;
        });
    }

    /**
     * 主端/代理等在有权限时查看某业务员的小程序账户概要（profile + 共享账号列表）。
     * 通过 env_openid_biz_scope 解析该业务员绑定的 openid；优先排除「仅作为被共享方」的 openid。
     */
    public Map<String, Object> getSalesmanPortalAccount(String viewerOpenid, long salesmanId) {
        OpenidBizScope viewer = scopeService.resolve(viewerOpenid);
        assertCanViewSalesmanPortal(viewer, salesmanId);

        Map<String, Object> meta = jdbcTemplate.query(
                "SELECT salesman_id, salesman_name, phone, agent_id, status FROM biz_env_salesman WHERE salesman_id = ? AND del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("salesmanId", rs.getLong("salesman_id"));
                    m.put("salesmanName", rs.getString("salesman_name"));
                    m.put("phone", rs.getString("phone"));
                    m.put("agentId", rs.getLong("agent_id"));
                    m.put("status", labelSalesmanStatus(rs.getString("status")));
                    m.put("statusCode", rs.getString("status"));
                    return m;
                },
                salesmanId);
        if (meta == null) {
            throw new IllegalArgumentException("业务员不存在");
        }

        String portalOpenid = resolveOwnerOpenidForSalesman(salesmanId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("salesman", meta);
        out.put("portalOpenid", portalOpenid);
        if (portalOpenid != null) {
            out.put("profile", accountProfile(portalOpenid));
            out.put("shares", listAccountShares(portalOpenid));
        } else {
            out.put("profile", null);
            out.put("shares", Collections.emptyList());
            out.put(
                    "accountNote",
                    "该业务员尚未绑定小程序登录，暂无门户账户与共享账号数据（或仅在 env_openid_biz_scope 中未登记）。");
        }
        return out;
    }

    private void assertCanViewSalesmanPortal(OpenidBizScope s, long salesmanId) {
        char r = s.getUserRole();
        Long smAgentId = jdbcTemplate.query(
                "SELECT agent_id FROM biz_env_salesman WHERE salesman_id = ? AND del_flag = '0'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return rs.getLong("agent_id");
                },
                salesmanId);
        if (smAgentId == null) {
            throw new IllegalArgumentException("业务员不存在");
        }
        if (r == '1') {
            return;
        }
        if (r == '2') {
            if (s.getAgentId() == null || !Objects.equals(s.getAgentId(), smAgentId)) {
                throw new IllegalArgumentException("无权查看该业务员");
            }
            return;
        }
        if (r == '3') {
            if (s.getAgentId() == null
                    || !Objects.equals(s.getAgentId(), smAgentId)
                    || !Objects.equals(s.getSalesmanId(), salesmanId)) {
                throw new IllegalArgumentException("无权查看该业务员");
            }
            return;
        }
        if (r == '4') {
            Long linkedSid = null;
            if (s.getMerchantId() != null) {
                linkedSid = jdbcTemplate.query(
                        "SELECT salesman_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            long v = rs.getLong("salesman_id");
                            return rs.wasNull() ? null : v;
                        },
                        s.getMerchantId());
            }
            if (linkedSid == null || linkedSid != salesmanId) {
                throw new IllegalArgumentException("无权查看该业务员");
            }
            return;
        }
        throw new IllegalArgumentException("无权查看该业务员");
    }

    /**
     * 解析用于门户资料、共享列表的主 openid：优先排除仅作为 shared_openid 出现的账号。
     */
    private String resolveOwnerOpenidForSalesman(long salesmanId) {
        List<String> primary = jdbcTemplate.query(
                "SELECT o.openid FROM env_openid_biz_scope o "
                        + "WHERE o.salesman_id = ? AND o.user_role = '3' "
                        + "AND o.openid NOT IN ("
                        + "SELECT s.shared_openid FROM biz_env_account_share s WHERE s.del_flag = '0' AND s.shared_openid IS NOT NULL"
                        + ") ORDER BY o.openid ASC LIMIT 1",
                (rs, i) -> rs.getString("openid"),
                salesmanId);
        if (!primary.isEmpty()) {
            return primary.get(0);
        }
        List<String> any = jdbcTemplate.query(
                "SELECT openid FROM env_openid_biz_scope WHERE salesman_id = ? AND user_role = '3' ORDER BY openid ASC LIMIT 1",
                (rs, i) -> rs.getString("openid"),
                salesmanId);
        return any.isEmpty() ? null : any.get(0);
    }

    /**
     * 主端新增代理。
     */
    public Map<String, Object> createAgent(AgentCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '1') {
            throw new IllegalArgumentException("仅主端可新增代理");
        }
        String name = req.getAgentName() == null ? "" : req.getAgentName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("代理名称不能为空");
        }
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_agent (agent_name, contact_name, contact_phone, province, city, district, address_detail, status, del_flag) "
                            + "VALUES (?,?,?,?,?,?,?,'0','0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, emptyToNull(req.getContactName()));
            ps.setString(3, emptyToNull(req.getContactPhone()));
            ps.setString(4, emptyToNull(req.getProvince()));
            ps.setString(5, emptyToNull(req.getCity()));
            ps.setString(6, emptyToNull(req.getDistrict()));
            ps.setString(7, emptyToNull(req.getAddressDetail()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("未能获取新代理 ID");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agentId", key.longValue());
        return out;
    }

    /**
     * 代理新增本名下业务员。
     */
    public Map<String, Object> createSalesman(SalesmanCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        if (s.getUserRole() != '2') {
            throw new IllegalArgumentException("仅代理可新增业务员");
        }
        if (s.getAgentId() == null) {
            throw new IllegalArgumentException("未绑定代理");
        }
        String nm = req.getSalesmanName() == null ? "" : req.getSalesmanName().trim();
        if (nm.isEmpty()) {
            throw new IllegalArgumentException("业务员姓名不能为空");
        }
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_salesman (salesman_name, phone, agent_id, status, del_flag) VALUES (?,?,?,'0','0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nm);
            ps.setString(2, emptyToNull(req.getPhone()));
            ps.setLong(3, s.getAgentId());
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("未能获取新业务员 ID");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("salesmanId", key.longValue());
        return out;
    }

    private static String emptyToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public List<Map<String, Object>> listDevices(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT d.device_id, d.device_no, d.device_type, d.device_status, d.merchant_id, ")
                .append("m.merchant_name, d.agent_id, ag.agent_name ")
                .append("FROM biz_env_device d ")
                .append("LEFT JOIN biz_env_merchant m ON m.merchant_id = d.merchant_id AND m.del_flag = '0' ")
                .append("LEFT JOIN biz_env_agent ag ON ag.agent_id = d.agent_id AND ag.del_flag = '0' ")
                .append("WHERE d.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        if (s.getUserRole() == '1') {
            // no filter
        } else if (s.getUserRole() == '2' || s.getUserRole() == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND d.agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (s.getUserRole() == '4') {
            if (s.getMerchantId() == null || s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND d.agent_id = ? AND (d.merchant_id = ? OR d.merchant_id IS NULL)");
                args.add(s.getAgentId());
                args.add(s.getMerchantId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
        sql.append(" ORDER BY d.device_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deviceId", rs.getLong("device_id"));
            row.put("deviceNo", rs.getString("device_no"));
            row.put("deviceType", labelDeviceType(rs.getString("device_type")));
            row.put("deviceStatusCode", rs.getString("device_status"));
            row.put("deviceStatus", labelDeviceStatus(rs.getString("device_status")));
            row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("agentName", rs.getString("agent_name"));
            return row;
        });
    }

    /**
     * 配件：按种类汇总库存（代理维度；主端全量；商家见本代理下与本店相关记录）。
     */
    public List<Map<String, Object>> listAccessorySummaryByType(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT a.type_id, t.type_name, t.sort_order, ")
                .append("SUM(a.qty) AS qty_total, SUM(a.inbound_cost) AS cost_total, COUNT(*) AS line_count ")
                .append("FROM biz_env_accessory a ")
                .append("JOIN biz_env_accessory_type t ON t.type_id = a.type_id AND t.del_flag = '0' ")
                .append("WHERE a.del_flag = '0' ");
        List<Object> args = new ArrayList<>();
        appendAccessoryScope(sql, args, s);
        sql.append(" GROUP BY a.type_id, t.type_name, t.sort_order ORDER BY t.sort_order, a.type_id");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("typeId", rs.getLong("type_id"));
            row.put("typeName", rs.getString("type_name"));
            row.put("qtyTotal", rs.getBigDecimal("qty_total").doubleValue());
            row.put("costTotal", rs.getBigDecimal("cost_total").doubleValue());
            row.put("lineCount", rs.getLong("line_count"));
            return row;
        });
    }

    /**
     * 某种类下的入库明细。
     */
    public List<Map<String, Object>> listAccessoryLinesByType(String openid, long typeId) {
        OpenidBizScope s = scopeService.resolve(openid);
        StringBuilder sql = new StringBuilder()
                .append("SELECT a.acc_id, a.agent_id, a.merchant_id, m.merchant_name, t.type_name, ")
                .append("a.acc_code, a.qty, a.inbound_cost, a.remark, a.create_time, a.operator_kind, a.operator_id, ")
                .append("CASE WHEN a.operator_kind = '3' THEN sm.salesman_name ELSE ag.agent_name END AS operator_label ")
                .append("FROM biz_env_accessory a ")
                .append("JOIN biz_env_accessory_type t ON t.type_id = a.type_id AND t.del_flag = '0' ")
                .append("LEFT JOIN biz_env_merchant m ON m.merchant_id = a.merchant_id AND m.del_flag = '0' ")
                .append("LEFT JOIN biz_env_agent ag ON ag.agent_id = a.agent_id AND ag.del_flag = '0' ")
                .append("LEFT JOIN biz_env_salesman sm ON a.operator_kind = '3' AND sm.salesman_id = a.operator_id AND sm.del_flag = '0' ")
                .append("WHERE a.del_flag = '0' AND a.type_id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(typeId);
        appendAccessoryScope(sql, args, s);
        sql.append(" ORDER BY a.acc_id DESC");
        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("accId", rs.getLong("acc_id"));
            row.put("agentId", rs.getLong("agent_id"));
            row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("typeName", rs.getString("type_name"));
            row.put("accCode", rs.getString("acc_code"));
            row.put("qty", rs.getBigDecimal("qty").doubleValue());
            row.put("inboundCost", rs.getBigDecimal("inbound_cost").doubleValue());
            row.put("remark", rs.getString("remark"));
            row.put("createTime", formatTs(rs.getTimestamp("create_time")));
            row.put("operatorLabel", rs.getString("operator_label"));
            return row;
        });
    }

    public List<Map<String, Object>> listAccessoryTypes() {
        return jdbcTemplate.query(
                "SELECT type_id, type_name, sort_order FROM biz_env_accessory_type WHERE del_flag = '0' ORDER BY sort_order, type_id",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("typeId", rs.getLong("type_id"));
                    row.put("typeName", rs.getString("type_name"));
                    row.put("sortOrder", rs.getInt("sort_order"));
                    return row;
                });
    }

    /**
     * 新增配件种类（与入库登记相同：仅代理或业务员可操作）。
     */
    public Map<String, Object> createAccessoryType(AccessoryTypeCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        if (r != '2' && r != '3') {
            throw new IllegalArgumentException("仅代理或业务员可新增配件种类");
        }
        if (s.getAgentId() == null) {
            throw new IllegalArgumentException("未绑定代理");
        }
        String name = req.getTypeName() == null ? "" : req.getTypeName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("种类名称不能为空");
        }
        Integer dup = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_accessory_type WHERE type_name = ? AND del_flag = '0'",
                Integer.class,
                name);
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException("已存在同名的配件种类");
        }
        int sort = req.getSortOrder() == null ? 0 : req.getSortOrder();
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO biz_env_accessory_type (type_name, sort_order, del_flag) VALUES (?,?, '0')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, sort);
            return ps;
        }, kh);
        long id = Objects.requireNonNull(kh.getKey()).longValue();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("typeId", id);
        out.put("typeName", name);
        out.put("sortOrder", sort);
        return out;
    }

    /**
     * 入库操作人员下拉：当前代理及其下属业务员（代理账号、业务员账号使用）。
     */
    public List<Map<String, Object>> listAccessoryInboundOperators(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        if ((r != '2' && r != '3') || s.getAgentId() == null) {
            return new ArrayList<>();
        }
        long agentId = s.getAgentId();
        List<Map<String, Object>> out = new ArrayList<>();
        List<String> agentNames = jdbcTemplate.queryForList(
                "SELECT agent_name FROM biz_env_agent WHERE agent_id = ? AND del_flag = '0'",
                String.class,
                agentId);
        if (!agentNames.isEmpty()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("operatorKey", "AGENT");
            row.put("label", "代理：" + agentNames.get(0));
            out.add(row);
        }
        List<Map<String, Object>> sms = jdbcTemplate.query(
                "SELECT salesman_id, salesman_name FROM biz_env_salesman WHERE agent_id = ? AND del_flag = '0' ORDER BY salesman_id",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    long sid = rs.getLong("salesman_id");
                    row.put("operatorKey", "SALESMAN:" + sid);
                    row.put("label", "业务员：" + rs.getString("salesman_name"));
                    return row;
                },
                agentId);
        out.addAll(sms);
        return out;
    }

    public void createAccessory(AccessoryCreateRequest req) {
        OpenidBizScope s = scopeService.resolve(req.getOpenid());
        char r = s.getUserRole();
        if (r != '2' && r != '3') {
            throw new IllegalArgumentException("仅代理或业务员可登记配件入库");
        }
        if (s.getAgentId() == null) {
            throw new IllegalArgumentException("未绑定代理");
        }
        long agentId = s.getAgentId();
        Long mid = req.getMerchantId();
        if (mid != null) {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_env_merchant WHERE merchant_id = ? AND agent_id = ? AND del_flag = '0'",
                    Integer.class,
                    mid,
                    agentId);
            if (n == null || n == 0) {
                throw new IllegalArgumentException("门店不属于该代理");
            }
        }
        Integer typeOk = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_accessory_type WHERE type_id = ? AND del_flag = '0'",
                Integer.class,
                req.getTypeId());
        if (typeOk == null || typeOk == 0) {
            throw new IllegalArgumentException("配件种类无效");
        }
        String typeName = jdbcTemplate.queryForObject(
                "SELECT type_name FROM biz_env_accessory_type WHERE type_id = ? AND del_flag = '0'",
                String.class,
                req.getTypeId());
        BigDecimal qty = BigDecimal.valueOf(req.getQty() == null ? 0 : req.getQty());
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("数量须大于0");
        }
        BigDecimal inboundCost = BigDecimal.valueOf(req.getInboundCost() == null ? 0 : req.getInboundCost())
                .setScale(2, RoundingMode.HALF_UP);
        if (inboundCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("入库成本不能小于0");
        }
        BigDecimal unitPrice = qty.compareTo(BigDecimal.ZERO) > 0
                ? inboundCost.divide(qty, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        char operatorKind;
        Long operatorId = null;
        String opKey = req.getOperatorKey() == null ? "" : req.getOperatorKey().trim();
        if ("AGENT".equalsIgnoreCase(opKey)) {
            operatorKind = '2';
        } else if (opKey.regionMatches(true, 0, "SALESMAN:", 0, 9)) {
            long sid = Long.parseLong(opKey.substring(9).trim());
            ensureSalesmanBelongsToAgent(sid, agentId);
            operatorKind = '3';
            operatorId = sid;
        } else {
            throw new IllegalArgumentException("入库操作人员无效");
        }
        String accCode = req.getAccCode() == null ? null : req.getAccCode().trim();
        if (accCode != null && accCode.isEmpty()) {
            accCode = null;
        }
        jdbcTemplate.update(
                "INSERT INTO biz_env_accessory (agent_id, merchant_id, type_id, acc_name, acc_code, qty, unit_price, "
                        + "inbound_cost, operator_kind, operator_id, remark, del_flag) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,'0')",
                agentId,
                mid,
                req.getTypeId(),
                typeName,
                accCode,
                qty,
                unitPrice,
                inboundCost,
                String.valueOf(operatorKind),
                operatorId,
                req.getRemark());
    }

    /**
     * 工单结单扣减代理配件库存：按种类汇总校验可用量后，写入负数量流水（与入库同表，汇总 SUM(qty) 即库存）。
     */
    public void consumeAccessoriesForWorkOrder(
            long agentId,
            String workOrderNo,
            char operatorKind,
            Long operatorSalesmanId,
            List<AccessoryConsumeLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> merged = new LinkedHashMap<>();
        for (AccessoryConsumeLine line : lines) {
            if (line == null || line.getTypeId() == null || line.getQty() == null) {
                continue;
            }
            BigDecimal q = BigDecimal.valueOf(line.getQty()).setScale(2, RoundingMode.HALF_UP);
            if (q.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            merged.merge(line.getTypeId(), q, BigDecimal::add);
        }
        if (merged.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, BigDecimal> e : merged.entrySet()) {
            long typeId = e.getKey();
            BigDecimal need = e.getValue();
            Integer typeOk = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_env_accessory_type WHERE type_id = ? AND del_flag = '0'",
                    Integer.class,
                    typeId);
            if (typeOk == null || typeOk == 0) {
                throw new IllegalArgumentException("配件种类无效: " + typeId);
            }
            String typeName = jdbcTemplate.queryForObject(
                    "SELECT type_name FROM biz_env_accessory_type WHERE type_id = ? AND del_flag = '0'",
                    String.class,
                    typeId);
            BigDecimal bal = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(qty),0) FROM biz_env_accessory WHERE agent_id = ? AND type_id = ? AND del_flag = '0'",
                    BigDecimal.class,
                    agentId,
                    typeId);
            if (bal == null) {
                bal = BigDecimal.ZERO;
            }
            bal = bal.setScale(2, RoundingMode.HALF_UP);
            if (bal.compareTo(need) < 0) {
                throw new IllegalArgumentException("配件「" + typeName + "」库存不足（当前 " + bal + "，需扣 " + need + "）");
            }
            BigDecimal negQty = need.negate();
            String remark = "工单消耗:" + workOrderNo;
            Long opId = operatorKind == '3' ? operatorSalesmanId : null;
            jdbcTemplate.update(
                    "INSERT INTO biz_env_accessory (agent_id, merchant_id, type_id, acc_name, acc_code, qty, unit_price, inbound_cost, "
                            + "operator_kind, operator_id, remark, del_flag) VALUES (?,?,?,?,?,?,0,0,?,?,?,'0')",
                    agentId,
                    null,
                    typeId,
                    typeName,
                    null,
                    negQty,
                    String.valueOf(operatorKind),
                    opId,
                    remark);
        }
    }

    private void appendAccessoryScope(StringBuilder sql, List<Object> args, OpenidBizScope s) {
        if (s.getUserRole() == '1') {
            return;
        }
        if (s.getUserRole() == '2' || s.getUserRole() == '3') {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ?");
                args.add(s.getAgentId());
            }
        } else if (s.getUserRole() == '4') {
            if (s.getMerchantId() == null || s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ? AND (a.merchant_id = ? OR a.merchant_id IS NULL)");
                args.add(s.getAgentId());
                args.add(s.getMerchantId());
            }
        } else {
            sql.append(" AND 1 = 0");
        }
    }

    /**
     * 库存汇总（按代理）；主端可查全部或指定 agentId。
     */
    public List<Map<String, Object>> listStockSummary(String openid, Long filterAgentId) {
        OpenidBizScope s = scopeService.resolve(openid);
        Long aid = resolveScopedAgentId(s, filterAgentId);
        if (s.getUserRole() == '1' && aid == null) {
            return jdbcTemplate.query(
                    "SELECT s.agent_id, a.agent_name, ot.type_name AS oil_type_name, s.stock_item_code, "
                            + "s.total_qty AS qty_on_hand, s.lock_qty AS qty_reserved, s.available_qty AS qty_available "
                            + "FROM biz_env_agent_stock s "
                            + "JOIN biz_env_agent a ON a.agent_id = s.agent_id AND a.del_flag = '0' "
                            + "LEFT JOIN biz_env_oil_type ot ON ot.oil_type_id = CAST(NULLIF(TRIM(s.stock_item_code), '') AS UNSIGNED) "
                            + "AND ot.del_flag = '0' "
                            + "WHERE s.stock_item_type = '1' AND IFNULL(TRIM(s.stock_item_code), '') <> '' "
                            + "AND s.del_flag = '0' ORDER BY s.agent_id, s.stock_item_code",
                    (rs, i) -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("agentId", rs.getLong("agent_id"));
                        row.put("agentName", rs.getString("agent_name"));
                        row.put("oilTypeName", rs.getString("oil_type_name"));
                        row.put("stockItemCode", rs.getString("stock_item_code"));
                        row.put("qtyOnHand", rs.getBigDecimal("qty_on_hand").doubleValue());
                        row.put("qtyReserved", rs.getBigDecimal("qty_reserved").doubleValue());
                        row.put("qtyAvailable", rs.getBigDecimal("qty_available").doubleValue());
                        return row;
                    });
        }
        if (aid == null) {
            return new ArrayList<>();
        }
        stockService.ensureAgentRow(aid, 1L);
        return jdbcTemplate.query(
                "SELECT s.agent_id, a.agent_name, ot.type_name AS oil_type_name, s.stock_item_code, "
                        + "s.total_qty AS qty_on_hand, s.lock_qty AS qty_reserved, s.available_qty AS qty_available "
                        + "FROM biz_env_agent_stock s "
                        + "JOIN biz_env_agent a ON a.agent_id = s.agent_id AND a.del_flag = '0' "
                        + "LEFT JOIN biz_env_oil_type ot ON ot.oil_type_id = CAST(NULLIF(TRIM(s.stock_item_code), '') AS UNSIGNED) "
                        + "AND ot.del_flag = '0' "
                        + "WHERE s.agent_id = ? AND s.stock_item_type = '1' AND IFNULL(TRIM(s.stock_item_code), '') <> '' "
                        + "AND s.del_flag = '0'",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("agentId", rs.getLong("agent_id"));
                    row.put("agentName", rs.getString("agent_name"));
                    row.put("oilTypeName", rs.getString("oil_type_name"));
                    row.put("stockItemCode", rs.getString("stock_item_code"));
                    row.put("qtyOnHand", rs.getBigDecimal("qty_on_hand").doubleValue());
                    row.put("qtyReserved", rs.getBigDecimal("qty_reserved").doubleValue());
                    row.put("qtyAvailable", rs.getBigDecimal("qty_available").doubleValue());
                    return row;
                },
                aid);
    }

    public List<Map<String, Object>> listStockFlows(String openid, Long filterAgentId) {
        OpenidBizScope s = scopeService.resolve(openid);
        Long aid = resolveScopedAgentId(s, filterAgentId);
        if (s.getUserRole() == '1' && aid == null) {
            return jdbcTemplate.query(
                    "SELECT f.flow_id, f.agent_id, f.change_type AS flow_kind_code, f.related_no AS ref_no, "
                            + "f.change_qty AS qty, f.remark, f.create_time, CAST(NULL AS CHAR) AS ref_type "
                            + "FROM biz_env_agent_stock_flow f "
                            + "JOIN biz_env_agent_stock s ON s.stock_id = f.stock_id AND s.del_flag = '0' "
                            + "WHERE s.stock_item_type = '1' AND IFNULL(TRIM(s.stock_item_code), '') <> '' "
                            + "ORDER BY f.flow_id DESC LIMIT 200",
                    (rs, i) -> flowRow(rs));
        }
        if (aid == null) {
            return new ArrayList<>();
        }
        return jdbcTemplate.query(
                "SELECT f.flow_id, f.agent_id, f.change_type AS flow_kind_code, f.related_no AS ref_no, "
                        + "f.change_qty AS qty, f.remark, f.create_time, CAST(NULL AS CHAR) AS ref_type "
                        + "FROM biz_env_agent_stock_flow f "
                        + "JOIN biz_env_agent_stock s ON s.stock_id = f.stock_id AND s.del_flag = '0' "
                        + "WHERE f.agent_id = ? AND s.stock_item_type = '1' AND IFNULL(TRIM(s.stock_item_code), '') <> '' "
                        + "ORDER BY f.flow_id DESC LIMIT 200",
                (rs, i) -> flowRow(rs),
                aid);
    }

    public List<Map<String, Object>> listAccountLedger(String openid, Long filterAgentId) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        // 账目流水仅主端、代理可查；业务员与商家不可见
        if (r == '3' || r == '4') {
            return new ArrayList<>();
        }
        Long aid = resolveScopedAgentId(s, filterAgentId);
        if (r == '1' && aid == null) {
            return jdbcTemplate.query(
                    "SELECT ledger_id, agent_id, merchant_id, ref_type, ref_no, title, amount, direction, create_time "
                            + "FROM biz_env_account_ledger ORDER BY ledger_id DESC LIMIT 200",
                    (rs, i) -> ledgerRow(rs));
        }
        if (aid == null) {
            return new ArrayList<>();
        }
        return jdbcTemplate.query(
                "SELECT ledger_id, agent_id, merchant_id, ref_type, ref_no, title, amount, direction, create_time "
                        + "FROM biz_env_account_ledger WHERE agent_id = ? ORDER BY ledger_id DESC LIMIT 200",
                (rs, i) -> ledgerRow(rs),
                aid);
    }

    /** 账户信息（基础信息卡 + 组织信息卡） */
    public Map<String, Object> accountProfile(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("openid", openid);
        out.put("roleCode", String.valueOf(r));
        out.put("roleName", roleName(r));
        // env_mini_subject 首版无 create_time，这里预留字段，后续有库字段可补
        out.put("bindTime", "");

        Map<String, Object> org = new LinkedHashMap<>();
        if (r == '1') {
            org.put("platformName", "环保油平台");
            org.put("adminName", "平台管理员");
        } else if (r == '2') {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "SELECT agent_name, contact_name, contact_phone, province, city, district "
                            + "FROM biz_env_agent WHERE agent_id = ? AND del_flag = '0'",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("agentName", rs.getString("agent_name"));
                        m.put("contactName", rs.getString("contact_name"));
                        m.put("contactPhone", rs.getString("contact_phone"));
                        m.put("region", String.join("/",
                                nullSafe(rs.getString("province")),
                                nullSafe(rs.getString("city")),
                                nullSafe(rs.getString("district"))));
                        return m;
                    },
                    s.getAgentId());
            if (!rows.isEmpty()) {
                org.putAll(rows.get(0));
            }
        } else if (r == '3') {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "SELECT sm.salesman_name, sm.phone, a.agent_name "
                            + "FROM biz_env_salesman sm "
                            + "LEFT JOIN biz_env_agent a ON a.agent_id = sm.agent_id AND a.del_flag = '0' "
                            + "WHERE sm.salesman_id = ? AND sm.del_flag = '0'",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("salesmanName", rs.getString("salesman_name"));
                        m.put("phone", rs.getString("phone"));
                        m.put("agentName", rs.getString("agent_name"));
                        return m;
                    },
                    s.getSalesmanId());
            if (!rows.isEmpty()) {
                org.putAll(rows.get(0));
            }
        } else if (r == '4') {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "SELECT m.merchant_name, m.contact_name, m.contact_phone, "
                            + "a.agent_name, sm.salesman_name "
                            + "FROM biz_env_merchant m "
                            + "LEFT JOIN biz_env_agent a ON a.agent_id = m.agent_id AND a.del_flag = '0' "
                            + "LEFT JOIN biz_env_salesman sm ON sm.salesman_id = m.salesman_id AND sm.del_flag = '0' "
                            + "WHERE m.merchant_id = ? AND m.del_flag = '0'",
                    (rs, i) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("merchantName", rs.getString("merchant_name"));
                        m.put("contactName", rs.getString("contact_name"));
                        m.put("contactPhone", rs.getString("contact_phone"));
                        m.put("agentName", rs.getString("agent_name"));
                        m.put("salesmanName", rs.getString("salesman_name"));
                        return m;
                    },
                    s.getMerchantId());
            if (!rows.isEmpty()) {
                org.putAll(rows.get(0));
            }
        }
        out.put("orgInfo", org);
        return out;
    }

    public List<Map<String, Object>> listAccountShares(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        return jdbcTemplate.query(
                "SELECT share_id, shared_openid, user_role, agent_id, merchant_id, salesman_id, create_time "
                        + "FROM biz_env_account_share WHERE owner_openid = ? AND del_flag = '0' ORDER BY share_id DESC",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("shareId", rs.getLong("share_id"));
                    m.put("sharedOpenid", rs.getString("shared_openid"));
                    String role = rs.getString("user_role");
                    m.put("roleCode", role);
                    m.put("roleName", roleName(role == null || role.isEmpty() ? s.getUserRole() : role.charAt(0)));
                    m.put("agentId", rs.getObject("agent_id") == null ? null : rs.getLong("agent_id"));
                    m.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
                    m.put("salesmanId", rs.getObject("salesman_id") == null ? null : rs.getLong("salesman_id"));
                    m.put("createTime", formatTs(rs.getTimestamp("create_time")));
                    return m;
                },
                openid);
    }

    public void addAccountShare(String openid, String sharedOpenid) {
        OpenidBizScope s = scopeService.resolve(openid);
        String target = sharedOpenid == null ? "" : sharedOpenid.trim();
        if (target.isEmpty()) {
            throw new IllegalArgumentException("共享 openid 不能为空");
        }
        if (openid.equals(target)) {
            throw new IllegalArgumentException("不能共享给自己");
        }
        jdbcTemplate.update(
                "UPDATE biz_env_account_share SET del_flag = '2' WHERE owner_openid = ? AND shared_openid = ?",
                openid, target);
        jdbcTemplate.update(
                "INSERT INTO biz_env_account_share (owner_openid, shared_openid, user_role, agent_id, merchant_id, salesman_id, status, del_flag) "
                        + "VALUES (?,?,?,?,?,?, '0', '0')",
                openid,
                target,
                String.valueOf(s.getUserRole()),
                s.getAgentId(),
                s.getMerchantId(),
                s.getSalesmanId());

        // 共享账号沿用主账号数据范围 + 门户角色
        jdbcTemplate.update("DELETE FROM env_openid_biz_scope WHERE openid = ?", target);
        jdbcTemplate.update(
                "INSERT INTO env_openid_biz_scope (openid, user_role, agent_id, merchant_id, salesman_id) VALUES (?,?,?,?,?)",
                target,
                String.valueOf(s.getUserRole()),
                s.getAgentId(),
                s.getMerchantId(),
                s.getSalesmanId());

        long roleId = roleIdByUserRole(s.getUserRole());
        jdbcTemplate.update("DELETE FROM env_mini_subject WHERE openid = ?", target);
        jdbcTemplate.update("INSERT INTO env_mini_subject (openid, role_id) VALUES (?,?)", target, roleId);
    }

    public void removeAccountShare(String openid, String sharedOpenid) {
        String target = sharedOpenid == null ? "" : sharedOpenid.trim();
        if (target.isEmpty()) {
            throw new IllegalArgumentException("sharedOpenid不能为空");
        }
        jdbcTemplate.update(
                "UPDATE biz_env_account_share SET del_flag = '2' WHERE owner_openid = ? AND shared_openid = ? AND del_flag = '0'",
                openid, target);
        jdbcTemplate.update("DELETE FROM env_openid_biz_scope WHERE openid = ?", target);
        jdbcTemplate.update("DELETE FROM env_mini_subject WHERE openid = ?", target);
    }

    public void inboundStock(
            String openid,
            BigDecimal qty,
            Long agentIdForMain,
            Long oilTypeId,
            String qtyUnitRaw,
            String remark) {
        OpenidBizScope s = scopeService.resolve(openid);
        char r = s.getUserRole();
        if (r != '1' && r != '2') {
            throw new IllegalArgumentException("仅主端或代理可入库");
        }
        long agentId;
        if (r == '1') {
            if (agentIdForMain == null) {
                throw new IllegalArgumentException("主端入库请指定 agentId");
            }
            agentId = agentIdForMain;
        } else {
            if (s.getAgentId() == null) {
                throw new IllegalArgumentException("未绑定代理");
            }
            agentId = s.getAgentId();
        }
        long otId = oilTypeId == null ? 1L : oilTypeId.longValue();
        List<Map<String, Object>> otRows = jdbcTemplate.queryForList(
                "SELECT density_kg_per_liter, liters_per_bucket FROM biz_env_oil_type WHERE oil_type_id = ? AND del_flag = '0'",
                otId);
        if (otRows.isEmpty()) {
            throw new IllegalArgumentException("油品类型不存在");
        }
        Map<String, Object> ot = otRows.get(0);
        BigDecimal density = new BigDecimal(ot.get("density_kg_per_liter").toString());
        BigDecimal litersPerBucket = new BigDecimal(ot.get("liters_per_bucket").toString());
        char u = OilQuantityConverter.normalizeOilQtyUnit(qtyUnitRaw);
        BigDecimal buckets = OilQuantityConverter.toBuckets(qty, u, density, litersPerBucket).setScale(4, RoundingMode.HALF_UP);
        String rm = remark;
        if (rm == null || rm.isEmpty()) {
            rm = "手工入库（折算 "
                    + buckets.stripTrailingZeros().toPlainString()
                    + " 桶当量，入库单位 "
                    + u
                    + "）";
        }
        stockService.inboundOil(agentId, otId, buckets, rm);
    }

    public List<Map<String, Object>> listOilTypes() {
        return jdbcTemplate.query(
                "SELECT oil_type_id, type_name, density_kg_per_liter, liters_per_bucket "
                        + "FROM biz_env_oil_type WHERE del_flag = '0' ORDER BY sort_order, oil_type_id",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("oilTypeId", rs.getLong("oil_type_id"));
                    row.put("typeName", rs.getString("type_name"));
                    row.put("densityKgPerLiter", rs.getBigDecimal("density_kg_per_liter").doubleValue());
                    row.put("litersPerBucket", rs.getBigDecimal("liters_per_bucket").doubleValue());
                    return row;
                });
    }

    /**
     * 客户端首页待办红泡：待处理订单、工单、店铺审核、设备审核（按当前 openid 数据范围）。
     */
    public Map<String, Object> pendingTodoCounts(String openid) {
        OpenidBizScope s = scopeService.resolve(openid);
        long orders = countScopedPendingOrders(s);
        long works = countScopedPendingWorkOrders(s);
        long merch = countScopedPendingMerchantAudits(s, openid);
        long dev = countScopedPendingDeviceAudits(s);
        long total = orders + works + merch + dev;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("orders", orders);
        out.put("workOrders", works);
        out.put("merchantAudits", merch);
        out.put("deviceAudits", dev);
        return out;
    }

    private static long queryCount(JdbcTemplate jdbc, String sql, Object[] args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private long countScopedPendingOrders(OpenidBizScope s) {
        char r = s.getUserRole();
        if (r != '1' && r != '2' && r != '3' && r != '4') {
            return 0;
        }
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(*) FROM biz_env_order o ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = o.merchant_id AND m.del_flag = '0' ")
                .append("WHERE o.del_flag = '0' AND o.status IN ('0','1') ");
        List<Object> args = new ArrayList<>();
        appendOrderPendingScope(sql, args, s);
        return queryCount(jdbcTemplate, sql.toString(), args.toArray());
    }

    private void appendOrderPendingScope(StringBuilder sql, List<Object> args, OpenidBizScope scope) {
        char r = scope.getUserRole();
        if (r == '1') {
            return;
        }
        if (r == '2' && scope.getAgentId() != null) {
            sql.append(" AND o.agent_id = ?");
            args.add(scope.getAgentId());
            return;
        }
        if (r == '4' && scope.getMerchantId() != null) {
            sql.append(" AND o.merchant_id = ?");
            args.add(scope.getMerchantId());
            return;
        }
        if (r == '3' && scope.getAgentId() != null && scope.getSalesmanId() != null) {
            sql.append(" AND o.agent_id = ? AND (m.salesman_id = ? OR o.receive_salesman_id = ?)");
            args.add(scope.getAgentId());
            args.add(scope.getSalesmanId());
            args.add(scope.getSalesmanId());
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private long countScopedPendingWorkOrders(OpenidBizScope s) {
        char r = s.getUserRole();
        if (r != '1' && r != '2' && r != '3' && r != '4') {
            return 0;
        }
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(*) FROM biz_env_work_order w ")
                .append("JOIN biz_env_merchant m ON m.merchant_id = w.merchant_id AND m.del_flag = '0' ")
                .append("WHERE w.del_flag = '0' AND w.status IN ('0','1') ");
        List<Object> args = new ArrayList<>();
        appendWorkOrderPendingScope(sql, args, s);
        return queryCount(jdbcTemplate, sql.toString(), args.toArray());
    }

    private void appendWorkOrderPendingScope(StringBuilder sql, List<Object> args, OpenidBizScope scope) {
        char r = scope.getUserRole();
        if (r == '1') {
            return;
        }
        if (r == '2' && scope.getAgentId() != null) {
            sql.append(" AND w.agent_id = ?");
            args.add(scope.getAgentId());
            return;
        }
        if (r == '4' && scope.getMerchantId() != null) {
            sql.append(" AND w.merchant_id = ?");
            args.add(scope.getMerchantId());
            return;
        }
        if (r == '3' && scope.getAgentId() != null && scope.getSalesmanId() != null) {
            sql.append(" AND w.agent_id = ? AND (")
                    .append("(w.status = '1' AND w.receive_salesman_id IS NULL) ")
                    .append("OR w.receive_salesman_id = ? ")
                    .append("OR (m.salesman_id = ? AND m.agent_id = ?)")
                    .append(")");
            args.add(scope.getAgentId());
            args.add(scope.getSalesmanId());
            args.add(scope.getSalesmanId());
            args.add(scope.getAgentId());
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private long countScopedPendingMerchantAudits(OpenidBizScope s, String openid) {
        char r = s.getUserRole();
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(*) FROM biz_env_merchant_audit a ")
                .append("WHERE a.del_flag = '0' AND a.status = '0' ");
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
        } else if (r == '4') {
            sql.append(" AND a.submitter_openid = ?");
            args.add(openid);
        } else {
            sql.append(" AND 1 = 0");
        }
        return queryCount(jdbcTemplate, sql.toString(), args.toArray());
    }

    private long countScopedPendingDeviceAudits(OpenidBizScope s) {
        char r = s.getUserRole();
        if (r != '1' && r != '2') {
            return 0;
        }
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(*) FROM biz_env_device_event_audit a ")
                .append("WHERE a.del_flag = '0' AND a.status = '0' ");
        List<Object> args = new ArrayList<>();
        if (r == '1') {
            /* all */
        } else {
            if (s.getAgentId() == null) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND a.agent_id = ?");
                args.add(s.getAgentId());
            }
        }
        return queryCount(jdbcTemplate, sql.toString(), args.toArray());
    }

    private Long resolveScopedAgentId(OpenidBizScope s, Long filterAgentId) {
        char r = s.getUserRole();
        if (r == '1') {
            return filterAgentId;
        }
        if (r == '2' || r == '3') {
            return s.getAgentId();
        }
        if (r == '4') {
            if (s.getMerchantId() == null) {
                return null;
            }
            List<Long> aids = jdbcTemplate.query(
                    "SELECT agent_id FROM biz_env_merchant WHERE merchant_id = ? AND del_flag = '0'",
                    (rs, rowNum) -> rs.getLong("agent_id"),
                    s.getMerchantId());
            return aids.isEmpty() ? null : aids.get(0);
        }
        return null;
    }

    private static Map<String, Object> flowRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("flowId", rs.getLong("flow_id"));
        row.put("agentId", rs.getLong("agent_id"));
        row.put("refType", rs.getString("ref_type"));
        row.put("refNo", rs.getString("ref_no"));
        String kindCode = rs.getString("flow_kind_code");
        row.put("flowKind", labelFlowKind(kindCode));
        row.put("flowKindCode", kindCode);
        row.put("qty", rs.getBigDecimal("qty").doubleValue());
        row.put("remark", rs.getString("remark"));
        row.put("createTime", formatTs(rs.getTimestamp("create_time")));
        return row;
    }

    private static Map<String, Object> ledgerRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ledgerId", rs.getLong("ledger_id"));
        row.put("agentId", rs.getObject("agent_id") == null ? null : rs.getLong("agent_id"));
        row.put("merchantId", rs.getObject("merchant_id") == null ? null : rs.getLong("merchant_id"));
        row.put("refType", rs.getString("ref_type"));
        row.put("refNo", rs.getString("ref_no"));
        row.put("title", rs.getString("title"));
        row.put("amount", rs.getBigDecimal("amount").doubleValue());
        row.put("direction", "1".equals(rs.getString("direction")) ? "收入" : "支出");
        row.put("directionCode", rs.getString("direction"));
        row.put("createTime", formatTs(rs.getTimestamp("create_time")));
        return row;
    }

    private static String labelFlowKind(String code) {
        if ("1".equals(code) || "I".equals(code)) {
            return "入库";
        }
        if ("2".equals(code) || "R".equals(code)) {
            return "预扣";
        }
        if ("3".equals(code) || "D".equals(code)) {
            return "实扣";
        }
        if ("4".equals(code) || "B".equals(code)) {
            return "回滚";
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

    private static String roleName(char code) {
        if (code == '1') return "主端";
        if (code == '2') return "代理";
        if (code == '3') return "运维";
        if (code == '4') return "商家";
        return String.valueOf(code);
    }

    private static String nullSafe(String v) {
        return v == null ? "" : v;
    }

    private static long roleIdByUserRole(char code) {
        if (code == '1') return 1L;
        if (code == '2') return 2L;
        if (code == '3') return 3L;
        if (code == '4') return 4L;
        return 1L;
    }

    private void appendMerchantScope(StringBuilder sql, List<Object> args, OpenidBizScope s) {
        char r = s.getUserRole();
        if (r == '1') {
            return;
        }
        if (r == '2' && s.getAgentId() != null) {
            sql.append(" AND m.agent_id = ?");
            args.add(s.getAgentId());
            return;
        }
        if (r == '3' && s.getAgentId() != null && s.getSalesmanId() != null) {
            sql.append(" AND m.agent_id = ? AND m.salesman_id = ?");
            args.add(s.getAgentId());
            args.add(s.getSalesmanId());
            return;
        }
        if (r == '4' && s.getMerchantId() != null) {
            sql.append(" AND m.merchant_id = ?");
            args.add(s.getMerchantId());
            return;
        }
        sql.append(" AND 1 = 0");
    }

    private static String labelMerchantStatus(String code) {
        if ("0".equals(code)) {
            return "正常";
        }
        if ("1".equals(code)) {
            return "停用";
        }
        return code == null ? "" : code;
    }

    private static String labelAgentStatus(String code) {
        return labelMerchantStatus(code);
    }

    private static String labelSalesmanStatus(String code) {
        return labelMerchantStatus(code);
    }

    private static String labelDeviceType(String code) {
        if ("1".equals(code)) {
            return "油机";
        }
        if ("2".equals(code)) {
            return "其它";
        }
        return code == null ? "" : code;
    }

    private static String labelDeviceStatus(String code) {
        if ("0".equals(code)) {
            return "在库（可调拨·未装机）";
        }
        if ("1".equals(code)) {
            return "在店（已装机·运营中）";
        }
        if ("2".equals(code)) {
            return "维修中（暂停使用）";
        }
        if ("3".equals(code)) {
            return "停用（未报废）";
        }
        if ("4".equals(code)) {
            return "报废（终态·已下线）";
        }
        return code == null ? "" : code;
    }
}
