package filip.test.socket;

import com.google.gson.internal.LinkedTreeMap;
import filip.test.*;

import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private ConcurrentHashMap<String, String> usersRunningLectures; //links users with running lectures
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

    public void initProperties() {
        clients = new ConcurrentHashMap<>();
        runningLectures = new ConcurrentHashMap<>();
        answersToQuestions = new ConcurrentHashMap<>();
        usersRunningLectures = new ConcurrentHashMap<>();
    }

    public void start() {
        while (true) try {
            Socket socket = serverSocket.accept();
            Utilities.printLog("SocketHandler: new socket client started");
            ClientSocketHandler client = new ClientSocketHandler(socket);
            Thread t = new Thread(client);
            t.setDaemon(true);
            client.setRunningOnThread(t);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
            break;
        }
    }

    public void clean() {
        try {
            for (String key : clients.keySet()) {
                ClientSocketHandler client = clients.get(key);
                client.getPW().println(makeClientMessage(SocketMethods.MESSAGE, "socket closed due to server restart"));
                closeClient(client);
            }
            clients = null;
            runningLectures = null;
            answersToQuestions = null;
            usersRunningLectures = null;
//            serverSocket.close();
            Utilities.printLog("SocketHandler: server cleaned");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loginClient(ClientSocketHandler client, String guid) throws ExceptionHandler {
        if (clients.containsKey(guid)) {
            ClientSocketHandler oldClient = clients.get(guid);
            oldClient.getPW().println(makeClientMessage(SocketMethods.CLOSE, "You logged in from different place"));
            closeClient(oldClient);
//            throw new ExceptionHandler("user already logged in");
        }

        Boolean exists;
        if (guid.equals("LISTENER")) {
            exists = true;
            client.setIsListener(true);
        } else{
            exists = Server.getDbHandler().checkIfUserExists(guid);
        }
        if (exists) {
            if (client.getIsListener()){
                client.setGuid("LISTENER" + ClientSocketHandler.getNewListenerNumber());
            }else {
                client.setGuid(guid);
                client.setLectureId(getRunningLectureForUser(guid));
            }

            clients.put(client.getGuid(), client);

        } else {
            throw new ExceptionHandler("user doesn't exist");
        }
    }

    void closeClient(ClientSocketHandler client){
        String guid = client.getGuid();
        if (null != guid) {
            clients.remove(guid);

            //remove if client is listening to some lecture
            if (client.getLectureId() != null) {
                stopListenLecture(client.getLectureId(), client.getGuid());
            }
            //////
        }
        client.closeSocket();
        client.interruptThread();
    }

    String getRunningLectureForUser(String userId){
        if (usersRunningLectures.containsKey(userId)){
            return usersRunningLectures.get(userId);
        }else {
            return null;
        }
    }

    String startLecture(LinkedTreeMap params, String guid) throws ExceptionHandler {
        try {
            String id = (String) params.get("id");
            if (runningLectures.containsKey(id)){
                throw new ExceptionHandler("lecture already started");
            }else if (Server.getDbHandler().checkIfUserHasRunningLecture(guid)){
                throw new ExceptionHandler("user already has one lecture running");
            }else if (!Server.getDbHandler().checkIfUserIsOwnerOfLecture(guid, id)){
                throw new ExceptionHandler("user is not owner of this lecture or lecture does not exist");
            }else {
                String password = (String) params.get("password");
                if (id != null){
                    Lecture lecture = Server.getDbHandler().getLectureFromUniqueId(id);
                    HashMap<String, Object> lectureEntry = new HashMap<>();
                    ArrayList<String> users = new ArrayList<>();
                    lectureEntry.put("owner", guid);
                    if (password != null) {
                        lectureEntry.put("password", password);
                    }
                    lectureEntry.put("listeners", users);
                    //questions that lecturer sends to the listeners
                    lectureEntry.put("questions", Collections.synchronizedList(new ArrayList()));
                    //questions that listener sends to the lecturer
                    lectureEntry.put("listenerQuestions", Collections.synchronizedList(new ArrayList()));

                    Server.getDbHandler().updateUserWithRunningLecture(guid, id, false);

                    runningLectures.put(id, lectureEntry);
                    usersRunningLectures.put(guid, id);

                    Utilities.printLog("SocketHandler: lecture started with id: " + lecture.getUnique_id());

                    return id;
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

    void stopLecture(ClientSocketHandler lecturer) throws ExceptionHandler {
        try {
            if (runningLectures.containsKey(lecturer.getLectureId())){
                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(lecturer.getLectureId());
                String ownerGuid = (String) lectureEntry.get("owner");
                if (ownerGuid.equals(lecturer.getGuid())) {
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    for (int i = 0; i < users.size(); i++) {
                        String listener = users.get(i);
                        ClientSocketHandler listenerSocket = clients.get(listener);
                        listenerSocket.getPW().println(makeClientMessage(SocketMethods.STOPPEDLECTURE, "lecture stopped"));
                        listenerSocket.setLectureId(null);
                    }

                    //cleanup
                    List<Object> questions = (List<Object>)lectureEntry.get("questions");
                    synchronized (questions) {
                        for (Object question : questions) {
                            if (question instanceof Question) {
                                Question q = (Question) question;
                                if (answersToQuestions.containsKey(q.getGuid())) {
                                    answersToQuestions.remove(q.getGuid());
                                }
                            }
                        }
                    }

                    Utilities.printLog("SocketHandler: lecture stopped with id: " + lecturer.getLectureId());

                    runningLectures.remove(lecturer.getLectureId());
                    usersRunningLectures.remove(lecturer.getGuid());
                    Server.getDbHandler().updateUserWithRunningLecture(lecturer.getGuid(), lecturer.getLectureId(), true);

                    lecturer.setLectureId(null);
                }else {
                    throw new ExceptionHandler("you are not allowed to stop this lecture");
                }
            }else {
                //do nothing
//                throw new ExceptionHandler("lecture isn't started or bad lecture id");
            }

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
                    users.add(client.getGuid());
                    client.setLectureId(id);
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

    void sendQuestionToListeners(LinkedTreeMap params, ClientSocketHandler sender) throws ExceptionHandler {
        try {
            String questionId = (String) params.get("questionId");
            if (runningLectures.containsKey(sender.getLectureId())){
                Question question = Server.getDbHandler().getQuestion(questionId);

                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(sender.getLectureId());
                String owner = (String) lectureEntry.get("owner");
                if (owner.equals(sender.getGuid())) {
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    for (String listenerGuid : users) {
                        ClientSocketHandler listenerSocket = clients.get(listenerGuid);
                        listenerSocket.getPW().println(makeClientMessage(SocketMethods.LECTURERSENTQUESTION, question));
                    }

                    //add entry to the answersToQuestions object
                    int count = question.getAnswers().size();
                    int [] answersArray = new int[count];
                    answersToQuestions.put(questionId, answersArray);
                    ///////

                    //update lectures
                    List<Object> questions = (List<Object>)lectureEntry.get("questions");
                    questions.add(question);
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

    void sendQuestionToLecturer(LinkedTreeMap params, ClientSocketHandler listener) throws ExceptionHandler {
        try {
            String questionText = (String) params.get("questionText");
            if (listener.getIsListener()){
                if (runningLectures.containsKey(listener.getLectureId())){
                    HashMap<String, Object> lectureEntry = (HashMap<String, Object>) runningLectures.get(listener.getLectureId());

                    //add question to the array
                    List<Object> listenerQuestions = (List<Object>) lectureEntry.get("listenerQuestions");
                    listenerQuestions.add(questionText);
                    ////

                    String owner = (String) lectureEntry.get("owner");
                    ClientSocketHandler ownerSocker = clients.get(owner);
                    if (ownerSocker != null) {
                        ownerSocker.getPW().println(makeClientMessage(SocketMethods.LISTENERSENTQUESTION, questionText));
                    } else {
                        //owner is disconnected, currently do nothing
                    }
                }else {
                    throw new ExceptionHandler("lecture isn't started or bad lecture id");
                }
            }else {
                throw new ExceptionHandler("lecturer is not allowed to do this");
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    void sendListenerQuestionToListeners(LinkedTreeMap params, ClientSocketHandler lecturer) throws ExceptionHandler {
        try {
            Object questionText = params.get("questionText");
            if (runningLectures.containsKey(lecturer.getLectureId())){
                HashMap<String, Object> lectureEntry = (HashMap<String, Object>)runningLectures.get(lecturer.getLectureId());
                String owner = (String) lectureEntry.get("owner");
                if (owner.equals(lecturer.getGuid())) {
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    for (String listenerGuid : users) {
                        ClientSocketHandler listenerSocket = clients.get(listenerGuid);
                        listenerSocket.getPW().println(makeClientMessage(SocketMethods.LECTURERSENTLISTENERQUESTION, questionText));
                    }

                    //update lectures
                    List<Object> questions = (List<Object>)lectureEntry.get("questions");
                    questions.add(questionText);
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

    //TODO: add check if user is listening to the lecture that has this question (mybe client sends more params)
    void sendAnswerToQuestion(LinkedTreeMap params, ClientSocketHandler listener) throws ExceptionHandler {
        try {
            if (listener.getIsListener()){
                String questionId = (String) params.get("questionId");
                Double answerIndex = (Double) params.get("answerIndex");
                if (answersToQuestions.containsKey(questionId)){
                    int[] answersArray = answersToQuestions.get(questionId);
                    if (answerIndex < answersArray.length){
                        answersArray[answerIndex.intValue()]++;
                    }else {
                        throw new ExceptionHandler("index greater than number of questions");
                    }
                }else {
                    throw new ExceptionHandler("question not asked or bad question id");
                }
            }else {
                throw new ExceptionHandler("you are not listening to this lecture");
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

    Object getLastQuestion(String lectureId) throws ExceptionHandler {
        if (runningLectures.containsKey(lectureId)) {
            HashMap<String, Object> lectureEntry = (HashMap<String, Object>) runningLectures.get(lectureId);
            List<Object> questions = (List<Object>)lectureEntry.get("questions");
            synchronized (questions) {
                if (questions.size() > 0) {
                    return questions.get(questions.size() - 1);
                } else {
                    return null;
                }
            }
        }else {
            throw new ExceptionHandler("lecture isn't started or bad lecture id");
        }
    }

    Object getListenerQuestions(ClientSocketHandler client) throws ExceptionHandler{
        try {
            if (runningLectures.containsKey(client.getLectureId())){
                HashMap<String, Object> lectureEntry = (HashMap<String, Object>) runningLectures.get(client.getLectureId());
                String owner = (String) lectureEntry.get("owner");
                if (owner.equals(client.getGuid())){
                    return lectureEntry.get("listenerQuestions");
                }else {
                    throw new ExceptionHandler("you are not owner of this lecture");
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

    int getNumOfListeners(ClientSocketHandler sender) throws ExceptionHandler{
        try {
            if (sender.getLectureId() != null){
                if (runningLectures.containsKey(sender.getLectureId())) {
                    HashMap<String, Object> lectureEntry = (HashMap<String, Object>) runningLectures.get(sender.getLectureId());
                    ArrayList<String> users = (ArrayList<String>) lectureEntry.get("listeners");
                    return users.size();
                } else {
                    throw new ExceptionHandler("lecture isn't started or bad lecture id");
                }
            }else {
                throw new ExceptionHandler("you are not listening to any lecture");
            }
        }catch (ExceptionHandler e){
            throw e;
        }catch (Exception e){
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
        Utilities.printLog("SocketHandler: Server sent to client: " + response);
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
