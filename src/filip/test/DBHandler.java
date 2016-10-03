package filip.test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;

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

        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
        ps.setString(1, (String)email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()){
            rs.close();
            throw new SQLException();
        }else {
            String cryptPassword = Utilities.cryptWithMD5((String) password);
            String guid = Utilities.cryptWithMD5((String) email);

            String imageId = createImage();
            ps = conn.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, (String) email);
            ps.setString(2, (String) title);
            ps.setString(3, (String) userId);
            ps.setString(4, (String) firstname);
            ps.setString(5, (String) lastname);
            ps.setString(6, (String) description);
            ps.setString(7, (String) university);
            ps.setString(8, imageId);
            ps.setString(9, cryptPassword);
            ps.setString(10, guid);
            ps.executeUpdate();
            ps.close();

            //update image with foreign key (need this for cascade delete)
            ps = conn.prepareStatement("UPDATE image SET userref = ? WHERE id = ?");
            ps.setString(1, guid);
            ps.setString(2, imageId);
            ps.executeUpdate();
            ps.close();
        }
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

    public void updateUserWithParams(String email, Map<String, Object> params) throws Exception {

        StringBuilder statement = new StringBuilder("UPDATE users SET ");

        Set<String> allowedFields = new HashSet<>(
                Arrays.asList("firstname", "lastname", "description", "title", "password", "university"));

        boolean hasChange = false;

        for(Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            //check if field is modifiable
            if (!allowedFields.contains(key)){
                continue;
            }

            if (statement.length() > "UPDATE users SET ".length()){
                statement.append(",");
            }

            statement.append(key);
            statement.append("='");
            statement.append(value);
            statement.append("'");

            hasChange = true;
        }

        statement.append(" WHERE email='");
        statement.append(email);
        statement.append("'");

        if (hasChange) {
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.executeUpdate();
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

                    String userEmail = rs.getString("email");

                    //napravi token i vrati korisniku
                    byte[] key = JWT_SECRET.getBytes();

                    Date dt = new Date();
                    DateTime dtOrg = new DateTime(dt);
                    DateTime dtPlusOne = dtOrg.plusDays(1);

                    String jwt =
                            Jwts.builder().setIssuer("http://lectures.com")
                                    .setSubject(userEmail)
                                    .setExpiration(dtPlusOne.toDate())
                                    .signWith(SignatureAlgorithm.HS256,key)
                                    .compact();
                    return jwt;
                }else {
                    // invalid credentials
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
