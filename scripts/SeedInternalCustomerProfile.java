import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SeedInternalCustomerProfile {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/user_servicedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "root";
        String pass = "Muskaan@8756";

        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id,mobile,role FROM lexho_user_profiles WHERE id = ?")) {
                ps.setLong(1, -3L);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("EXISTS id=" + rs.getLong("id")
                                + " mobile=" + rs.getString("mobile")
                                + " role=" + rs.getString("role"));
                        return;
                    }
                }
            }

            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO lexho_user_profiles (id,mobile,name,email,profile_image,gender,role) VALUES (?,?,?,?,?,?,?)")) {
                ins.setLong(1, -3L);
                ins.setString(2, "9999999999");
                ins.setString(3, "Internal Customer");
                ins.setString(4, "internal-customer@local");
                ins.setString(5, null);
                ins.setString(6, null);
                ins.setString(7, "CUSTOMER");
                int n = ins.executeUpdate();
                System.out.println("INSERTED rows=" + n);
            }
        }
    }
}
