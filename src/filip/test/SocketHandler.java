package filip.test;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public enum SocketHandler {
    INSTANCE;
    /**
     * Created by Filip on 10/4/2016.
     */
    private ServerSocket serverSocket;
    private Map<String, ClientSocketHandler>clients;

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

    void addClient(ClientSocketHandler client, String guid){
        clients.put(guid, client);
    }
    void closeClient(ClientSocketHandler client, String guid){
        if (null != guid) {
            clients.remove(guid);
        }
        try {
            client.socket.close();
            client.runningOnThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}