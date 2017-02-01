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
    private ConcurrentHashMap<String, int []> answersToQuestions;

    int port = 8210;

    public enum SocketMethods {
        MESSAGE,
        LOGIN, //server wants user to login
        STOPPEDLECTURE,
        LISTENERSENTQUESTION,
        LECTURERSENTQUESTION,
        LECTURERSENTLISTENERQUESTION,
        CLOSE
    }

    SocketHandler(){
        try {
            serverSocket = new ServerSocket(port);
            initProperties();
            Utilities.printLog("SocketHandler: started at " + port);
        } catch (IOException e) {
            e.printStackTrace();
            Utilities.printLog("SocketHandler: failed to start at " + port);
        }
    }

    void initProperties() {
        clients = new ConcurrentHashMap<>();
        runningLectures = new ConcurrentHashMap<>();
        answersToQuestions = new ConcurrentHashMap<>();
    }

    void start() {
        while (true) try {
            Socket socket = serverSocket.accept();
            Utilities.printLog("SocketHandler: new socket client started");
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

    void clean() {
        try {
            for (String key : clients.keySet()) {
                ClientSocketHandler client = clients.get(key);
                client.pw.println(makeClientMessage(SocketMethods.MESSAGE, "socket closed due to server restart"));
                closeClient(client);
            }
            clients = null;
            runningLectures = null;
            answersToQuestions = null;
//            serverSocket.close();
            Utilities.printLog("SocketHandler: server cleaned");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loginClient(ClientSocketHandler client, String guid) throws ExceptionHandler {
        if (clients.containsKey(guid)) {
            ClientSocketHandler oldClient = clients.get(guid);
            oldClient.pw.println(makeClientMessage(SocketMethods.CLOSE, "You logged in from different place"));
            closeClient(oldClient);
//            throw new ExceptionHandler("user already logged in");
        }

        Boolean exists;
        if (guid.equals("LISTENER")) {
            exists = true;
            client.isListener = true;
        } else{
            exists = Server.dbHandler.checkIfUserExists(guid);
        }
        if (exists) {
            if (client.isListener){
                client.guid = "LISTENER" + ClientSocketHandler.getNewListenerNumber();
            }else {
                client.guid = guid;
            }

            clients.put(client.guid, client);

        } else {
            throw new ExceptionHandler("user doesn't exist");
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
                throw new ExceptionHandler("user is not owner of this lecture or lecture does not exist");
            }else {
                String password = (String) params.get("password");
                if (id != null){
                    Lecture lecture = Server.dbHandler.getLectureFromUniqueId(id);
                    HashMap<String, Object> lectureEntry = new HashMap<>();
                    ArrayList<String> users = new ArrayList<>();
                    lectureEntry.put("owner", guid);
                    if (password != null) {
                        lectureEntry.put("password", password);
                    }
                    lectureEntry.put("listeners", users);
                    lectureEntry.put("questions", new ArrayList<String>());

                    Server.dbHandler.updateUserWithRunningLecture(guid, id, false);

                    runningLectures.put(id, lectureEntry);
                    Utilities.printLog("SocketHandler: lecture started with id: " + lecture.unique_id);
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

                    //cleanup
                    ArrayList<String> questions = (ArrayList<String>)lectureEntry.get("questions");
                    for (String questionId : questions) {
                        if (answersToQuestions.containsKey(questionId)){
                            answersToQuestions.remove(questionId);
                        }
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
                    ArrayList<String> users = (ArrayList<String>)lectureEntry.get("listeners");
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
            ArrayList<String> users = (ArrayList<String>)lectureEntry.get("listeners");
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

    void sendQuestionToListeners(LinkedTreeMap params, String senderId) throws ExceptionHandler {
        try {
            String lectureId = (String) params.get("lectureId");
            String questionId = (String) params.get("questionId");
            if (runningLectures.containsKey(lectureId)){
                Question question = Server.dbHandler.getQuestion(questionId);

                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(lectureId);
                String owner = (String) lectureEntry.get("owner");
                if (owner.equals(senderId)) {
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    for (String listenerGuid : users) {
                        ClientSocketHandler listenerSocket = clients.get(listenerGuid);
                        listenerSocket.pw.println(makeClientMessage(SocketMethods.LECTURERSENTQUESTION, question));
                    }

                    //add entry to the answersToQuestions object
                    int count = question.answers.size();
                    int [] answersArray = new int[count];
                    answersToQuestions.put(questionId, answersArray);
                    ///////

                    //update lectures
                    ArrayList<String> questions = (ArrayList<String>)lectureEntry.get("questions");
                    questions.add(questionId);
                    ////////

                }else {
                    throw new ExceptionHandler("you are not allowed to do this");
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

    void sendQuestionToLecturer(LinkedTreeMap params, String listeningToTheLecture) throws ExceptionHandler {
        try {
            String lectureId = (String) params.get("lectureId");
            String questionText = (String) params.get("questionText");
            if (runningLectures.containsKey(lectureId)){
                if (listeningToTheLecture != null && listeningToTheLecture.equals(lectureId)) {
                    HashMap<String, Object> lectureEntry = (HashMap<String, Object>) runningLectures.get(lectureId);
                    String owner = (String) lectureEntry.get("owner");
                    ClientSocketHandler ownerSocker = clients.get(owner);
                    if (ownerSocker != null) {
                        ownerSocker.pw.println(makeClientMessage(SocketMethods.LISTENERSENTQUESTION, questionText));
                    } else {
                        //owner is disconnected, currently do nothing
                    }
                }else {
                    throw new ExceptionHandler("you are not listening to this lecture");
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

    void sendListenerQuestionToListeners(LinkedTreeMap params, String senderId) throws ExceptionHandler {
        try {
            String lectureId = (String) params.get("lectureId");
            String questionText = (String) params.get("questionText");
            if (runningLectures.containsKey(lectureId)){
                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(lectureId);
                String owner = (String) lectureEntry.get("owner");
                if (owner.equals(senderId)) {
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    for (String listenerGuid : users) {
                        ClientSocketHandler listenerSocket = clients.get(listenerGuid);
                        listenerSocket.pw.println(makeClientMessage(SocketMethods.LECTURERSENTLISTENERQUESTION, questionText));
                    }
                }else {
                    throw new ExceptionHandler("you are not allowed to do this");
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

    void sendAnswerToQuestion(LinkedTreeMap params) throws ExceptionHandler {
        try {
            String questionId = (String) params.get("questionId");
            Double answerIndex = (Double) params.get("answerIndex");
            if (answersToQuestions.containsKey(questionId)){
                int[] answersArray = answersToQuestions.get(questionId);
                answersArray[answerIndex.intValue()]++;
            }else {
                throw new ExceptionHandler("question not asked or bad question id");
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    int[] getAnswersToQuestion(LinkedTreeMap params) throws ExceptionHandler {
        //extra security is to send senderId and lectureId and check if senderId is owner of lecture, but this is not needed for now
        try {
            String questionId = (String) params.get("questionId");
            if (answersToQuestions.containsKey(questionId)){
                int[] answersArray = answersToQuestions.get(questionId);
                return answersArray;
            }else {
                throw new ExceptionHandler("question not asked or bad question id");
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
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
