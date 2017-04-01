package filip.test.socket;

import com.google.gson.internal.LinkedTreeMap;
import filip.test.*;
import org.joda.time.DateTime;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public enum SocketHandler {
    INSTANCE;
    /**
     * Created by Filip on 10/4/2016.
     * unique (INSTANCE)
     * creating ClientSocketHandler threads for each client connected
     */

    class RunningLecture {

        private String lectureId;
        private String owner;
        private String password;
        private final List<String> listeners;
        private final List<SocketQuestion> lecturerQuestions;    //questions that lecturer sends to the listeners
        private final List<SocketQuestion> listenerQuestions;    //questions that listener sends to the lecturer
        private final ConcurrentHashMap<String, int []> answersToQuestions;

        public String getLectureId() {
            return lectureId;
        }

        public String getOwner() {
            return owner;
        }

        public String getPassword() {
            return password;
        }

        RunningLecture(String id, String password, String owner){
            lectureId = id;
            this.owner = owner;
            this.password = password;
            listeners = Collections.synchronizedList(new ArrayList());
            lecturerQuestions = Collections.synchronizedList(new ArrayList());
            listenerQuestions = Collections.synchronizedList(new ArrayList());
            answersToQuestions = new ConcurrentHashMap<>();
        }

        void addListener(String guid){
            synchronized (listeners) {
                listeners.add(guid);
            }
        }

        private void notifyListenersLectureStopped(){
            synchronized (listeners) {
                for (String listener : listeners) {
                    ClientSocketHandler listenerSocket = clients.get(listener);
                    listenerSocket.getPW().println(makeClientMessage(SocketMethods.STOPPEDLECTURE, "lecture stopped"));
                    listenerSocket.setLectureId(null);
                }
            }
        }

        private void removeQuestionsFromAnswersToQuestions(){
            synchronized (lecturerQuestions) {
                for (Object question : lecturerQuestions) {
                    if (question instanceof Question) {
                        Question q = (Question) question;
                        if (answersToQuestions.containsKey(q.getGuid())) {
                            answersToQuestions.remove(q.getGuid());
                        }
                    }
                }
            }
        }

        void notifyListenersLectureStoppedAndRemoveUsedQuestions(){
            notifyListenersLectureStopped();
            removeQuestionsFromAnswersToQuestions();
        }

        boolean removeListenerWithId(String guid){
            boolean found = false;

            synchronized (listeners) {
                for (int i = 0; i < listeners.size(); i++) {
                    String userGuid = listeners.get(i);
                    if (userGuid.equals(guid)) {
                        listeners.remove(i);
                        found = true;
                        break;
                    }
                }
            }

            return found;
        }

        private SocketQuestion addLecturerQuestion(Question question){
            SocketQuestion squestion = new SocketQuestion(question);
            lecturerQuestions.add(squestion);
            return squestion;
        }

        private SocketQuestion addLecturerQuestion(String question){
            SocketQuestion squestion = new SocketQuestion(question);
            lecturerQuestions.add(squestion);
            return squestion;
        }

        //lecturer sends his question to listeners
        void sendQuestionToListeners(Question question){
            SocketQuestion squestion = addLecturerQuestion(question);

            //send question to listeners
            synchronized (listeners) {
                for (String listenerGuid : listeners) {
                    ClientSocketHandler listenerSocket = clients.get(listenerGuid);
                    listenerSocket.getPW().println(makeClientMessage(SocketMethods.LECTURERSENTQUESTION, squestion.getQuestion()));
                }
            }

            //add entry to the answersToQuestions object
            int count = question.getAnswers().size();
            int[] answersArray = new int[count];
            answersToQuestions.put(question.getGuid(), answersArray);
        }

        //lecturer sends listener question to listeners
        void sendQuestionToListeners(String question){
            SocketQuestion squestion = addLecturerQuestion(question);

            //send question to listeners
            for (String listenerGuid : listeners) {
                ClientSocketHandler listenerSocket = clients.get(listenerGuid);
                listenerSocket.getPW().println(makeClientMessage(SocketMethods.LECTURERSENTLISTENERQUESTION, squestion.getQuestion()));
            }
        }

        private SocketQuestion addListenerQuestion(String question){
            SocketQuestion squestion = new SocketQuestion(question);
            listenerQuestions.add(squestion);
            return squestion;
        }

        void sendQuestionToLecturer(String question){
            SocketQuestion squestion = addListenerQuestion(question);

            ClientSocketHandler ownerSocker = clients.get(owner);
            if (ownerSocker != null) {
                ownerSocker.getPW().println(makeClientMessage(SocketMethods.LISTENERSENTQUESTION, squestion.getQuestion()));
            }
//            else {
                //owner is disconnected, currently do nothing
//            }
        }

        SocketQuestion getLastLecturerQuestion(){
            synchronized (lecturerQuestions) {
                if (lecturerQuestions.size() > 0) {
                    return lecturerQuestions.get(lecturerQuestions.size() - 1);
                } else {
                    return null;
                }
            }
        }

        ArrayList<Object> getListenerQuestions(){
            ArrayList<Object> questions = new ArrayList<>();
            synchronized (listenerQuestions) {
                for (SocketQuestion squestion : listenerQuestions) {
                    questions.add(squestion.getQuestion());
                }
            }
            return questions;
        }

        int getListenersCount(){
            return listeners.size();
        }

        void updateAnswersToQuestion(String questionId, Double answerIndex) throws Exception{
            if (answersToQuestions.containsKey(questionId)){
                int[] answersArray = answersToQuestions.get(questionId);
                if (answerIndex < answersArray.length){
                    answersArray[answerIndex.intValue()]++;
                }else {
                    throw new ExceptionHandler("index greater than number of questions");
                }
            }else {
                throw new ExceptionHandler("question not asked");
            }
        }

        int [] getAnswersToQuestion(String questionId) throws Exception{
            if (answersToQuestions.containsKey(questionId)){
                return answersToQuestions.get(questionId);
            }else {
                throw new ExceptionHandler("question not asked or bad question id");
            }
        }
    }

    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientSocketHandler> clients;
    private ConcurrentHashMap<String, RunningLecture> runningLectures;
    private ConcurrentHashMap<String, String> usersRunningLectures; //links users with running lectures

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

                //check if lecture exist, otherwise throw error
                Server.getDbHandler().getLectureFromUniqueId(id);
                /////

                RunningLecture rlecture = new RunningLecture(id, password, guid);
                runningLectures.put(id, rlecture);
                usersRunningLectures.put(guid, id);

                Server.getDbHandler().updateUserWithRunningLecture(guid, id, false);

                Utilities.printLog("SocketHandler: lecture started with id: " + id);


                return id;
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    void stopLecture(ClientSocketHandler lecturer) throws ExceptionHandler {
        try {
            if (lecturer.getLectureId() != null) {
                if (runningLectures.containsKey(lecturer.getLectureId())) {
                    RunningLecture rlecture = runningLectures.get(lecturer.getLectureId());
                    String ownerGuid = rlecture.getOwner();
                    if (ownerGuid.equals(lecturer.getGuid())) {

                        rlecture.notifyListenersLectureStoppedAndRemoveUsedQuestions();
                        Utilities.printLog("SocketHandler: lecture stopped with id: " + lecturer.getLectureId());

                        runningLectures.remove(lecturer.getLectureId());
                        usersRunningLectures.remove(lecturer.getGuid());
                        Server.getDbHandler().updateUserWithRunningLecture(lecturer.getGuid(), lecturer.getLectureId(), true);

                        lecturer.setLectureId(null);
                    } else {
                        throw new ExceptionHandler("you are not allowed to stop this lecture");
                    }
                }
//                else {
                    //do nothing
                    //                throw new ExceptionHandler("lecture isn't started or bad lecture id");
//                }
            }else {
                throw new ExceptionHandler("you have no running lecture");
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
                RunningLecture rlecture = runningLectures.get(id);
                if (rlecture.password == null || rlecture.password.equals(password)){
                    rlecture.addListener(client.getGuid());
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
        RunningLecture rlecture = runningLectures.get(lectureId);
        if (rlecture != null) {

            rlecture.removeListenerWithId(guid);
//            boolean found = rlecture.removeListenerWithId(guid);

//            if (!found){
//                    throw new ExceptionHandler("user is not listening to the selected lecture");
//                    better do nothing, so that clientsocket sets listeningtothelecture = null and client gets success
//            }
        }
//        else {
//                throw new ExceptionHandler("lecture isn't started or bad lecture id");
//                better do nothing, so that clientsocket sets listeningtothelecture = null and client gets success
//        }
    }

    void sendQuestionToListeners(LinkedTreeMap params, ClientSocketHandler sender) throws ExceptionHandler {
        try {
            String questionId = (String) params.get("questionId");
            if (sender.getLectureId() != null) {
                if (runningLectures.containsKey(sender.getLectureId())) {

                    //fetch Question from DB
                    Question question = Server.getDbHandler().getQuestion(questionId);
                    ////

                    RunningLecture rlecture = runningLectures.get(sender.getLectureId());
                    String owner = rlecture.owner;
                    if (owner.equals(sender.getGuid())) {
                        rlecture.sendQuestionToListeners(question);
                    } else {
                        throw new ExceptionHandler("you are not allowed to do this");
                    }
                } else {
                    throw new ExceptionHandler("lecture isn't started or bad lecture id");
                }
            }else {
                throw new ExceptionHandler("you have no running lecture");
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
                if (listener.getLectureId() != null) {
                    if (runningLectures.containsKey(listener.getLectureId())) {
                        RunningLecture rlecture = runningLectures.get(listener.getLectureId());
                        rlecture.sendQuestionToLecturer(questionText);
                    } else {
                        throw new ExceptionHandler("lecture isn't started or bad lecture id");
                    }
                }else {
                    throw new ExceptionHandler("you are not listening to any lecture");
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
            String questionText = (String)params.get("questionText");
            if (lecturer.getLectureId() != null) {
                if (runningLectures.containsKey(lecturer.getLectureId())) {
                    RunningLecture rlecture = runningLectures.get(lecturer.getLectureId());
                    String owner = rlecture.getOwner();
                    if (owner.equals(lecturer.getGuid())) {
                        rlecture.sendQuestionToListeners(questionText);
                    } else {
                        throw new ExceptionHandler("you are not allowed to do this");
                    }
                } else {
                    throw new ExceptionHandler("lecture isn't started or bad lecture id");
                }
            }else {
                throw new ExceptionHandler("you have no running lecture");
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
                String lectureId = listener.getLectureId();
                if (lectureId != null) {
                    RunningLecture rlecture = runningLectures.get(lectureId);
                    rlecture.updateAnswersToQuestion(questionId, answerIndex);
                }else {
                    throw new ExceptionHandler("you are not listening to any lecture");
                }
            }else {
                throw new ExceptionHandler("lecturer is not allower to send answer to question");
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    int[] getAnswersToQuestion(LinkedTreeMap params, ClientSocketHandler client) throws ExceptionHandler {
        //extra security is to send senderId and lectureId and check if senderId is owner of lecture, but this is not needed for now
        try {
            String questionId = (String) params.get("questionId");
            String lectureId = client.getLectureId();
            if (lectureId != null) {
                RunningLecture rlecture = runningLectures.get(lectureId);
                return rlecture.getAnswersToQuestion(questionId);
            }else {
                throw new ExceptionHandler("you are not listening to any lecture");
            }
        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    Object getLastQuestion(String lectureId) throws ExceptionHandler {
        if (runningLectures.containsKey(lectureId)) {
            RunningLecture rlecture = runningLectures.get(lectureId);
            return rlecture.getLastLecturerQuestion().getQuestion();
        }else {
            throw new ExceptionHandler("lecture isn't started or bad lecture id");
        }
    }

    Object getListenerQuestions(ClientSocketHandler client) throws ExceptionHandler{
        try {
            if (client.getLectureId() != null) {
                if (runningLectures.containsKey(client.getLectureId())) {
                    RunningLecture rlecture = runningLectures.get(client.getLectureId());
                    String owner = rlecture.getOwner();
                    if (owner.equals(client.getGuid())) {
                        return rlecture.getListenerQuestions();
                    } else {
                        throw new ExceptionHandler("you are not owner of this lecture");
                    }
                } else {
                    throw new ExceptionHandler("lecture isn't started or bad lecture id");
                }
            }else {
                throw new ExceptionHandler("you have no running lecture");
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
                    RunningLecture rlecture = runningLectures.get(sender.getLectureId());
                    return rlecture.getListenersCount();
                } else {
                    throw new ExceptionHandler("lecture isn't started or bad lecture id");
                }
            }else {
                if (sender.getIsListener()) {
                    throw new ExceptionHandler("you are not listening to any lecture");
                }else {
                    throw new ExceptionHandler("you have no running lecture");
                }
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
