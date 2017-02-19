package filip.test;

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.security.SecureRandom;
import java.math.BigInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.joda.time.DateTime;

import javax.mail.internet.*;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 8/8/2016.
 */
class Utilities {
    static String getFormattedResult(ResultSet rs) {
        List<JsonObject> resList = new ArrayList<JsonObject>();
        try {
            // get column names
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCnt = rsMeta.getColumnCount();
            List<String> columnNames = new ArrayList<String>();
            for (int i = 1; i <= columnCnt; i++) {
                columnNames.add(rsMeta.getColumnName(i).toUpperCase());
            }

            while (rs.next()) { // convert each object to an human readable JSON object
                JsonObject obj = new JsonObject();
                for (int i = 1; i <= columnCnt; i++) {
                    String key = columnNames.get(i - 1);
                    String value = rs.getString(i);
                    obj.addProperty(key, value);
                }
                resList.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Object forConvert;
        if (resList.size() == 1) {
            forConvert = resList.get(0);
        } else {
            forConvert = resList;
        }

        return new Gson().toJson(forConvert);
    }

    static String cryptWithMD5(String pass) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] passBytes = pass.getBytes();
        md.reset();
        byte[] digested = md.digest(passBytes);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < digested.length; i++) {
            sb.append(Integer.toHexString(0xff & digested[i]));
        }
        return sb.toString();
    }

    static Claims verifyToken(HttpExchange he) throws ExceptionHandler {

        Headers headers = he.getRequestHeaders();

        try {
            String jwt = headers.get("JWT").get(0);
            Claims claims = verifyToken(jwt);
            return claims;
        } catch (Exception e) {
            throw new ExceptionHandler(EXCEPTION_BADTOKEN, HttpURLConnection.HTTP_UNAUTHORIZED);
        }
    }

    static Claims verifyToken(String jwt) throws ExceptionHandler {
        try {
            byte[] key = JWT_SECRET.getBytes();
            return Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody();
        } catch (SignatureException e) {
            throw new ExceptionHandler(EXCEPTION_BADTOKEN, HttpURLConnection.HTTP_UNAUTHORIZED);
        }
    }

    static Map<String, String> getEndpoints() {
        Map<java.lang.String, java.lang.String> endpoints = new HashMap<>();

        String host = "http://24.135.42.105";
        String port = "8000";

        StringBuffer sb = new StringBuffer(host);
        sb.append(':').append(port).append('/');

        endpoints.put("login", sb.toString() + "login");
        endpoints.put("user", sb.toString() + "user");
        endpoints.put("user-get", sb.toString() + "user?email={email}");
        endpoints.put("logs", sb.toString() + "logs");
        endpoints.put("lecture", sb.toString() + "lecture");
        endpoints.put("lecture-get", sb.toString() + "lecture?id={id}");
        endpoints.put("question", sb.toString() + "question");
        endpoints.put("question-get", sb.toString() + "question?id={id}");
        endpoints.put("docs", sb.toString() + "docs");
        endpoints.put("utilities", sb.toString() + "utilities?util={util}");

        return endpoints;
    }

    static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    static List<HashMap<String,Object>> convertResultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

        while (rs.next()) {
            HashMap<String, Object> row = new HashMap<String, Object>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    static void printLog(String message){
        Date dt = new Date();
        DateTime currentDate = new DateTime(dt);

        StringBuilder stringBuilder = new StringBuilder();
        int hour = currentDate.getHourOfDay();
        if (hour<10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(hour).append(":");

        int minute = currentDate.getMinuteOfHour();
        if (minute<10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(minute).append(":");

        int second = currentDate.getSecondOfMinute();
        if (second<10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(second).append(" ");
        stringBuilder.append(message);

        System.out.println(stringBuilder.toString());
    }

    static void printLog(Object callerClass, String message){
        String fullClassName = callerClass.getClass().getName();
        String[] parts = fullClassName.split("\\.");
        String className = parts[parts.length - 1];

        Date dt = new Date();
        DateTime currentDate = new DateTime(dt);

        StringBuilder stringBuilder = new StringBuilder();
        int hour = currentDate.getHourOfDay();
        if (hour<10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(hour).append(":");

        int minute = currentDate.getMinuteOfHour();
        if (minute<10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(minute).append(":");

        int second = currentDate.getSecondOfMinute();
        if (second<10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(second).append(" ");
        stringBuilder.append(className).append(": ");
        stringBuilder.append(message);

        System.out.println(stringBuilder.toString());
    }

     static String generateLectureString(){
        SecureRandom random = new SecureRandom();
        return new BigInteger(30, random).toString(32);
    }

    static Map<String, Object> readJsonApplication(String body){
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        try {
            return gson.fromJson(body, Map.class);
        }catch (JsonSyntaxException e){
            return null;
        }
    }

    static String mapToJson(Object object){
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        return gson.toJson(object);
    }

    static void restartServer(){
        Server.restart();
    }
}