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
        while (true) {
            try {
                if (null == guid) {
                    pw.println(makeResponse(true, "What is your guid?"));
                    String str = br.readLine();
                    try {
                        SocketHandler.INSTANCE.addClient(this, str);
                        pw.println(makeResponse(true, "Hello, " + str + ". Select action"));
                        guid = str;
                    } catch (ExceptionHandler exceptionHandler) {
                        Utilities.printLog("ClientSocket: " + exceptionHandler.message);
                        pw.println(makeResponse(false, exceptionHandler.message));
                    }

                } else {
                    //user should now select operation
                    String clientMessage = br.readLine();

                    Map<String, Object> message;
                    try {
                        message = Utilities.readJsonApplication(clientMessage);
                        checkMessage(message);
                    }
                    catch (ExceptionHandler e){
                        Utilities.printLog("ClientSocket: message not in good format: "+ clientMessage);
                        pw.println(makeResponse(false, e.message));
                        continue;
                    }

                    String method = (String) message.get("method");
                    try {
                        switch (method){
                            case "startLecture":
                                if (message.get("params") instanceof LinkedTreeMap) {
                                    LinkedTreeMap params = (LinkedTreeMap) message.get("params");
                                    handleStartLecture(params);
                                    pw.println(makeResponse(true));
                                }else {
                                    pw.println(makeResponse(false, "bad params"));
                                }
                                break;
                            case "close":
//                                pw.println(makeResponse(true));
                                SocketHandler.INSTANCE.closeClient(this);
                                break;
                            default:
                                pw.println(makeResponse(false, "method not found"));
                                break;
                        }
                    }catch (ExceptionHandler e) {
                        pw.println(makeResponse(false, e.message));
                    }
                }

            } catch (IOException e) {
                Utilities.printLog("Client disconnected with guid: " + guid);
                SocketHandler.INSTANCE.closeClient(this);
                break;
            }
        }
    }

    void checkMessage(Map<String, Object> message) throws ExceptionHandler {
        if (message != null && message.containsKey("method")) {

        }else {
            throw new ExceptionHandler("message not in good format");
        }
    }

    void handleStartLecture(LinkedTreeMap params) throws ExceptionHandler {
        try {
            String id = (String) params.get("id");
            String password = (String) params.get("password");
            if (id != null){
                Lecture lecture = Server.dbHandler.getLecture(id);
            }else {
                throw new ExceptionHandler("bad params");
            }

        }catch (ExceptionHandler e){
            throw e;
        } catch (Exception e){
            throw new ExceptionHandler("bad params");
        }
    }

    String makeResponse(boolean ok, Object message){
        HashMap <String, Object> response = new HashMap<>();
        response.put("ok", ok);
        response.put("message", message);
        return Utilities.mapToJson(response);
    }

    String makeResponse(boolean ok){
        HashMap <String, Object> response = new HashMap<>();
        response.put("ok", ok);
        return Utilities.mapToJson(response);
    }
}
