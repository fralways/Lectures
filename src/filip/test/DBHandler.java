package filip.test;

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

            ResultSet rs = st.executeQuery("SELECT * FROM question");
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
                    ArrayList<Map<String, Lecture>> lecturesList = new ArrayList();
                    for (int i=0; i<results.size(); i++){
                        Map<String, Object> lecture = results.get(i);
                        lecture.remove("questions");
                    }
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

    public Lecture createLecture(Map<String, Object> parameters, String userId) throws Exception {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT uuid_generate_v4()");
        rs.next();
        String guid = rs.getString(1);
        Lecture lecture = new Lecture(parameters, guid);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO lecture(title, description, guid) VALUES (?, ?, ?)");
        ps.setString(1, lecture.title);
        ps.setString(2, lecture.description);
        ps.setString(3, lecture.guid);
        ps.executeUpdate();
        ps.close();

        ps = conn.prepareStatement("update users set lectures = array_append(lectures, CAST (? AS TEXT )) where guid=?");
        ps.setString(1, lecture.guid);
        ps.setString(2, userId);
        ps.executeUpdate();
        ps.close();

        return lecture;
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

    List<HashMap<String,Object>> getLectureQuestions(Lecture lecture) throws Exception{
        StringBuilder statement = new StringBuilder("SELECT * FROM question WHERE guid='");
        for (int i = 0; i < lecture.questionIds.size(); i++) {
            if (i != 0) {
                statement.append(" or guid='");
            }
            String lectureId = lecture.questionIds.get(i);
            statement.append(lectureId);
            statement.append("'");
        }

        PreparedStatement ps = conn.prepareStatement(statement.toString());
        ResultSet rs = ps.executeQuery();

        List<HashMap<String,Object>> questionsList = Utilities.convertResultSetToList(rs);
        for (int i=0; i<questionsList.size(); i++){
            HashMap<String,Object> question = questionsList.get(i);
            PgArray answersDB = (PgArray)question.get("answers");
            String[] answersStringArray = (String[]) answersDB.getArray();
            ArrayList<String> questions = new ArrayList<>();
            questions.addAll(Arrays.asList(answersStringArray));
            question.put("answers", questions);
        }

        return questionsList;
    }

    public Lecture getLecture(Object id) throws Exception {
        if (String.class.isInstance(id)){
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM lecture WHERE guid = ?");
            ps.setString(1, (String)id);
            ResultSet rs = ps.executeQuery();

            Lecture lecture = new Lecture(rs);
            List<HashMap<String,Object>> questions = getLectureQuestions(lecture);
            lecture.updateQuestionsWithPulledValues(questions);
            return lecture;
        } else{
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }

    public void updateLectureWithParams(Map<String, Object> params) throws Exception {

        //provera se da li postoje na ovaj nacin
        Lecture lecture = getLecture("f2584c52-ed7a-4242-ad74-59c5b962f811");
        Question question = getQuestion("b1a1dcb0-4f3e-42bf-9b72-d3a30093f9cf");

        if (lecture != null && question != null){
            PreparedStatement ps = conn.prepareStatement("update lecture set questions = array_append(questions, CAST (? AS TEXT )) where guid=?");
            ps.setString(1, question.guid);
            ps.setString(2, lecture.guid);
            ps.executeUpdate();
            ps.close();
        }else {
            throw new Exception(EXCEPTION_BADREQUEST);
        }



//        StringBuilder statement = new StringBuilder("UPDATE users SET ");
//
//        Set<String> allowedFields = new HashSet<>(
//                Arrays.asList("firstname", "lastname", "description", "title", "password", "university"));
//
//        boolean hasChange = false;
//
//        for(Map.Entry<String, Object> entry : params.entrySet()) {
//            String key = entry.getKey();
//            Object value = entry.getValue();
//
//            //check if field is modifiable
//            if (!allowedFields.contains(key)){
//                continue;
//            }
//            //hash password
//            if (key.equals("password")){
//                value = Utilities.cryptWithMD5((String) value);
//            }
//
//            if (statement.length() > "UPDATE users SET ".length()){
//                statement.append(",");
//            }
//
//            statement.append(key);
//            statement.append("='");
//            statement.append(value);
//            statement.append("'");
//
//            hasChange = true;
//        }
//
//        statement.append(" WHERE guid='");
//        statement.append(userId);
//        statement.append("'");
//
//        if (hasChange) {
//            PreparedStatement ps = conn.prepareStatement(statement.toString());
//            ps.executeUpdate();
//        }
    }

    public String createQuestion(Map<String, Object> parameters) throws Exception{
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT uuid_generate_v4()");
        rs.next();
        String guid = rs.getString(1);
        Question question = new Question(parameters, guid);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO question(guid, question, correctIndex, duration, answers) VALUES (?, ?, ?, ?, ?)");
//        PreparedStatement ps = conn.prepareStatement("INSERT INTO question(guid, question, correctIndex, duration) VALUES (?, ?, ?, ?)");
        ps.setString(1, question.guid);
        ps.setString(2, question.question);
        ps.setInt(3, question.correctIndex);
        ps.setInt(4, question.duration);

        final String[] data = question.answers.toArray(new String[question.answers.size()]);
        final java.sql.Array sqlArray = conn.createArrayOf("VARCHAR", data);
        ps.setArray(5, sqlArray);
        ps.executeUpdate();
        ps.close();

        return question.guid;
    }

    public Question getQuestion(Object id) throws Exception {
        if (String.class.isInstance(id)){
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM question WHERE guid = ?");
            ps.setString(1, (String)id);
            ResultSet rs = ps.executeQuery();
            Question question = new Question(rs);
            return question;
        } else{
            throw new RuntimeException(EXCEPTION_BADREQUEST);
        }
    }
}
