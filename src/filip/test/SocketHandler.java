package filip.test;

import java.io.*;
import java.net.*;

/**
 * Created by Filip on 10/4/2016.
 */
public class SocketHandler {
    ServerSocket serverSocket;
    boolean run;

    int port = 81;

    SocketHandler() throws IOException{
        System.out.println("Creating server socket on port " + port);
        serverSocket = new ServerSocket(port);
    }

    void start() {
        run = true;
        while (run) try {

            Socket socket = serverSocket.accept();
            ClientSocketHandler client = new ClientSocketHandler(socket);
            new Thread(client).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stop(){
        run = false;
    }
}
