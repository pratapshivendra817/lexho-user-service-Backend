import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class FlywayChecksumRepair {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: FlywayChecksumRepair <jdbcUrl> <username> <password> <version> <checksum>");
            System.exit(1);
        }

        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String version = args[3];
        int checksum = Integer.parseInt(args[4]);

        String sql = "UPDATE flyway_schema_history SET checksum=? WHERE version=?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, checksum);
            preparedStatement.setString(2, version);
            int updated = preparedStatement.executeUpdate();
            System.out.println("Rows updated: " + updated);
            if (updated == 0) {
                System.err.println("No flyway row found for version=" + version);
                System.exit(2);
            }
        }
    }
}
