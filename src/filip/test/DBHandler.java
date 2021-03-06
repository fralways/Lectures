package filip.test;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;
import org.postgresql.jdbc.PgArray;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 8/7/2016.
 */
public class DBHandler {

    String host = "localhost";
    int port = 5432;

//    private Connection conn;
    private ComboPooledDataSource cpds;

    DBHandler() throws ClassNotFoundException, SQLException, PropertyVetoException {
        connect(host, port);
        cleanupDB();
    }

    DBHandler(String host, int port) throws ClassNotFoundException, SQLException, PropertyVetoException {
        this.port = port;
        this.host = host;
        connect(host, port);
        cleanupDB();
    }

    private void closeConnections(Connection conn){
        closeConnections(null, null, null, conn);
    }

    private void closeConnections(Statement st, Connection conn){
        closeConnections(null, null, st, conn);
    }

    private void closeConnections(ResultSet rs, Connection conn){
        closeConnections(rs, null, null, conn);
    }

    private void closeConnections(ResultSet rs, PreparedStatement ps, Connection conn){
        closeConnections(rs, ps, null, conn);
    }

    private void closeConnections(ResultSet rs, PreparedStatement ps, Statement st, Connection conn){
        if (rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                Utilities.printLog(e.toString());
            }
        }
        if (ps != null){
            try {
                ps.close();
            } catch (SQLException e) {
                Utilities.printLog(e.toString());
            }
        }
        if (st != null){
            try {
                st.close();
            } catch (SQLException e) {
                Utilities.printLog(e.toString());
            }
        }
        if (conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                Utilities.printLog(e.toString());
            }
        }
    }

    private void connect(String host, int port) throws ClassNotFoundException, SQLException, PropertyVetoException {
//        Class.forName("org.postgresql.Driver");
//        String url = "jdbc:postgresql://"+ host + ":" + port + "/postgres?currentSchema=lectures";
//        Properties props = new Properties();
//        props.setProperty("user","postgres");
//        props.setProperty("password","filip123");
//        props.setProperty("ssl","false");
//        props.setProperty("tcpKeepAlive", "true");
//        props.put("autoReconnect", "true");
//        conn = DriverManager.getConnection(url, props);

        cpds = new ComboPooledDataSource();
        cpds.setDriverClass("org.postgresql.Driver"); //loads the jdbc driver
        String url = "jdbc:postgresql://"+ host + ":" + port + "/postgres?currentSchema=lectures";
        cpds.setJdbcUrl(url);
        cpds.setUser("postgres");
        cpds.setPassword("filip123");
    }

    private void cleanupDB() {
        Utilities.printLog(this, "DB cleanup started");

        Connection conn = null;
        try {
            conn = cpds.getConnection();
            PreparedStatement ps = conn.prepareStatement("update users set runninglecture = null");
            ps.executeUpdate();
            ps.close();
            Utilities.printLog(this, "set no running lecture to all users");

            ps = conn.prepareStatement("delete from question where owner IS null");
            int count = ps.executeUpdate();
            ps.close();
            Utilities.printLog(this, "cleaned questions without owner: " + count);

            ps = conn.prepareStatement("delete from listener_question");
            count = ps.executeUpdate();
            ps.close();
            Utilities.printLog(this, "cleaned listener questions: " + count);

            Utilities.printLog(this, "DB cleanup end");
        } catch (SQLException e) {
            Utilities.printLog(e.toString());
        } finally {
            closeConnections(conn);
        }
    }

    public void getAllFromDB() {
        Statement st = null;
        Connection conn = null;
        try {
            conn = cpds.getConnection();
            st = conn.createStatement();

            ResultSet rs = st.executeQuery("SELECT * FROM question");
            while (rs.next())
            {
                System.out.print("Column 1 returned ");
                System.out.println(rs.getString(1));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnections(st, conn);
        }
    }

    public void createUser(Map<String, Object> parameters) throws ExceptionHandler {
        if (!String.class.isInstance(parameters.get("email")) || !String.class.isInstance(parameters.get("title")) ||
                !String.class.isInstance(parameters.get("userId")) || !String.class.isInstance(parameters.get("firstname")) ||
                !String.class.isInstance(parameters.get("lastname")) || !String.class.isInstance(parameters.get("description")) ||
                !String.class.isInstance(parameters.get("university")) || !String.class.isInstance(parameters.get("password"))){

            throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
        }else {

            if (Utilities.isValidEmailAddress((String) parameters.get("email"))){
                createUser(parameters.get("email"), parameters.get("title"), parameters.get("userId"),
                        parameters.get("firstname"), parameters.get("lastname"), parameters.get("description"), parameters.get("university"),
                        parameters.get("password"));
            }else {
                throw new ExceptionHandler("email not in correct format", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }
    }

    private void createUser(Object email, Object title, Object userId, Object firstname,
                           Object lastname, Object description, Object university, Object password) throws ExceptionHandler {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = cpds.getConnection();
            ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
            ps.setString(1, (String) email);
            rs = ps.executeQuery();
            if (rs.next()) {
                throw new ExceptionHandler("user already exist", HttpURLConnection.HTTP_INTERNAL_ERROR);
            } else {
                String cryptPassword = Utilities.cryptWithMD5((String) password);
                String guid = Utilities.cryptWithMD5((String) email);

                String imageId = createImage();
                ps.close();
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
            }
        }catch (SQLException|NoSuchAlgorithmException|IOException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        } finally {
            closeConnections(rs, ps, conn);
        }
    }

    public void deleteUserByGuid(String guid) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = cpds.getConnection();
            ps = conn.prepareStatement("select * FROM users WHERE guid = ?");
            ps.setString(1, guid);
            rs = ps.executeQuery();

            List<HashMap<String,Object>> userList = Utilities.convertResultSetToList(rs);
            if (userList.size() > 0){
                HashMap<String,Object> user = userList.get(0);
                String userId = (String) user.get("guid");
                PgArray lectures = (PgArray)user.get("lectures");
                String[] lecturesArray = (String[]) lectures.getArray();
                for (String lectureId : lecturesArray){
                    deleteLecture(lectureId, userId);
                }

                ps = conn.prepareStatement("DELETE FROM users WHERE guid = ?");
                ps.setString(1, guid);
                ps.executeUpdate();

            }else {
                throw new ExceptionHandler("user does not exist", HttpURLConnection.HTTP_NOT_FOUND);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public Object getUserByEmail(Object email) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (String.class.isInstance(email)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
                ps.setString(1, (String) email);
                rs = ps.executeQuery();

                List<HashMap<String, Object>> results = Utilities.convertResultSetToList(rs);

                if (results != null && results.size() > 0) {
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

                            ps.close();
                            ps = conn.prepareStatement(statement.toString());
                            rs.close();
                            rs = ps.executeQuery();

                            results = Utilities.convertResultSetToList(rs);
                            ArrayList<Map<String, Lecture>> lecturesList = new ArrayList();
                            for (int i = 0; i < results.size(); i++) {
                                Map<String, Object> lecture = results.get(i);
                                lecture.remove("questions");
                            }
                            user.put("lectures", results);
                        }else{
                            user.remove("lectures");
                        }
                    }
                    return user;
                }else {
                    throw new ExceptionHandler("user does not exist", HttpURLConnection.HTTP_NOT_FOUND);
                }
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public Boolean checkIfUserExists(String guid) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (guid != null) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("select exists(select 1 from users where guid = ?) AS \"exists\"");
                ps.setString(1, guid);
                rs = ps.executeQuery();
                List<HashMap<String,Object>> list = Utilities.convertResultSetToList(rs);
                HashMap<String,Object> result = list.get(0);
                return (Boolean) result.get("exists");
            }else {
                throw new ExceptionHandler("bad guid", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public void updateUserWithParams(String userId, Map<String, Object> params) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            Set<String> allowedFields = new HashSet<>(
                    Arrays.asList("firstname", "lastname", "description", "title", "password", "university"));

            String querry = makePatchDBQuerry("users", allowedFields, params, userId);
            if (querry != null) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement(querry);
                ps.executeUpdate();
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(ps, conn);
        }
    }

    public void updateUserWithRunningLecture(String userId, String uniqueId, boolean delete) throws ExceptionHandler{
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if (userId != null) {
                conn = cpds.getConnection();
                if (delete) {
                    ps = conn.prepareStatement("UPDATE users SET runninglecture = null WHERE guid = ? and runninglecture = ?");
                    ps.setString(1, userId);
                    ps.setString(2, uniqueId);
                    ps.executeUpdate();
                }else {
                    ps = conn.prepareStatement("UPDATE users SET runninglecture = ? WHERE guid = ?");
                    ps.setString(1, uniqueId);
                    ps.setString(2, userId);
                    ps.executeUpdate();
                }
            }else {
                throw new ExceptionHandler("bad user", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(ps, conn);
        }
    }

    public Boolean checkIfUserHasRunningLecture(String userId) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (userId != null) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("select exists(select 1 from users where runninglecture is not null and guid = ?) AS \"exists\"");
                ps.setString(1, userId);
                rs = ps.executeQuery();
                List<HashMap<String,Object>> list = Utilities.convertResultSetToList(rs);
                HashMap<String,Object> result = list.get(0);
                return (Boolean) result.get("exists");
            }else {
                throw new ExceptionHandler("bad guid", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public Boolean checkIfUserIsOwnerOfLecture(String userId, String uniqueId) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (userId != null && uniqueId != null) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("select exists(select 1 from lecture where owner = ? and unique_id = ?) AS \"exists\"");
                ps.setString(1, userId);
                ps.setString(2, uniqueId);
                rs = ps.executeQuery();
                List<HashMap<String,Object>> list = Utilities.convertResultSetToList(rs);
                HashMap<String,Object> result = list.get(0);
                return (Boolean) result.get("exists");
            }else {
                throw new ExceptionHandler("bad user or lecture id", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public String createImage() throws IOException, SQLException {
        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        conn = cpds.getConnection();
        st = conn.createStatement();
        rs = st.executeQuery("SELECT uuid_generate_v4()");
        rs.next();
        String genuuid = rs.getString(1);

        ps = conn.prepareStatement("INSERT INTO image(id, defaultImage) VALUES (?, ?)");

        ps.setString(1, genuuid);
        ps.setBoolean(2, true);
        ps.executeUpdate();

        closeConnections(rs, ps, st, conn);

        return genuuid;
    }

    public String authenticateUser(Map<String, Object> parameters) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        if (String.class.isInstance(parameters.get("email")) && String.class.isInstance(parameters.get("password"))) {
            String email = (String) parameters.get("email");
            String password = (String) parameters.get("password");

            if (!email.equals("") && !password.equals("")){
                try {
                    String cryptPassword = Utilities.cryptWithMD5(password);
                    conn = cpds.getConnection();
                    ps = conn.prepareStatement("SELECT * FROM users WHERE email = ? and password = ?");
                    ps.setString(1, email);
                    ps.setString(2, cryptPassword);
                    rs = ps.executeQuery();
                    if (rs.next()) {
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
                                        .signWith(SignatureAlgorithm.HS256, key)
                                        .compact();
                        return jwt;
                    } else {
                        // invalid credentials
                        throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
                    }
                }catch (SQLException|NoSuchAlgorithmException e){
                    throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
                }finally {
                    closeConnections(rs, ps, conn);
                }
            }else{
                throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }else {
            throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }

    public Lecture createLecture(Map<String, Object> parameters, String userId) throws ExceptionHandler {
        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = cpds.getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT uuid_generate_v4()");
            rs.next();
            String guid = rs.getString(1);
            Lecture lecture = new Lecture(parameters, guid);

            String uniqueid = Utilities.generateLectureString();
            boolean exists;
            do {
                ps = conn.prepareStatement("select exists(select 1 from lecture where unique_id = ?) AS \"exists\"");
                ps.setString(1, uniqueid);
                rs = ps.executeQuery();
                List<HashMap<String,Object>> list = Utilities.convertResultSetToList(rs);
                HashMap<String,Object> result = list.get(0);
                exists = (Boolean) result.get("exists");
                ps.close();
                ps = null;
            }while (exists);

            ps = conn.prepareStatement("INSERT INTO lecture(title, description, guid, unique_id, owner) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, lecture.getTitle());
            ps.setString(2, lecture.getDescription());
            ps.setString(3, lecture.getGuid());
            ps.setString(4, uniqueid);
            ps.setString(5, userId);
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement("update users set lectures = array_append(lectures, CAST (? AS TEXT )) where guid=?");
            ps.setString(1, lecture.getGuid());
            ps.setString(2, userId);
            ps.executeUpdate();

            return lecture;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }catch (ExceptionHandler e){
            throw e;
        }finally {
            closeConnections(rs, ps, st, conn);
        }
    }

    public void deleteLecture(Object id, String userId) throws ExceptionHandler{
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (String.class.isInstance(id)){
                conn = cpds.getConnection();
                ps = conn.prepareStatement("update users set lectures = array_remove(lectures, CAST (? AS TEXT )) where guid=?");
                ps.setString(1, (String)id);
                ps.setString(2, userId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("select * from lecture where guid=?");
                ps.setString(1, (String)id);
                rs = ps.executeQuery();

                Lecture lecture = new Lecture(rs);
                if (lecture != null && lecture.questionIds != null && lecture.questionIds.size() > 0) {
                    StringBuilder query = new StringBuilder("delete from question where guid in (");
                    for (int i = 0; i < lecture.questionIds.size(); i++) {
                        if (i!=0){
                            query.append(",");
                        }
                        query.append("'").append(lecture.questionIds.get(i)).append("'");
                    }
                    query.append(")");
                    ps.close();
                    ps = conn.prepareStatement(query.toString());
                    ps.executeUpdate();
                }

                ps.close();
                ps = conn.prepareStatement("DELETE FROM lecture WHERE guid = ?");
                ps.setString(1, (String) id);
                ps.executeUpdate();
            }else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }
        catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    List<HashMap<String,Object>> getLectureQuestions(Lecture lecture) throws ExceptionHandler{
        StringBuilder statement = new StringBuilder("SELECT * FROM question WHERE guid='");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (lecture.questionIds != null) {
                for (int i = 0; i < lecture.questionIds.size(); i++) {
                    if (i != 0) {
                        statement.append(" or guid='");
                    }
                    String lectureId = lecture.questionIds.get(i);
                    statement.append(lectureId);
                    statement.append("'");
                }

                conn = cpds.getConnection();
                ps = conn.prepareStatement(statement.toString());
                rs = ps.executeQuery();

                List<HashMap<String, Object>> questionsList = Utilities.convertResultSetToList(rs);
                for (int i = 0; i < questionsList.size(); i++) {
                    HashMap<String, Object> question = questionsList.get(i);
                    PgArray answersDB = (PgArray) question.get("answers");
                    String[] answersStringArray = (String[]) answersDB.getArray();
                    ArrayList<String> questions = new ArrayList<>();
                    questions.addAll(Arrays.asList(answersStringArray));
                    question.put("answers", questions);
                }

                return questionsList;
            } else {
                return null;
            }
        }catch (Exception e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public Lecture getLecture(Object id) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (String.class.isInstance(id)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("SELECT * FROM lecture WHERE guid = ?");
                ps.setString(1, (String) id);
                rs = ps.executeQuery();

                Lecture lecture = new Lecture(rs);
                List<HashMap<String, Object>> questions = getLectureQuestions(lecture);
                lecture.updateQuestionsWithPulledValues(questions);
                return lecture;
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public Lecture getLectureFromUniqueId(Object uniqueid) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (String.class.isInstance(uniqueid)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("SELECT * FROM lecture WHERE unique_id = ?");
                ps.setString(1, (String) uniqueid);
                rs = ps.executeQuery();

                Lecture lecture = new Lecture(rs);
                List<HashMap<String, Object>> questions = getLectureQuestions(lecture);
                lecture.updateQuestionsWithPulledValues(questions);
                return lecture;
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }


    public void updateLectureWithParams(Map<String, Object> params) throws ExceptionHandler {
        String op = (String) params.get("op");
        String path = (String) params.get("path");
        Map<String, Object> parameters = (Map<String, Object>)params.get("parameters");
        if (op != null && path != null && parameters != null){
            switch (op) {
                case "add": {
                    String lectureId = path;
                    String questionId = (String) parameters.get("questionId");

                    if (lectureId != null && questionId != null) {
                        //proverava se da li postoje na ovaj nacin
                        Lecture lecture = getLecture(lectureId);
                        Question question = getQuestion(questionId);

                        if (lecture != null && question != null) {
                            Connection conn = null;
                            PreparedStatement ps = null;
                            try {
                                conn = cpds.getConnection();
                                ps = conn.prepareStatement("update lecture set questions = array_append(questions, CAST (? AS TEXT )) where guid=?");
                                ps.setString(1, question.getGuid());
                                ps.setString(2, lecture.getGuid());
                                ps.executeUpdate();
                                ps.close();

                                ps = conn.prepareStatement("update question set owner = ? where guid = ?");
                                ps.setString(1, lecture.getGuid());
                                ps.setString(2, question.getGuid());
                                ps.executeUpdate();
                            }catch (SQLException e){
                                throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
                            }finally {
                                closeConnections(ps, conn);
                            }
                        } else {
                            throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
                        }
                    } else {
                        throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
                    }

                    break;
                }
                case "remove": {
                    String lectureId = path;
                    String questionId = (String) parameters.get("questionId");

                    if (lectureId != null && questionId != null) {
                        //provera se da li postoje na ovaj nacin
                        Lecture lecture = getLecture(lectureId);
                        Question question = getQuestion(questionId);

                        if (lecture != null && question != null) {
                            Connection conn = null;
                            PreparedStatement ps = null;
                            try {
                                conn = cpds.getConnection();
                                ps = conn.prepareStatement("update lecture set questions = array_remove(questions, CAST (? AS TEXT )) where guid=?");
                                ps.setString(1, question.getGuid());
                                ps.setString(2, lecture.getGuid());
                                ps.executeUpdate();
                                ps.close();

                                ps = conn.prepareStatement("DELETE FROM question WHERE guid = ?");
                                ps.setString(1, question.getGuid());
                                ps.executeUpdate();
                            }catch (SQLException e){
                                throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
                            }finally {
                                closeConnections(ps, conn);
                            }
                        } else {
                            throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
                        }
                    } else {
                        throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
                    }

                    break;
                }
                case "replace": {
                    Connection conn = null;
                    PreparedStatement ps = null;
                    try {
                        Set<String> allowedFields = new HashSet<>(
                                Arrays.asList("title", "description"));
                        String query = makePatchDBQuerry("lecture", allowedFields, parameters, path);
                        if (query != null) {
                            conn = cpds.getConnection();
                            ps = conn.prepareStatement(query);
                            ps.executeUpdate();
                        }
                    }catch (SQLException e){
                        throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
                    }finally {
                        closeConnections(ps, conn);
                    }

                    break;
                }
                default: {
                    throw new ExceptionHandler("op method not supported", HttpURLConnection.HTTP_BAD_REQUEST);
                }
            }
        }else{
            throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }

    public String createQuestion(Map<String, Object> parameters) throws ExceptionHandler{
        Connection conn = null;
        PreparedStatement ps = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = cpds.getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT uuid_generate_v4()");
            rs.next();
            String guid = rs.getString(1);
            Question question = new Question(parameters, guid);

            ps = conn.prepareStatement("INSERT INTO question(guid, question, correctindex, duration, answers) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, question.getGuid());
            ps.setString(2, question.getQuestion());
            ps.setInt(3, question.getCorrectIndex());
            ps.setInt(4, question.getDuration());

            final String[] data = question.getAnswers().toArray(new String[question.getAnswers().size()]);
            final java.sql.Array sqlArray = conn.createArrayOf("VARCHAR", data);
            ps.setArray(5, sqlArray);
            ps.executeUpdate();
            return question.getGuid();
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e) {
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, st, conn);
        }
    }

    public Question getQuestion(Object id) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (String.class.isInstance(id)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("SELECT * FROM question WHERE guid = ?");
                ps.setString(1, (String) id);
                rs = ps.executeQuery();
                Question question = new Question(rs);
                return question;
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public void updateQuestionWithParams(Map<String, Object> params) throws ExceptionHandler {
        String op = (String) params.get("op");
        String path = (String) params.get("path");
        Map<String, Object> parameters = (Map<String, Object>)params.get("parameters");
        if (op != null && path != null && parameters != null){
            switch (op) {
                case "replace": {
                    Connection conn = null;
                    PreparedStatement ps = null;
                    try {
                        ArrayList<String> answers = (ArrayList<String>) parameters.get("answers");
                        //update answer array
                        if (answers != null && answers.size() > 0) {
                            final String[] data = answers.toArray(new String[answers.size()]);
                            final java.sql.Array sqlArray = conn.createArrayOf("VARCHAR", data);
                            parameters.put("answers", sqlArray);
                        }
                        //now update fields
                        Set<String> allowedFields = new HashSet<>(
                                Arrays.asList("question", "correctindex", "duration", "answers"));
                        String query = makePatchDBQuerry("question", allowedFields, parameters, path);
                        if (query != null) {
                            conn = cpds.getConnection();
                            ps = conn.prepareStatement(query);
                            ps.executeUpdate();
                        }
                    }catch (SQLException e){
                        throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
                    }finally {
                        closeConnections(ps, conn);
                    }
                    break;
                }
                default: {
                    throw new ExceptionHandler("op method not supported", HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }else{
            throw new ExceptionHandler(EXCEPTION_BADREQUEST, HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }

    public void deleteQuestion(Object id, Object lectureId) throws ExceptionHandler{
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if (String.class.isInstance(id) && String.class.isInstance(lectureId)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("update lecture set questions = array_remove(questions, CAST (? AS TEXT )) where guid=?");
                ps.setString(1, (String) id);
                ps.setString(2, (String) lectureId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("DELETE FROM question WHERE guid = ?");
                ps.setString(1, (String) id);
                ps.executeUpdate();
            } else {
                throw new ExceptionHandler(EXCEPTION_BADREQUEST, HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(ps, conn);
        }
    }

    public ListenerQuestion createListenerQuestion(Map<String, Object> parameters, String lectureId) throws ExceptionHandler{
        Connection conn = null;
        PreparedStatement ps = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = cpds.getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT uuid_generate_v4()");
            rs.next();
            String guid = rs.getString(1);
            Date dateNow = new Date();
            ListenerQuestion question = new ListenerQuestion(parameters, guid, lectureId, dateNow, false);

            ps = conn.prepareStatement("INSERT INTO listener_question(guid, question, lecture_id, date) VALUES (?, ?, ?, to_timestamp(?, 'YYYY-DD-MM HH24:MI:SS.MS'))");
            ps.setString(1, question.getGuid());
            ps.setString(2, question.getQuestion());
            ps.setString(3, question.getLectureId());

            Timestamp timestamp = new Timestamp(dateNow.getTime());
            String timestampValue = timestamp.toString();
            ps.setString(4, timestampValue);

            ps.executeUpdate();
            return question;
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e) {
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, st, conn);
        }
    }

    public List<ListenerQuestion> getListenerQuestions(Object lectureId) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (String.class.isInstance(lectureId)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("SELECT * FROM listener_question WHERE lecture_id = ?");
                ps.setString(1, (String) lectureId);
                rs = ps.executeQuery();
                List<ListenerQuestion> questions = ListenerQuestion.getQuestions(rs);

                return questions;
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public ListenerQuestion getListenerQuestion(String id) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = cpds.getConnection();
            ps = conn.prepareStatement("SELECT * FROM listener_question WHERE guid = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            ListenerQuestion question = new ListenerQuestion(rs);
            return question;
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(rs, ps, conn);
        }
    }

    public void setSharedListenerQuestion(String id) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = cpds.getConnection();
            ps = conn.prepareStatement("update listener_question set shared = true where guid = ?");
            ps.setString(1, id);
            ps.executeUpdate();
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(ps, conn);
        }
    }

    public void removeListenerQuestions(Object lectureId) throws ExceptionHandler {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if (String.class.isInstance(lectureId)) {
                conn = cpds.getConnection();
                ps = conn.prepareStatement("DELETE FROM listener_question WHERE lecture_id = ?");
                ps.setString(1, (String) lectureId);
                ps.executeUpdate();
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }finally {
            closeConnections(ps, conn);
        }
    }

    private String makePatchDBQuerry(String tableName, Set<String> allowedFields, Map<String, Object> params, String guid){
        StringBuilder statement = new StringBuilder("UPDATE ");
        statement.append(tableName);
        statement.append(" SET ");

        boolean hasChange = false;

        int startStatementLength = statement.length();

        for(Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            //check if field is modifiable
            if (!allowedFields.contains(key)){
                continue;
            }

            if (statement.length() > startStatementLength){
                statement.append(",");
            }

            statement.append(key);
            statement.append("='");
            if (value instanceof Double){
                statement.append(((Double) value).intValue());
            }else {
                statement.append(value);
            }
            statement.append("'");

            hasChange = true;
        }

        statement.append(" WHERE guid='");
        statement.append(guid);
        statement.append("'");

        if (!hasChange){
            return null;
        }

        return statement.toString();
    }
}
