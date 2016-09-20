package filip.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 8/8/2016.
 */
public class Utilities {
    public static String getFormattedResult(ResultSet rs) {
        List<JsonObject> resList = new ArrayList<JsonObject>();
        try {
            // get column names
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCnt = rsMeta.getColumnCount();
            List<String> columnNames = new ArrayList<String>();
            for(int i=1;i<=columnCnt;i++) {
                columnNames.add(rsMeta.getColumnName(i).toUpperCase());
            }

            while(rs.next()) { // convert each object to an human readable JSON object
                JsonObject obj = new JsonObject();
                for(int i=1;i<=columnCnt;i++) {
                    String key = columnNames.get(i - 1);
                    String value = rs.getString(i);
                    obj.addProperty(key, value);
                }
                resList.add(obj);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Object forConvert;
        if (resList.size() == 1){
            forConvert = resList.get(0);
        }else{
            forConvert = resList;
        }

        return new Gson().toJson(forConvert);
    }

    public static String cryptWithMD5(String pass) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] passBytes = pass.getBytes();
        md.reset();
        byte[] digested = md.digest(passBytes);
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<digested.length;i++){
            sb.append(Integer.toHexString(0xff & digested[i]));
        }
        return sb.toString();
    }

    public static Jws verifyToken(HttpExchange he) throws Exception {

        Headers headers= he.getRequestHeaders();

        try {
            String jwt = headers.get("JWT").get(0);
            Jws token = verifyToken(jwt);
            return token;
        }catch (Exception e){
            throw new Exception(EXCEPTION_NOTAUTHORIZED);
        }
    }

    public static Jws verifyToken(String jwt) throws Exception {
        try {
            byte[] key = JWT_SECRET.getBytes();
            return (Jws) Jwts.parser().setSigningKey(key).parseClaimsJws(jwt);
        }catch (SignatureException e){
            throw new Exception(EXCEPTION_NOTAUTHORIZED);
        }
    }

    public static Map<String, String> getEndpoints(){
        Map<java.lang.String, java.lang.String> endpoints = new HashMap<>();

        endpoints.put("user", "http://localhost:8000/user");
        endpoints.put("login", "http://localhost:8000/login");

        return endpoints;
    }
}
