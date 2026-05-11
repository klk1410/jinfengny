package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppPortalJdbcService {

    private final JdbcTemplate jdbc;
    private final AppMockService appMockService;

    public AppPortalJdbcService(JdbcTemplate jdbc, AppMockService appMockService) {
        this.jdbc = jdbc;
        this.appMockService = appMockService;
    }

    public Map<String, Object> portalModules(String openid) {
        Map<String, Object> login = new LinkedHashMap<>(appMockService.loginByOpenid(openid));

        long roleId = resolveRoleId(openid);
        Map<String, String> roleMeta = loadRole(roleId);
        String roleCode = roleMeta.getOrDefault("roleCode", "guest");
        String roleName = roleMeta.getOrDefault("roleName", "未授权");

        boolean hasBiz = !"guest".equals(roleCode);
        login.put("role", roleName);
        login.put("roleCode", roleCode);
        login.put("hasBusinessAccess", hasBiz);

        List<Row> rows = jdbc.query(
                "SELECT g.group_id, g.title AS gtitle, g.sort_order AS gsort, "
                        + "f.label, f.icon, f.route_path, f.sort_order AS fsort, f.perm_code "
                        + "FROM env_portal_func_group g "
                        + "JOIN env_portal_function f ON f.group_id = g.group_id AND f.del_flag = '0' "
                        + "WHERE g.del_flag = '0' AND g.status = '0' AND f.status = '0' "
                        + "AND f.perm_code IN ("
                        + " SELECT perm_code FROM env_mini_role_perm WHERE role_id = ?"
                        + ") ORDER BY g.sort_order, g.group_id, f.sort_order, f.func_id",
                (rs, i) -> new Row(
                        rs.getLong("group_id"),
                        rs.getString("gtitle"),
                        rs.getInt("gsort"),
                        rs.getString("label"),
                        rs.getString("icon"),
                        rs.getString("route_path"),
                        rs.getString("perm_code"),
                        rs.getInt("fsort")),
                roleId);

        Long lastGroup = null;
        List<Map<String, Object>> sections = new ArrayList<>();
        List<Map<String, Object>> currentItems = null;
        String currentTitle = null;
        List<String> moduleTitles = new ArrayList<>();
        List<String> gridEntries = new ArrayList<>();

        for (Row r : rows) {
            if (lastGroup == null || lastGroup.longValue() != r.groupId) {
                lastGroup = r.groupId;
                currentTitle = r.groupTitle;
                moduleTitles.add(currentTitle);
                Map<String, Object> sec = new LinkedHashMap<>();
                sec.put("title", currentTitle);
                currentItems = new ArrayList<>();
                sec.put("items", currentItems);
                sections.add(sec);
            }
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("label", r.label);
            it.put("icon", r.icon);
            it.put("routePath", r.routePath);
            it.put("permCode", r.permCode);
            currentItems.add(it);
            gridEntries.add(r.label);
        }

        login.put("sections", sections);
        login.put("modules", moduleTitles.isEmpty()
                ? login.getOrDefault("modules", new ArrayList<>())
                : moduleTitles);
        login.put("gridEntries", gridEntries);

        login.put("openid", openid);
        return login;
    }

    private long resolveRoleId(String openid) {
        List<Long> ids = jdbc.query(
                "SELECT role_id FROM env_mini_subject WHERE openid = ?",
                (rs, i) -> rs.getLong(1),
                openid);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        String inferred = inferRoleCode(openid);
        List<Long> byCode = jdbc.query(
                "SELECT role_id FROM env_mini_role WHERE role_code = ?",
                (rs, i) -> rs.getLong(1),
                inferred);
        if (!byCode.isEmpty()) {
            return byCode.get(0);
        }
        List<Long> guest = jdbc.query(
                "SELECT role_id FROM env_mini_role WHERE role_code = 'guest'",
                (rs, i) -> rs.getLong(1));
        if (guest.isEmpty()) {
            throw new IllegalStateException("数据库缺少 env_mini_role.guest，请执行 Flyway 迁移");
        }
        return guest.get(0);
    }

    private static String inferRoleCode(String openid) {
        if (openid.contains("main-openid")) {
            return "main";
        }
        if (openid.contains("agent-openid")) {
            return "agent";
        }
        if (openid.contains("sales-openid")) {
            return "sales";
        }
        if (openid.contains("merchant-openid")) {
            return "merchant";
        }
        return "guest";
    }

    private Map<String, String> loadRole(long roleId) {
        List<Map<String, String>> list = jdbc.query(
                "SELECT role_code AS roleCode, role_name AS roleName FROM env_mini_role WHERE role_id = ?",
                (rs, i) -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("roleCode", rs.getString("roleCode"));
                    m.put("roleName", rs.getString("roleName"));
                    return m;
                },
                roleId);
        return list.isEmpty() ? new LinkedHashMap<>() : list.get(0);
    }

    private static final class Row {
        final long groupId;
        final String groupTitle;
        @SuppressWarnings("unused")
        final int groupSort;
        final String label;
        final String icon;
        final String routePath;
        final String permCode;
        @SuppressWarnings("unused")
        final int funcSort;

        Row(long groupId, String groupTitle, int groupSort,
            String label, String icon, String routePath, String permCode, int funcSort) {
            this.groupId = groupId;
            this.groupTitle = groupTitle;
            this.groupSort = groupSort;
            this.label = label;
            this.icon = icon;
            this.routePath = routePath;
            this.permCode = permCode;
            this.funcSort = funcSort;
        }
    }
}
