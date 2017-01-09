package filip.test;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public enum SocketHandler {
    INSTANCE;
    /**
     * Created by Filip on 10/4/2016.
     * unique (INSTANCE)
     * creating ClientSocketHandler threads for each client connected
     */
    private ServerSocket serverSocket;
    private Map<String, ClientSocketHandler> clients;
    private Map<String, Object> runningLectures;

    int port = 8210;

    SocketHandler(){
        try {
            serverSocket = new ServerSocket(port);
            clients = new HashMap<>();
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

    void addClient(ClientSocketHandler client, String guid) throws ExceptionHandler{
        Boolean exists = Server.dbHandler.checkIfUserExists(guid);
        if (exists) {
            clients.put(guid, client);
        }else {
            throw new ExceptionHandler("user doesn't exist");
        }
    }
    void closeClient(ClientSocketHandler client){
        String guid = client.guid;
        if (null != guid) {
            clients.remove(guid);
            //clearRunningLecture(guid);
        }
        try {
            client.socket.close();
            client.runningOnThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
