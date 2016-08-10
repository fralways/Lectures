package filip.test;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 8/7/2016.
 */
public class DBHandler {

    private final Connection conn;
    private static int num = 10;

    public DBHandler() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/postgres";
        Properties props = new Properties();
        props.setProperty("user","postgres");
        props.setProperty("password","filip123");
        props.setProperty("ssl","false");
        conn = DriverManager.getConnection(url, props);
    }

    public void getAllFromDB() {
        Statement st = null;
        try {
            st = conn.createStatement();

            ResultSet rs = st.executeQuery("SELECT * FROM users");
            while (rs.next())
            {
                System.out.print("Column 1 returned ");
                System.out.println(rs.getString(1));
            } rs.close();

            st.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createUser(Map<String, Object> parameters) throws Exception {
        if (!String.class.isInstance(parameters.get("email")) || !String.class.isInstance(parameters.get("title")) ||
                !String.class.isInstance(parameters.get("userId")) || !String.class.isInstance(parameters.get("firstname")) ||
                !String.class.isInstance(parameters.get("lastname")) || !String.class.isInstance(parameters.get("description")) ||
                !String.class.isInstance(parameters.get("university")) || !String.class.isInstance(parameters.get("password"))){

            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }else {
            createUser(parameters.get("email"), parameters.get("title"), parameters.get("userId"),
                    parameters.get("firstname"), parameters.get("lastname"), parameters.get("description"), parameters.get("university"),
                    parameters.get("password"));
        }
    }

    private void createUser(Object email, Object title, Object userId, Object firstname,
                           Object lastname, Object description, Object university, Object password) throws IOException, SQLException, NoSuchAlgorithmException {

        String imageId = createImage();
        String cryptPassword = Utilities.cryptWithMD5((String) password);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setString(1, (String)email);
        ps.setString(2, (String)title);
        ps.setString(3, (String)userId);
        ps.setString(4, (String)firstname);
        ps.setString(5, (String)lastname);
        ps.setString(6, (String)description);
        ps.setString(7, (String)university);
        ps.setString(8, imageId);
        ps.setString(9, cryptPassword);
        ps.executeUpdate();
        ps.close();

        //update image with foreign key (need this for cascade delete)
        ps = conn.prepareStatement("UPDATE image SET userref = ? WHERE id = ?");
        ps.setString(1, (String)email);
        ps.setString(2, imageId);
        ps.executeUpdate();
        ps.close();
    }

    public void deleteAllUsers() throws IOException, SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM users");

        ps.executeUpdate();
        ps.close();
    }

    public void deleteUserByEmail(Object email) throws Exception {
        if (String.class.isInstance(email)) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE email = ?");
            ps.setString(1, (String) email);
            ps.executeUpdate();
            ps.close();
        }else{
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

    public Object getUserByEmail(Object email) throws Exception {
        if (String.class.isInstance(email)) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
            ps.setString(1, (String) email);
            ResultSet rs = ps.executeQuery();
            return rs;
        }else{
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

    public String createImage() throws IOException, SQLException {
        Statement st = null;
        st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT uuid_generate_v4()");
        rs.next();
        String genuuid = rs.getString(1);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO image(id, defaultImage) VALUES (?, ?)");

        ps.setString(1, genuuid);
        ps.setBoolean(2, true);
//        File file = new File("giphy_s.gif");
//        FileInputStream fis = new FileInputStream(file);
//        ps.setBinaryStream(3, fis, (int)file.length());
//        fis.close();
        ps.executeUpdate();

        ps.close();

        return genuuid;
    }

    public String authenticateUser(Map<String, Object> parameters) throws SQLException, NoSuchAlgorithmException {
        if (String.class.isInstance(parameters.get("email")) && String.class.isInstance(parameters.get("password"))) {
            String email = (String) parameters.get("email");
            String password = (String) parameters.get("password");

            if (!email.equals("") && !password.equals("")){
                String cryptPassword = Utilities.cryptWithMD5(password);
                Statement st = conn.createStatement();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ? and password = ?");
                ps.setString(1, email);
                ps.setString(2, cryptPassword);
                ResultSet rs = ps.executeQuery();
                if (rs.next()){
                    //napravi token i vrati korisniku
                    return "OK";
                }else {
                    throw new RuntimeException(EXCEPTION_BADREQUEST);
                }
            }else{
                throw new RuntimeException(EXCEPTION_BADREQUEST);
            }
        }else {
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

}
