package filip.test.socket;

import com.google.gson.internal.LinkedTreeMap;
import filip.test.ExceptionHandler;
import filip.test.Utilities;

import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.Socket;
import java.util.Map;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 10/4/2016.
 */
public class ClientSocketHandler implements Runnable{

    static int listenerCountTEST = 0;

    private Socket socket;
    private Thread runningOnThread;
    private String guid;
    private BufferedReader br;
    private OutputStream os;
    private PrintWriter pw;
    private boolean isListener;
    private String lectureId;

    public void setRunningOnThread(Thread t){
        runningOnThread = t;
    }

    public synchronized void PWPrintln(String s){
        pw.println(s);
    }

    public boolean getIsListener() {
        return isListener;
    }

    public void setIsListener(boolean listener) {
        isListener = listener;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String id) {
        guid = id;
    }

    public String getLectureId() {
        return lectureId;
    }

    public void setLectureId(String id) {
        lectureId = id;
    }

    ClientSocketHandler(Socket socket){
        this.socket = socket;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            os = socket.getOutputStream();
            pw = new PrintWriter(os, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        PWPrintln(SocketHandler.makeClientMessage(SocketHandler.SocketMethods.LOGIN, "What is your guid?"));
        while (true) {
            try {
                String clientMessage = br.readLine();
                Map<String, Object> message;
                try {
                    Utilities.printLog("ClientSocketHandler: Client sent: " + clientMessage);
                    message = Utilities.readJsonApplication(clientMessage);
                    checkMessage(message);
                }
                catch (ExceptionHandler e){
                    Utilities.printLog("ClientHandler: user not logged in or message not in good format: "+ clientMessage);
                    PWPrintln(SocketHandler.makeClientResponse(false, e.getMessage()));
                    if (clientMessage == null){
                        Utilities.printLog("ClientHandler: client sent message = null - disconnecting");
                        SocketHandler.INSTANCE.closeClient(this);
                        Utilities.printLog("ClientHandler: client disconnected with guid: " + guid);
                        break;
                    }
                    continue;
                }

                String method = (String) message.get("method");
                try {
                    switch (method){
                        case SOCKET_LOGIN:
                            if (message.get("params") instanceof LinkedTreeMap) {
                                if (this.guid == null) {
                                    LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                    SocketHandler.INSTANCE.loginClient(this, params);
                                    PWPrintln(SocketHandler.makeClientResponse(true, "Hello, " + this.guid + ". Select action"));
                                    Utilities.printLog("ClientHandler: client logged in with guid " + this.guid);
                                }else {
                                    PWPrintln(SocketHandler.makeClientResponse(false, "you are already logged in"));
                                }
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        case SOCKET_STARTLECTURE:
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                lectureId = SocketHandler.INSTANCE.startLecture(params, guid);
                                PWPrintln(SocketHandler.makeClientResponse(true, "lecture started"));
                            } else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        case SOCKET_STOPLECTURE:{
                            SocketHandler.INSTANCE.stopLecture(this);
                            PWPrintln(SocketHandler.makeClientResponse(true, "lecture stopped"));
                            break;
                        }
                        case SOCKET_LISTENLECTURE:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                if (isListener) {
                                    if (lectureId != null) {
                                        PWPrintln(SocketHandler.makeClientResponse(false, "user already listens to the lecture"));
                                    } else {
                                        SocketHandler.INSTANCE.listenLecture(params, this);
                                        PWPrintln(SocketHandler.makeClientResponse(true, "added to lecture"));
                                    }
                                }else {
                                    PWPrintln(SocketHandler.makeClientResponse(true, "lecturer cannot listen to lecture"));
                                }
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_STOPLISTENLECTURE:{
                            if (lectureId != null) {
                                if (isListener) {
                                    SocketHandler.INSTANCE.stopListenLecture(lectureId, guid);
                                    lectureId = null;
                                    PWPrintln(SocketHandler.makeClientResponse(true, "stopped listening lecture"));
                                }else {
                                    PWPrintln(SocketHandler.makeClientResponse(true, "lecturer cannot stop listen to lecture"));
                                }
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "client is not listening to any lecture"));
                            }
                            break;
                        }
                        case SOCKET_SENDQUESTIONTOLISTENERS:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendQuestionToListeners(params, this);
                                PWPrintln(SocketHandler.makeClientResponse(true, "sent question to the listeners"));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_SENDQUESTIONTOLECTURER:{
                            Utilities.printLog("wants to send question to lecturer");
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendQuestionToLecturer(params, this);
                                PWPrintln(SocketHandler.makeClientResponse(true, "sent question to the lecturer"));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_SENDLISTENERQUESTIONTOLISTENERS:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendListenerQuestionToListeners(params, this);
                                PWPrintln(SocketHandler.makeClientResponse(true, "sent listener's question to the listeners"));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_SENDANSWERTOQUESTION:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendAnswerToQuestion(params, this);
                                PWPrintln(SocketHandler.makeClientResponse(true, "sent answer"));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_GETANSWERSTOQUESTION:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                int[] answers = SocketHandler.INSTANCE.getAnswersToQuestion(params, this);
                                PWPrintln(SocketHandler.makeClientResponse(true, answers));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_GETLASTQUESTION:{
                            if (lectureId != null) {
                                Object question = SocketHandler.INSTANCE.getLastQuestion(lectureId);
                                PWPrintln(SocketHandler.makeClientResponse(true, question));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "user is not listening to any lecture"));
                            }
                            break;
                        }
                        case SOCKET_GETLISTENERQUESTIONS:{
                            if (!isListener) {
                                Object questions = SocketHandler.INSTANCE.getListenerQuestions(this);
                                PWPrintln(SocketHandler.makeClientResponse(true, questions));
                            }else {
                                PWPrintln(SocketHandler.makeClientResponse(false, "listener is not allowed to do this"));
                            }
                            break;
                        }
                        case SOCKET_GETNUMOFLISTENERS:
                            int num = SocketHandler.INSTANCE.getNumOfListeners(this);
                            PWPrintln(SocketHandler.makeClientResponse(true, num));
                            break;
                        case SOCKET_CLOSE:
                            Utilities.printLog("ClientHandler: client disconnected with guid: " + guid);
                            SocketHandler.INSTANCE.closeClient(this);
                            guid = null;
                            lectureId = null;
                            break;
                        default:
                            PWPrintln(SocketHandler.makeClientResponse(false, "method not found"));
                            break;
                    }
                }catch (ExceptionHandler e) {
                    PWPrintln(SocketHandler.makeClientResponse(false, e.getMessage()));
                }

            } catch (IOException e) {
                if (guid != null) {
                    Utilities.printLog("ClientHandler: client disconnected with guid: " + guid);
                    SocketHandler.INSTANCE.closeClient(this);
                }
                break;
            }
        }
    }

    void checkMessage(Map<String, Object> message) throws ExceptionHandler {
        if (message != null && message.containsKey("method")) {
            if (guid != null){
                if (isListener) {
                    String method = (String) message.get("method");
                    switch (method) {
                        case SOCKET_LOGIN:
                        case SOCKET_CLOSE:
                        case SOCKET_LISTENLECTURE:
                        case SOCKET_SENDANSWERTOQUESTION:
                        case SOCKET_SENDQUESTIONTOLECTURER:
                        case SOCKET_STOPLISTENLECTURE:
                        case SOCKET_GETLASTQUESTION:
                        case SOCKET_GETNUMOFLISTENERS:
                            //all good
                            break;
                        default:
                            throw new ExceptionHandler("you are not allowed to do this");
                    }
                }
            }else {
                if (message.get("params") instanceof LinkedTreeMap && ((LinkedTreeMap) message.get("params")).get("guid") instanceof String && message.get("method").equals(SOCKET_LOGIN)) {

                }else {
                    throw new ExceptionHandler("message not in good format or user not logged in");
                }
            }
        }else {
            throw new ExceptionHandler("message not in good format");
        }
    }

    void closeSocket(){
        try {
            socket.close();
        }catch (IOException e) {
            PWPrintln(SocketHandler.makeClientResponse(false, "failed to close socket"));
            Utilities.printLog("ClientSocketHandler: Failed to close socket");
        }
    }

    void interruptThread(){
        runningOnThread.interrupt();
    }
}
