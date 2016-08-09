package filip.test;

/**
 * Created by Filip on 8/7/2016.
 */

        import java.io.*;
        import java.net.HttpURLConnection;
        import java.net.InetSocketAddress;
        import java.net.URI;
        import java.net.URLDecoder;
        import java.util.*;
        import java.sql.*;

        import com.google.gson.Gson;
        import com.google.gson.GsonBuilder;
        import com.sun.net.httpserver.Headers;
        import com.sun.net.httpserver.HttpExchange;
        import com.sun.net.httpserver.HttpHandler;
        import com.sun.net.httpserver.HttpServer;

public class Server {

    private int port = 8000;
    private DBHandler dbHandler;

    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.serverInit();
            server.dbHandler = new DBHandler();
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void serverInit() throws IOException {
        //server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("Server started at " + port);
        server.createContext("/user", new UserHandler());
        server.setExecutor(null);
        server.start();
    }

    private Map<String, Object> extractBodyParameters (HttpExchange he) throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String query = br.readLine();
        parseQuery(query, parameters);

        return parameters;
    }

    private Map<String, Object> extractURIParameters (HttpExchange he) throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        parseQuery(query, parameters);

        return parameters;
    }

    private class UserHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange he) throws IOException {
            // parse request
            String method = he.getRequestMethod();
            String response = "";
            Map<String, Object> parameters;

            try {

                switch (method){
                    case "POST": {
                        parameters = extractBodyParameters(he);
                        dbHandler.createUser(parameters);
                        break;
                    }
                    case "DELETE": {
                        parameters = extractBodyParameters(he);
                        dbHandler.deleteUserByEmail(parameters.get("email"));
                        break;
                    }
                    case "GET": {
                        parameters = extractURIParameters(he);
                        Object obj = dbHandler.getUserByEmail(parameters.get("email"));
                        response = makeResponse(obj);
                        break;
                    }
                    default:{
                        throw new RuntimeException("method not supported");
                    }
                }
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            } catch (SQLException e) {
                response = makeResponse(e.toString());
                he.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
            } catch (RuntimeException e){
                response = makeResponse(e.toString());
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, 0);
            } catch (Exception e){
                response = makeResponse(e.toString());
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
            } finally {
                // send response
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private static void parseQuery(String query, Map<String,
            Object> parameters) throws UnsupportedEncodingException {

        if (query != null) {
            String pairs[] = query.split("[&]");
            for (String pair : pairs) {
                String param[] = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);

                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }

    private String makeResponse(Object messageObject){
//        Map<String, Object> responseMap = new HashMap<>();
        String response;
        if (ResultSet.class.isInstance(messageObject)){
            response = Utilities.getFormattedResult((ResultSet)messageObject);
        }else{
            Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            response = gson.toJson(messageObject);
        }

        return response;
    }


}