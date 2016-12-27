package filip.test;
/**
 * Created by Filip on 8/7/2016.
 */

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.sql.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;

import static filip.test.StaticKeys.*;
        import static filip.test.Utilities.*;

public class Server {

    private int port = 8000;
    private DBHandler dbHandler;

    public static void main(String[] args) {
        Server server = new Server();

        try {
            server.serverInit();
            server.dbHandler = new DBHandler();
            SocketHandler socketServer = SocketHandler.INSTANCE;
            socketServer.start();
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void serverInit() throws IOException {
        //server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("Server started at " + port);
        server.createContext("/user", new UserHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/home", new HomeHandler());
        server.createContext("/test", new TestHandler());
        server.createContext("/logs", new LogsHandler());
        server.createContext("/lecture", new LectureHandler());
        server.createContext("/question", new QuestionHandler());
        server.setExecutor(null);
        server.start();
    }

    //region Handlers

    private class HomeHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            String method = he.getRequestMethod();
            String response = "";

            try {
                switch (method){
                    case "GET": {
                        Map<String, String> endpoints = Utilities.getEndpoints();
                        response = makeResponse(endpoints);
                        break;
                    }
                    default:{
                        throw new RuntimeException(EXCEPTION_BADMETHOD);
                    }
                }
                handleResponseHeader(he, null);
            } catch (Exception e){
                response = makeResponse(e.toString());
                handleResponseHeader(he, e);
            } finally {
                // send response
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
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
                        System.out.println("Usao u create user");
                        parameters = extractBodyParameters(he);
                        dbHandler.createUser(parameters);
                        break;
                    }
                    case "DELETE": {
                        System.out.println("Usao u delete user");
                        verifyToken(he);
                        parameters = extractBodyParameters(he);
                        dbHandler.deleteUserByEmail(parameters.get("email"));
                        break;
                    }
                    case "GET": {
                        System.out.println("Usao u get user");
                        verifyToken(he);
                        parameters = extractURIParameters(he);
                        Object obj = dbHandler.getUserByEmail(parameters.get("email"));

                        response = makeResponse(obj);

                        break;
                    }
                    case "PATCH": {
                        System.out.println("Usao u patch user");
                        Claims claims = verifyToken(he);
                        String userId = claims.getSubject();
                        parameters = extractBodyParameters(he);
                        dbHandler.updateUserWithParams(userId, parameters);
                        break;
                    }
                    default:{
                        throw new RuntimeException(EXCEPTION_BADMETHOD);
                    }
                }
                handleResponseHeader(he, null);
                System.out.println("success");
            } catch (Exception e){
                response = makeResponse(e.toString());
                handleResponseHeader(he, e);
                System.out.println("error");
            } finally {
                // send response
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private class LoginHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String method = he.getRequestMethod();
            String response = "";
            Map<String, Object> parameters;
            try {
                if (method.equals("POST")) {
                    System.out.println("Trying to login");
                    parameters = extractBodyParameters(he);
                    String jwt = dbHandler.authenticateUser(parameters);
                    Headers headers = he.getResponseHeaders();
                    headers.set("JWT", jwt);
                } else {
                    throw new RuntimeException(EXCEPTION_BADMETHOD);
                }
                System.out.println("success");
                handleResponseHeader(he, null);
            }catch (RuntimeException|SQLException|NoSuchAlgorithmException e){
                response = makeResponse(e.toString());
                handleResponseHeader(he, e);
            }finally {
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private class LogsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String method = he.getRequestMethod();
            String response = "";
            Map<String, Object> parameters;

            try {
                if (method.equals("GET")) {
                    byte[] encoded = Files.readAllBytes(Paths.get("C:\\Users\\Filip\\Desktop\\serverOutput.txt"));
                    if (null == encoded){
                        System.out.println("Cannot find log file");
                    }else {
                        response = new String(encoded, StandardCharsets.UTF_8);
                    }
                }else {
                    throw new RuntimeException(EXCEPTION_BADMETHOD);
                }
                handleResponseHeader(he, null);
            }catch (RuntimeException e){
                response = makeResponse(e.toString());
                handleResponseHeader(he, e);
            }finally {
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private class LectureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            String method = he.getRequestMethod();
            String response = "";
            Map<String, Object> parameters;

            try {
                switch (method){
                    case "POST": {
                        System.out.println("create new lecture");
                        parameters = extractBodyParameters(he);
                        Claims claims = verifyToken(he);
                        String userId = claims.getSubject();
                        String lectureGuid = dbHandler.createLecture(parameters, userId);
                        Map<String, String> jsonResponse = new HashMap<>();
                        jsonResponse.put("id", lectureGuid);
                        response = makeResponse(jsonResponse);
                        break;
                    }
                    case "DELETE": {
                        System.out.println("delete lecture");
                        Claims claims = verifyToken(he);
                        String userId = claims.getSubject();
                        parameters = extractBodyParameters(he);
                        dbHandler.deleteLecture(parameters.get("id"), userId);
                        break;
                    }
                    case "GET": {
                        System.out.println("get lecture");
                        verifyToken(he);
                        parameters = extractURIParameters(he);
                        Object lecture = dbHandler.getLecture(parameters.get("id"));
                        response = makeResponse(lecture);
                        break;
                    }
                    case "PATCH": {
                        System.out.println("patch lecture");
                        Claims claims = verifyToken(he);
                        String userId = claims.getSubject();
                        System.out.println("user id: " + userId);
                        parameters = extractBodyParameters(he);
                        dbHandler.updateUserWithParams(userId, parameters);
                        break;
                    }
                    default:{
                        throw new RuntimeException(EXCEPTION_BADMETHOD);
                    }
                }
                handleResponseHeader(he, null);
                System.out.println("success");
            } catch (Exception e){
                response = makeResponse(e.toString());
                handleResponseHeader(he, e);
                System.out.println("error");
            } finally {
                // send response
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private class QuestionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            String method = he.getRequestMethod();
            String response = "";
            Map<String, Object> parameters;

            try {
                switch (method){
                    case "POST": {
                        System.out.println("create new question");
                        parameters = extractBodyParameters(he);
                        verifyToken(he);
                        String questionGuid = dbHandler.createQuestion(parameters);
                        Map<String, String> jsonResponse = new HashMap<>();
                        jsonResponse.put("id", questionGuid);
                        response = makeResponse(jsonResponse);
                        break;
                    }
                    case "DELETE": {
                        System.out.println("delete lecture");
                        Claims claims = verifyToken(he);
                        String userId = claims.getSubject();
                        parameters = extractBodyParameters(he);
                        dbHandler.deleteLecture(parameters.get("id"), userId);
                        break;
                    }
                    case "GET": {
                        System.out.println("get question");
                        verifyToken(he);
                        parameters = extractURIParameters(he);
                        Question question = dbHandler.getQuestion(parameters.get("id"));
                        response = makeResponse(question);
                        break;
                    }
                    case "PATCH": {
                        System.out.println("patch lecture");
                        Claims claims = verifyToken(he);
                        String userId = claims.getSubject();
                        System.out.println("user id: " + userId);
                        parameters = extractBodyParameters(he);
                        dbHandler.updateUserWithParams(userId, parameters);
                        break;
                    }
                    default:{
                        throw new RuntimeException(EXCEPTION_BADMETHOD);
                    }
                }
                handleResponseHeader(he, null);
                System.out.println("success");
            } catch (Exception e){
                response = makeResponse(e.toString());
                handleResponseHeader(he, e);
                System.out.println("error");
            } finally {
                // send response
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private class TestHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            dbHandler.getAllFromDB();
        }
    }
    //endregion

    //region Supporting methods

    private void handleResponseHeader(HttpExchange he, Exception ex) throws IOException {
        if (ex == null){
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.set("Content-Type", "application/json");
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
        }else{
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain");
            System.out.println("error message: " + ex.getMessage());

            switch (ex.getMessage()){
                case EXCEPTION_BADMETHOD:{
                    he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, 0);
                    break;
                }
                case EXCEPTION_BADREQUEST:{
                    he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
                    break;
                }
                case EXCEPTION_NOTAUTHORIZED:{
                    he.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, 0);
                    break;
                }
                case EXCEPTION_INTERNAL:
                default:{
                    he.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
                    break;
                }
            }
        }
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

    //endregion

}