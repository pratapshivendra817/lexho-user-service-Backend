import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbTableInspector {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: DbTableInspector <jdbcUrl> <username> <password> <tableName>");
            System.exit(1);
        }
        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String table = args[3];

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            String sql = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY " +
                    "FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                    "ORDER BY ORDINAL_POSITION";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("Table: " + table);
                    while (rs.next()) {
                        System.out.println(
                                rs.getString("COLUMN_NAME") + " | " +
                                        rs.getString("COLUMN_TYPE") + " | " +
                                        rs.getString("IS_NULLABLE") + " | " +
                                        rs.getString("COLUMN_KEY")
                        );
                    }
                }
            }
        }
    }
}
