import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class FlywayFailedMigrationCleanup {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: FlywayFailedMigrationCleanup <jdbcUrl> <username> <password> <version>");
            System.exit(1);
        }

        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String version = args[3];

        String sql = "DELETE FROM flyway_schema_history WHERE version = ? AND success = 0";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, version);
            int deleted = preparedStatement.executeUpdate();
            System.out.println("Rows deleted: " + deleted);
        }
    }
}
