package com.envoil.admin.portal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PortalJdbcService {

    private final JdbcTemplate jdbc;

    public PortalJdbcService(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public List<Map<String, Object>> loadPortalTree() {
        List<Map<String, Object>> groups = jdbc.query(
                "SELECT group_id AS id, title, sort_order AS sortOrder "
                        + "FROM env_portal_func_group WHERE del_flag = '0' ORDER BY sort_order, group_id",
                (rs, i) -> {
                    Map<String, Object> g = new LinkedHashMap<>();
                    g.put("id", rs.getLong("id"));
                    g.put("title", rs.getString("title"));
                    g.put("sortOrder", rs.getInt("sortOrder"));
                    return g;
                });

        for (Map<String, Object> g : groups) {
            long gid = (Long) g.get("id");
            List<Map<String, Object>> funcs = jdbc.query(
                    "SELECT func_id AS id, group_id AS groupId, perm_code AS permCode, "
                            + "label, icon, route_path AS routePath, sort_order AS sortOrder "
                            + "FROM env_portal_function WHERE del_flag = '0' AND group_id = ? "
                            + "ORDER BY sort_order, func_id",
                    (rs, row) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", rs.getLong("id"));
                        m.put("groupId", rs.getLong("groupId"));
                        m.put("permCode", rs.getString("permCode"));
                        m.put("label", rs.getString("label"));
                        m.put("icon", rs.getString("icon"));
                        m.put("routePath", rs.getString("routePath"));
                        m.put("sortOrder", rs.getInt("sortOrder"));
                        return m;
                    },
                    gid);
            g.put("functions", funcs);
        }
        return groups;
    }

    public Long createGroup(String title, Integer sortOrder) {
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        int ord = sortOrder == null ? 0 : sortOrder;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO env_portal_func_group (title, sort_order, status, del_flag) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            ps.setInt(2, ord);
            ps.setString(3, "0");
            ps.setString(4, "0");
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).longValue();
    }

    public void updateGroup(long id, String title, Integer sortOrder) {
        jdbc.update(
                "UPDATE env_portal_func_group SET title = ?, sort_order = ? WHERE group_id = ? AND del_flag = '0'",
                title, sortOrder == null ? 0 : sortOrder, id);
    }

    public void softDeleteGroup(long id) {
        jdbc.update("UPDATE env_portal_func_group SET del_flag = '2' WHERE group_id = ?", id);
        jdbc.update("UPDATE env_portal_function SET del_flag = '2' WHERE group_id = ?", id);
    }

    public Long createFunction(long groupId, String permCode, String label, String icon,
                               String routePath, Integer sortOrder) {
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        int ord = sortOrder == null ? 0 : sortOrder;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO env_portal_function (group_id, perm_code, label, icon, route_path, sort_order, status, del_flag) "
                            + "VALUES (?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, groupId);
            ps.setString(2, permCode);
            ps.setString(3, label);
            ps.setString(4, icon);
            ps.setString(5, routePath);
            ps.setInt(6, ord);
            ps.setString(7, "0");
            ps.setString(8, "0");
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).longValue();
    }

    public void updateFunction(long funcId, long groupId, String permCode, String label,
                              String icon, String routePath, Integer sortOrder) {
        jdbc.update(
                "UPDATE env_portal_function SET group_id=?, perm_code=?, label=?, icon=?, route_path=?, sort_order=? "
                        + "WHERE func_id=? AND del_flag='0'",
                groupId, permCode, label, icon, routePath,
                sortOrder == null ? 0 : sortOrder, funcId);
    }

    public List<Map<String, Object>> listRolesWithPerms() {
        List<Map<String, Object>> roles = jdbc.query(
                "SELECT role_id AS id, role_code AS roleCode, role_name AS roleName, sort_order AS sortOrder "
                        + "FROM env_mini_role WHERE status = '0' ORDER BY sort_order",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("roleCode", rs.getString("roleCode"));
                    m.put("roleName", rs.getString("roleName"));
                    m.put("sortOrder", rs.getInt("sortOrder"));
                    return m;
                });

        for (Map<String, Object> r : roles) {
            long rid = ((Number) r.get("id")).longValue();
            List<String> codes = jdbc.query(
                    "SELECT perm_code FROM env_mini_role_perm WHERE role_id = ? ORDER BY perm_code",
                    (rs, rowNum) -> rs.getString(1),
                    rid);
            r.put("permCodes", codes);
        }
        return roles;
    }

    @Transactional
    public void replaceRolePerms(long roleId, List<String> permCodes) {
        jdbc.update("DELETE FROM env_mini_role_perm WHERE role_id = ?", roleId);
        if (permCodes != null) {
            for (String code : permCodes) {
                if (code != null && !code.isEmpty()) {
                    jdbc.update("INSERT INTO env_mini_role_perm (role_id, perm_code) VALUES (?,?)", roleId, code.trim());
                }
            }
        }
    }

    public List<Map<String, Object>> listSubjects() {
        return jdbc.query(
                "SELECT s.openid, s.role_id AS roleId, r.role_code AS roleCode, r.role_name AS roleName "
                        + "FROM env_mini_subject s JOIN env_mini_role r ON r.role_id = s.role_id "
                        + "ORDER BY s.openid",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("openid", rs.getString("openid"));
                    m.put("roleId", rs.getLong("roleId"));
                    m.put("roleCode", rs.getString("roleCode"));
                    m.put("roleName", rs.getString("roleName"));
                    return m;
                });
    }

    public void upsertSubject(String openid, long roleId) {
        int n = jdbc.update("UPDATE env_mini_subject SET role_id = ? WHERE openid = ?", roleId, openid);
        if (n == 0) {
            jdbc.update("INSERT INTO env_mini_subject (openid, role_id) VALUES (?,?)", openid, roleId);
        }
    }

    public void deleteSubject(String openid) {
        jdbc.update("DELETE FROM env_mini_subject WHERE openid = ?", openid);
    }

    /** 当前库中可用的权限码列表（门户功能仍在用的），用于勾选框 */
    public List<String> allActivePermCodes() {
        return jdbc.query(
                "SELECT perm_code FROM env_portal_function WHERE del_flag = '0' AND status='0' ORDER BY perm_code",
                (rs, i) -> rs.getString(1));
    }

    /** 删除功能并移除各角色对该 perm_code 的授权 */
    public void softDeleteFunction(long funcId) {
        List<String> codes = jdbc.query(
                "SELECT perm_code FROM env_portal_function WHERE func_id=?",
                (rs, rn) -> rs.getString(1),
                funcId);
        jdbc.update("UPDATE env_portal_function SET del_flag = '2' WHERE func_id = ?", funcId);
        for (String c : codes) {
            jdbc.update("DELETE FROM env_mini_role_perm WHERE perm_code = ?", c);
        }
    }
}
