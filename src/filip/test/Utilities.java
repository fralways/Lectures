package filip.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

        String json = new Gson().toJson(forConvert);
        return json;
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

}
