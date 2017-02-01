package filip.test;

import com.google.gson.internal.LinkedTreeMap;
import java.io.*;
import java.net.Socket;
import java.util.Map;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 10/4/2016.
 */
public class ClientSocketHandler implements Runnable{

    Socket socket;
    Thread runningOnThread;
    String guid;
    BufferedReader br;
    OutputStream os;
    PrintWriter pw;
    String listeningToTheLecture;
    boolean isListener;
    private static long listenerNumber = 0;

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
        pw.println(SocketHandler.makeClientMessage(SocketHandler.SocketMethods.LOGIN, "What is your guid?"));
        while (true) {
            try {
                String clientMessage = br.readLine();
                Map<String, Object> message;
                try {
                    message = Utilities.readJsonApplication(clientMessage);
                    checkMessage(message);
                }
                catch (ExceptionHandler e){
                    Utilities.printLog("ClientHandler: user not logged in or message not in good format: "+ clientMessage);
                    pw.println(SocketHandler.makeClientResponse(false, e.message));
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
                            if (message.get("params") instanceof String) {
                                String guid = (String) message.get("params");
                                SocketHandler.INSTANCE.loginClient(this, guid);
//                                this.guid = guid;
                                pw.println(SocketHandler.makeClientResponse(true, "Hello, " + this.guid + ". Select action"));
                                Utilities.printLog("ClientHandler: client logged in with guid " + this.guid);
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        case SOCKET_STARTLECTURE:
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.startLecture(params, guid);
                                pw.println(SocketHandler.makeClientResponse(true, "lecture started"));
                            } else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        case SOCKET_STOPLECTURE:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.stopLecture(params, guid);
                                pw.println(SocketHandler.makeClientResponse(true, "lecture stopped"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_LISTENLECTURE:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                if (listeningToTheLecture != null){
                                    pw.println(SocketHandler.makeClientResponse(false, "user already listens to the lecture"));
                                }else {
                                    SocketHandler.INSTANCE.listenLecture(params, this);
                                    pw.println(SocketHandler.makeClientResponse(true, "added to lecture"));
                                }
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_STOPLISTENLECTURE:{
                            if (listeningToTheLecture != null) {
                                SocketHandler.INSTANCE.stopListenLecture(listeningToTheLecture, guid);
                                listeningToTheLecture = null;
                                pw.println(SocketHandler.makeClientResponse(true, "stopped listening lecture"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "client is not listening to any lecture"));
                            }
                            break;
                        }
                        case SOCKET_SENDQUESTIONTOLISTENERS:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendQuestionToListeners(params, guid);
                                pw.println(SocketHandler.makeClientResponse(true, "sent question to the listeners"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_SENDQUESTIONTOLECTURER:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendQuestionToLecturer(params, listeningToTheLecture);
                                pw.println(SocketHandler.makeClientResponse(true, "sent question to the lecturer"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_SENDLISTENERQUESTIONTOLISTENERS:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendListenerQuestionToListeners(params, guid);
                                pw.println(SocketHandler.makeClientResponse(true, "sent listener's question to the listeners"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_SENDANSWERTOQUESTION:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.sendAnswerToQuestion(params);
                                pw.println(SocketHandler.makeClientResponse(true, "sent answer"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_GETANSWERSTOQUESTION:{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                int[] answers = SocketHandler.INSTANCE.getAnswersToQuestion(params);
                                pw.println(SocketHandler.makeClientResponse(true, answers));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case SOCKET_CLOSE:
                            Utilities.printLog("ClientHandler: client disconnected with guid: " + guid);
                            SocketHandler.INSTANCE.closeClient(this);
                            guid = null;
                            listeningToTheLecture = null;
                            break;
                        default:
                            pw.println(SocketHandler.makeClientResponse(false, "method not found"));
                            break;
                    }
                }catch (ExceptionHandler e) {
                    pw.println(SocketHandler.makeClientResponse(false, e.message));
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
                            //all good
                            break;
                        default:
                            throw new ExceptionHandler("you are not allowed to do this");
                    }
                }
            }else {
                if (message.get("params") instanceof String && message.get("method").equals(SOCKET_LOGIN)) {

                }else {
                    throw new ExceptionHandler("message not in good format or user not logged in");
                }
            }
        }else {
            throw new ExceptionHandler("message not in good format");
        }
    }

    static long getNewListenerNumber(){
        long num = listenerNumber++;
        return num;
    }
}
