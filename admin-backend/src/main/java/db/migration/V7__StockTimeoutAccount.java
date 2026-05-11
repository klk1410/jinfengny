package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 工单抢单截止、仓储、库存流水、账目流水。
 * 列已存在时跳过 ALTER（避免 #1060 Duplicate column）。
 */
public class V7__StockTimeoutAccount extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        try (Statement st = conn.createStatement()) {
            addColumnIfNotExists(conn, st, "biz_env_work_order", "accept_deadline", "TIMESTAMP NULL");
            addColumnIfNotExists(conn, st, "biz_env_work_order", "assign_type", "CHAR(1) DEFAULT '1'");

            st.execute("CREATE TABLE IF NOT EXISTS biz_env_agent_stock ("
                    + " agent_id      BIGINT NOT NULL,"
                    + " sku_code      CHAR(1) NOT NULL DEFAULT '1',"
                    + " qty_on_hand   DECIMAL(14,2) NOT NULL DEFAULT 0,"
                    + " qty_reserved  DECIMAL(14,2) NOT NULL DEFAULT 0,"
                    + " PRIMARY KEY (agent_id, sku_code)"
                    + ")");

            st.execute("CREATE TABLE IF NOT EXISTS biz_env_stock_flow ("
                    + " flow_id       BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + " agent_id      BIGINT NOT NULL,"
                    + " sku_code      CHAR(1) NOT NULL DEFAULT '1',"
                    + " ref_type      VARCHAR(32) NOT NULL,"
                    + " ref_no        VARCHAR(64),"
                    + " flow_kind     CHAR(1) NOT NULL,"
                    + " qty           DECIMAL(14,2) NOT NULL,"
                    + " remark        VARCHAR(255),"
                    + " create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            st.execute("CREATE TABLE IF NOT EXISTS biz_env_account_ledger ("
                    + " ledger_id     BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + " agent_id      BIGINT,"
                    + " merchant_id   BIGINT,"
                    + " ref_type      VARCHAR(32) NOT NULL,"
                    + " ref_no        VARCHAR(64),"
                    + " title         VARCHAR(128) NOT NULL,"
                    + " amount        DECIMAL(14,2) NOT NULL,"
                    + " direction     CHAR(1) NOT NULL,"
                    + " create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            st.execute(
                    "INSERT INTO biz_env_agent_stock (agent_id, sku_code, qty_on_hand, qty_reserved) "
                            + "SELECT 1, '1', 10000.00, 0.00 WHERE NOT EXISTS "
                            + "(SELECT 1 FROM biz_env_agent_stock WHERE agent_id = 1 AND sku_code = '1')");

            if (columnExists(conn, "biz_env_work_order", "accept_deadline")) {
                st.execute(
                        "UPDATE biz_env_work_order SET accept_deadline = work_order_time "
                                + "WHERE accept_deadline IS NULL AND del_flag = '0'");
            }
        }
    }

    private static void addColumnIfNotExists(Connection conn, Statement st, String table, String column, String typeDef)
            throws Exception {
        if (columnExists(conn, table, column)) {
            return;
        }
        st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + typeDef);
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData md = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = conn.getSchema();
        String[] tableVariants = new String[] {table, table.toUpperCase(), table.toLowerCase()};
        for (String t : tableVariants) {
            try (ResultSet rs = md.getColumns(catalog, schema, t, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null && name.equalsIgnoreCase(column)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
