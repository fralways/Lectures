package filip.test;

import com.google.gson.Gson;
import com.sun.corba.se.spi.orbutil.fsm.Guard;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;
import org.postgresql.jdbc.PgArray;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 8/7/2016.
 */
public class DBHandler {

    private final Connection conn;

    public DBHandler() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/postgres";
        Properties props = new Properties();
        props.setProperty("user","postgres");
        props.setProperty("password","filip123");
        props.setProperty("ssl","false");
        props.setProperty("tcpKeepAlive", "true");
        props.put("autoReconnect", "true");
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

            if (Utilities.isValidEmailAddress((String) parameters.get("email"))){
                createUser(parameters.get("email"), parameters.get("title"), parameters.get("userId"),
                        parameters.get("firstname"), parameters.get("lastname"), parameters.get("description"), parameters.get("university"),
                        parameters.get("password"));
            }else {
                throw new RuntimeException(EXCEPTION_BADREQUEST);
            }


        }
    }

    private void createUser(Object email, Object title, Object userId, Object firstname,
                           Object lastname, Object description, Object university, Object password) throws IOException, SQLException, NoSuchAlgorithmException {

        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
        ps.setString(1, (String)email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()){
            rs.close();
            throw new SQLException(EXCEPTION_INTERNAL);
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

            List<HashMap<String, Object>> results = Utilities.convertResultSetToList(rs);
            HashMap<String, Object> user = results.get(0);
            PgArray lectures = (PgArray) user.get("lectures");
            if (lectures != null) {
                String[] lecturesArray = (String[]) lectures.getArray();
                //add lectures to USER object
                if (lecturesArray.length > 0) {
                    StringBuilder statement = new StringBuilder("SELECT * FROM lecture WHERE guid='");
                    for (int i = 0; i < lecturesArray.length; i++) {
                        if (i != 0) {
                            statement.append(" or guid='");
                        }
                        String lectureId = lecturesArray[i];
                        statement.append(lectureId);
                        statement.append("'");
                    }

                    ps = conn.prepareStatement(statement.toString());
                    rs = ps.executeQuery();

                    results = Utilities.convertResultSetToList(rs);
                    user.put("lectures", results);
                }
            }

            return user;
        }else{
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

    public void updateUserWithParams(String userId, Map<String, Object> params) throws Exception {

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
            //hash password
            if (key.equals("password")){
                value = Utilities.cryptWithMD5((String) value);
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

        statement.append(" WHERE guid='");
        statement.append(userId);
        statement.append("'");

        if (hasChange) {
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.executeUpdate();
        }
    }

    public String createImage() throws IOException, SQLException {
        Statement st;
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
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ? and password = ?");
                ps.setString(1, email);
                ps.setString(2, cryptPassword);
                ResultSet rs = ps.executeQuery();
                if (rs.next()){

//                    String userEmail = rs.getString("email");
                    String guid = rs.getString("guid");

                    //napravi token i vrati korisniku
                    byte[] key = JWT_SECRET.getBytes();

                    Date dt = new Date();
                    DateTime dtOrg = new DateTime(dt);
                    DateTime dtPlusOne = dtOrg.plusDays(1);

                    String jwt =
                            Jwts.builder().setIssuer("http://lectures.com")
                                    .setSubject(guid)
                                    .setExpiration(dtPlusOne.toDate())
                                    .signWith(SignatureAlgorithm.HS256,key)
                                    .compact();
                    return jwt;
                }else {
                    // invalid credentials
                    throw new RuntimeException(EXCEPTION_NOTAUTHORIZED);
                }
            }else{
                throw new RuntimeException(EXCEPTION_BADREQUEST);
            }
        }else {
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }


    public String createLecture(Map<String, Object> parameters, String userId) throws Exception {

        boolean badReq = false;
        if (!String.class.isInstance(parameters.get("title"))) {
            badReq = true;
        }else if (parameters.get("description") != null && !String.class.isInstance(parameters.get("description"))){
            badReq = true;
        }

        if (badReq){
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }else {
            //title,descr,id od 10brojeva
            String title = (String)parameters.get("title");
            String descr = (String)parameters.get("description");
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT uuid_generate_v4()");
            rs.next();
            String genuuid = rs.getString(1);

            PreparedStatement ps = conn.prepareStatement("INSERT INTO lecture(title, description, guid) VALUES (?, ?, ?)");
            ps.setString(1, title);
            ps.setString(2, descr);
            ps.setString(3, genuuid);
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement("update users set lectures = array_append(lectures, CAST (? AS TEXT )) where guid=?");
            ps.setString(1, genuuid);
            ps.setString(2, userId);
            ps.executeUpdate();
            ps.close();

            return genuuid;
        }
    }

    public void deleteLecture(Object id, String userId) throws SQLException{
        if (String.class.isInstance(id)){
            PreparedStatement ps = conn.prepareStatement("DELETE FROM lecture WHERE guid = ?");
            ps.setString(1, (String) id);
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement("update users set lectures = array_remove(lectures, CAST (? AS TEXT )) where guid=?");
            ps.setString(1, (String)id);
            ps.setString(2, userId);
            ps.executeUpdate();
            ps.close();
        }else {
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

    public Object getLecture(Object id) throws Exception {
        if (String.class.isInstance(id)){
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM lecture WHERE guid = ?");
            ps.setString(1, (String)id);
            ResultSet rs = ps.executeQuery();
            return rs;
        } else{
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

}
