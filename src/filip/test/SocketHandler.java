package filip.test;

import com.google.gson.internal.LinkedTreeMap;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public enum SocketHandler {
    INSTANCE;
    /**
     * Created by Filip on 10/4/2016.
     * unique (INSTANCE)
     * creating ClientSocketHandler threads for each client connected
     */
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientSocketHandler> clients;
    private ConcurrentHashMap<String, Object> runningLectures;

    int port = 8210;

    public enum SocketMethods {
        MESSAGE,
        STOPPEDLECTURE
    }

    SocketHandler(){
        try {
            serverSocket = new ServerSocket(port);
            clients = new ConcurrentHashMap<>();
            runningLectures = new ConcurrentHashMap<>();;
            System.out.println("Socket server started at " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to start socket server at " + port);
        }
    }

    void start() {
        while (true) try {
            Socket socket = serverSocket.accept();
            ClientSocketHandler client = new ClientSocketHandler(socket);
            Thread t = new Thread(client);
            t.setDaemon(true);
            client.runningOnThread = t;
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
            break;
        }
    }

    void addClient(ClientSocketHandler client, String guid) throws ExceptionHandler {
        if (clients.containsKey(guid)) {
            throw new ExceptionHandler("user already logged in");
        } else {
            Boolean exists = Server.dbHandler.checkIfUserExists(guid);
            if (exists) {
                clients.put(guid, client);
            } else {
                throw new ExceptionHandler("user doesn't exist");
            }
        }
    }

    void closeClient(ClientSocketHandler client){
        String guid = client.guid;
        if (null != guid) {
            clients.remove(guid);

            //remove if client is listening to some lecture
            if (client.listeningToTheLecture != null) {
                stopListenLecture(client.listeningToTheLecture, client.guid);
            }
            //////
        }
        try {
            client.socket.close();
            client.runningOnThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startLecture(LinkedTreeMap params, String guid) throws ExceptionHandler {
        try {
            String id = (String) params.get("id");
            if (runningLectures.containsKey(id)){
                throw new ExceptionHandler("lecture already started");
            }else if (Server.dbHandler.checkIfUserHasRunningLecture(guid)){
                throw new ExceptionHandler("user already has one lecture running");
            }else if (!Server.dbHandler.checkIfUserIsOwnerOfLecture(guid, id)){
                throw new ExceptionHandler("user is not owner of this lecture");
            }else {
                String password = (String) params.get("password");
                if (id != null){
                    Lecture lecture = Server.dbHandler.getLecture(id);
                    HashMap<String, Object> lectureEntry = new HashMap<>();
                    ArrayList<String> users = new ArrayList<>();
                    lectureEntry.put("owner", guid);
                    if (password != null) {
                        lectureEntry.put("password", password);
                    }
                    lectureEntry.put("listeners", users);

                    Server.dbHandler.updateUserWithRunningLecture(guid, lecture.guid, false);

                    runningLectures.put(lecture.guid, lectureEntry);
                    Utilities.printLog("SocketHandler: lecture started with id: " + lecture.guid);
                }else {
                    throw new ExceptionHandler("bad params");
                }
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    void stopLecture(LinkedTreeMap params, String callerGuid) throws ExceptionHandler {
        try {
            String id = (String) params.get("id");
            if (runningLectures.containsKey(id)){
                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(id);
                String ownerGuid = (String) lectureEntry.get("owner");
                if (ownerGuid.equals(callerGuid)) {
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    for (int i = 0; i < users.size(); i++) {
                        String listener = users.get(i);
                        ClientSocketHandler listenerSocket = clients.get(listener);
                        listenerSocket.pw.println(makeClientMessage(SocketMethods.STOPPEDLECTURE, "lecture stopped"));
                        listenerSocket.listeningToTheLecture = null;
                    }
                    runningLectures.remove(id);

                    Utilities.printLog("SocketHandler: lecture stopped with id: " + id);
                }else {
                    throw new ExceptionHandler("you are not allowed to stop this lecture");
                }
            }else {
                //do nothing
//                throw new ExceptionHandler("lecture isn't started or bad lecture id");
            }
            Server.dbHandler.updateUserWithRunningLecture(callerGuid, id, true);

        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    void listenLecture(LinkedTreeMap params, ClientSocketHandler client) throws ExceptionHandler {
        try {
            String id = (String) params.get("id");
            String password = (String) params.get("password");
            if (runningLectures.containsKey(id)){
                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(id);
                if (!lectureEntry.containsKey("password") || (password != null && password.equals(lectureEntry.get("password")))){
                    ArrayList<String> users = ( ArrayList<String>)lectureEntry.get("listeners");
                    users.add(client.guid);
                    client.listeningToTheLecture = id;
                }else {
                    throw new ExceptionHandler("wrong password");
                }
            }else {
                throw new ExceptionHandler("lecture isn't started or bad lecture id");
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    void stopListenLecture(String lectureId, String guid) {
        HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(lectureId);

        if (lectureEntry != null) {
            ArrayList<String> users = ( ArrayList<String>)lectureEntry.get("listeners");
            boolean found = false;

            for (int i=0; i<users.size(); i++){
                String userGuid = users.get(i);
                if (userGuid.equals(guid)){
                    users.remove(i);
                    found = true;
                    break;
                }
            }

            if (!found){
//                    throw new ExceptionHandler("user is not listening to the selected lecture");
//                    better do nothing, so that clientsocket sets listeningtothelecture = null and client gets success
            }
        }else {
//                throw new ExceptionHandler("lecture isn't started or bad lecture id");
//                better do nothing, so that clientsocket sets listeningtothelecture = null and client gets success
        }
    }

    static String makeClientResponse(boolean ok, Object message){
        HashMap <String, Object> response = new HashMap<>();
        response.put("type", "response");
        response.put("ok", ok);

        if (message != null) {
            response.put("message", message);
        }
        return Utilities.mapToJson(response);
    }

    static String makeClientMessage(SocketMethods method, Object message){
        HashMap <String, Object> response = new HashMap<>();
        response.put("type", "message");

        if (method != SocketMethods.MESSAGE) {
            response.put("method", method.toString());
        }
        if (message != null) {
            response.put("message", message);
        }
        return Utilities.mapToJson(response);
    }
}
