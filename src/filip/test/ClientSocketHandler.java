package filip.test;

import java.io.*;
import java.net.Socket;

/**
 * Created by Filip on 10/4/2016.
 */
public class ClientSocketHandler implements Runnable{

    Socket socket;

    ClientSocketHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {

        OutputStream os;
        try {
            os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os, true);
            while (true){
                try {
                    pw.println("What's you name?");

                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String str = br.readLine();

                    pw.println("Hello, " + str);

                    System.out.println("Just said hello to:" + str);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
