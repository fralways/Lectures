package filip.test;

import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;
import org.postgresql.jdbc.PgArray;

import javax.rmi.CORBA.Util;
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

        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
            ps.setString(1, (String) email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                rs.close();
                throw new ExceptionHandler("internal error", HttpURLConnection.HTTP_INTERNAL_ERROR);
            } else {
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
        }catch (SQLException|NoSuchAlgorithmException|IOException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    public void deleteUserByEmail(Object email) throws ExceptionHandler {
        try {
            if (String.class.isInstance(email)) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE email = ?");
                ps.setString(1, (String) email);
                ps.executeUpdate();
                ps.close();
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    public Object getUserByEmail(Object email) throws ExceptionHandler {
        try {
            if (String.class.isInstance(email)) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
                ps.setString(1, (String) email);
                ResultSet rs = ps.executeQuery();

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

                            ps = conn.prepareStatement(statement.toString());
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
                    throw new ExceptionHandler("user does not exist", HttpURLConnection.HTTP_BAD_REQUEST);
                }
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    public void updateUserWithParams(String userId, Map<String, Object> params) throws ExceptionHandler {

        try {
            Set<String> allowedFields = new HashSet<>(
                    Arrays.asList("firstname", "lastname", "description", "title", "password", "university"));

            String querry = makePatchDBQuerry("users", allowedFields, params, userId);
            if (querry != null) {
                PreparedStatement ps = conn.prepareStatement(querry);
                ps.executeUpdate();
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
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

    public String authenticateUser(Map<String, Object> parameters) throws ExceptionHandler {
        if (String.class.isInstance(parameters.get("email")) && String.class.isInstance(parameters.get("password"))) {
            String email = (String) parameters.get("email");
            String password = (String) parameters.get("password");

            if (!email.equals("") && !password.equals("")){
                try {
                    String cryptPassword = Utilities.cryptWithMD5(password);
                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ? and password = ?");
                    ps.setString(1, email);
                    ps.setString(2, cryptPassword);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {

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
                                        .signWith(SignatureAlgorithm.HS256, key)
                                        .compact();
                        return jwt;
                    } else {
                        // invalid credentials
                        throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
                    }
                }catch (SQLException|NoSuchAlgorithmException e){
                    throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
                }
            }else{
                throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }else {
            throw new ExceptionHandler("bad username or password", HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }

    public Lecture createLecture(Map<String, Object> parameters, String userId) throws ExceptionHandler {
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT uuid_generate_v4()");
            rs.next();
            String guid = rs.getString(1);
            Lecture lecture = new Lecture(parameters, guid);


            String uniqueid = Utilities.generateLectureString();
            boolean exists;
            do {
                PreparedStatement ps = conn.prepareStatement("select exists(select 1 from lecture where unique_id = ?) AS \"exists\"");
                ps.setString(1, uniqueid);
                rs = ps.executeQuery();
                List<HashMap<String,Object>> list = Utilities.convertResultSetToList(rs);
                HashMap<String,Object> result = list.get(0);
                exists = (Boolean) result.get("exists");
            }while (exists);

            PreparedStatement ps = conn.prepareStatement("INSERT INTO lecture(title, description, guid, unique_id) VALUES (?, ?, ?, ?)");
            ps.setString(1, lecture.title);
            ps.setString(2, lecture.description);
            ps.setString(3, lecture.guid);
            ps.setString(4, uniqueid);
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement("update users set lectures = array_append(lectures, CAST (? AS TEXT )) where guid=?");
            ps.setString(1, lecture.guid);
            ps.setString(2, userId);
            ps.executeUpdate();
            ps.close();

            return lecture;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }catch (ExceptionHandler e){
            throw e;
        }
    }

    public void deleteLecture(Object id, String userId) throws ExceptionHandler{
        try {
            if (String.class.isInstance(id)){
                PreparedStatement ps = conn.prepareStatement("update users set lectures = array_remove(lectures, CAST (? AS TEXT )) where guid=?");
                ps.setString(1, (String)id);
                ps.setString(2, userId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("select * from lecture where guid=?");
                ps.setString(1, (String)id);
                ResultSet rs = ps.executeQuery();

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
                    ps = conn.prepareStatement(query.toString());
                    ps.executeUpdate();
                    ps.close();
                }

                ps = conn.prepareStatement("DELETE FROM lecture WHERE guid = ?");
                ps.setString(1, (String) id);
                ps.executeUpdate();
                ps.close();
            }else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }
        catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    List<HashMap<String,Object>> getLectureQuestions(Lecture lecture) throws ExceptionHandler{
        StringBuilder statement = new StringBuilder("SELECT * FROM question WHERE guid='");

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

                PreparedStatement ps = conn.prepareStatement(statement.toString());
                ResultSet rs = ps.executeQuery();

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
        }
    }

    public Lecture getLecture(Object id) throws ExceptionHandler {
        try {
            if (String.class.isInstance(id)) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM lecture WHERE guid = ?");
                ps.setString(1, (String) id);
                ResultSet rs = ps.executeQuery();

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
                        //provera se da li postoje na ovaj nacin
                        Lecture lecture = getLecture(lectureId);
                        Question question = getQuestion(questionId);

                        if (lecture != null && question != null) {
                            try {
                                PreparedStatement ps = conn.prepareStatement("update lecture set questions = array_append(questions, CAST (? AS TEXT )) where guid=?");
                                ps.setString(1, question.guid);
                                ps.setString(2, lecture.guid);
                                ps.executeUpdate();
                                ps.close();
                            }catch (SQLException e){
                                throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
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
                            try {
                                PreparedStatement ps = conn.prepareStatement("update lecture set questions = array_remove(questions, CAST (? AS TEXT )) where guid=?");
                                ps.setString(1, question.guid);
                                ps.setString(2, lecture.guid);
                                ps.executeUpdate();
                                ps.close();

                                ps = conn.prepareStatement("DELETE FROM question WHERE guid = ?");
                                ps.setString(1, question.guid);
                                ps.executeUpdate();
                                ps.close();
                            }catch (SQLException e){
                                throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
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
                    try {
                        Set<String> allowedFields = new HashSet<>(
                                Arrays.asList("title", "description"));
                        String query = makePatchDBQuerry("lecture", allowedFields, parameters, path);
                        if (query != null) {
                            PreparedStatement ps = conn.prepareStatement(query);
                            ps.executeUpdate();
                            ps.close();
                        }
                    }catch (SQLException e){
                        throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
                    }
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
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT uuid_generate_v4()");
            rs.next();
            String guid = rs.getString(1);
            Question question = new Question(parameters, guid);

            PreparedStatement ps = conn.prepareStatement("INSERT INTO question(guid, question, correctIndex, duration, answers) VALUES (?, ?, ?, ?, ?)");
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
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e) {
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    public Question getQuestion(Object id) throws ExceptionHandler {
        try {
            if (String.class.isInstance(id)) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM question WHERE guid = ?");
                ps.setString(1, (String) id);
                ResultSet rs = ps.executeQuery();
                Question question = new Question(rs);
                return question;
            } else {
                throw new ExceptionHandler("bad params", HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    public void updateQuestionWithParams(Map<String, Object> params) throws ExceptionHandler {
        String op = (String) params.get("op");
        String path = (String) params.get("path");
        Map<String, Object> parameters = (Map<String, Object>)params.get("parameters");
        if (op != null && path != null && parameters != null){
            switch (op) {
                case "replace": {
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
                                Arrays.asList("question", "correctIndex", "duration", "answers"));
                        String query = makePatchDBQuerry("question", allowedFields, parameters, path);
                        if (query != null) {
                            PreparedStatement ps = conn.prepareStatement(query);
                            ps.executeUpdate();
                            ps.close();
                        }
                    }catch (SQLException e){
                        throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
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
        try {
            if (String.class.isInstance(id) && String.class.isInstance(lectureId)) {
                PreparedStatement ps = conn.prepareStatement("update lecture set questions = array_remove(questions, CAST (? AS TEXT )) where guid=?");
                ps.setString(1, (String) id);
                ps.setString(2, (String) lectureId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("DELETE FROM question WHERE guid = ?");
                ps.setString(1, (String) id);
                ps.executeUpdate();
                ps.close();
            } else {
                throw new ExceptionHandler(EXCEPTION_BADREQUEST, HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }catch (SQLException e){
            throw new ExceptionHandler(e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
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
            statement.append(value);
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
