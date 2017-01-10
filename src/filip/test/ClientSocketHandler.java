package filip.test;

import com.google.gson.internal.LinkedTreeMap;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

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

        pw.println(SocketHandler.makeClientMessage(SocketHandler.SocketMethods.MESSAGE, "What is your guid?"));

        while (true) {
            try {
                String clientMessage = br.readLine();

                Map<String, Object> message;
                try {
                    message = Utilities.readJsonApplication(clientMessage);
                    checkMessage(message);
                }
                catch (ExceptionHandler e){
                    Utilities.printLog("ClientSocket: message not in good format: "+ clientMessage);
                    pw.println(SocketHandler.makeClientResponse(false, e.message));
                    continue;
                }

                String method = (String) message.get("method");
                try {
                    switch (method){
                        case "login":
                            if (message.get("params") instanceof String) {
                                String guid = (String) message.get("params");
                                SocketHandler.INSTANCE.addClient(this, guid);
                                pw.println(SocketHandler.makeClientResponse(true, "Hello, " + guid + ". Select action"));
                                this.guid = guid;
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        case "startLecture":
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.startLecture(params, guid);
                                pw.println(SocketHandler.makeClientResponse(true, "lecture started"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        case "stopLecture":{
                            if (message.get("params") instanceof LinkedTreeMap) {
                                LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                SocketHandler.INSTANCE.stopLecture(params);
                                pw.println(SocketHandler.makeClientResponse(true, "lecture stopped"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "bad params"));
                            }
                            break;
                        }
                        case "listenLecture":{
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
                        case "stopListenLecture":{
                            if (listeningToTheLecture != null) {
                                SocketHandler.INSTANCE.stopListenLecture(listeningToTheLecture, guid);
                                listeningToTheLecture = null;
                                pw.println(SocketHandler.makeClientResponse(true, "stopped listening lecture"));
                            }else {
                                pw.println(SocketHandler.makeClientResponse(false, "client is not listening to any lecture"));
                            }
                            break;
                        }
                        case "close":
                            Utilities.printLog("ClientHandler: client disconnected with guid: " + guid);
                            SocketHandler.INSTANCE.closeClient(this);
                            break;
                        default:
                            pw.println(SocketHandler.makeClientResponse(false, "method not found"));
                            break;
                    }
                }catch (ExceptionHandler e) {
                    pw.println(SocketHandler.makeClientResponse(false, e.message));
                }

            } catch (IOException e) {
                Utilities.printLog("ClientHandler: client disconnected with guid: " + guid);
                SocketHandler.INSTANCE.closeClient(this);
                break;
            }
        }
    }

    void checkMessage(Map<String, Object> message) throws ExceptionHandler {
        if (message != null && message.containsKey("method")) {
            if (guid != null){

            }else {
                if (message.get("params") instanceof String && message.get("method").equals("login")) {

                }else {
                    throw new ExceptionHandler("message not in good format or user not logged in");
                }
            }
        }else {
            throw new ExceptionHandler("message not in good format");
        }
    }


}
