package filip.test;

import java.io.*;
import java.net.Socket;

/**
 * Created by Filip on 10/4/2016.
 */
public class ClientSocketHandler implements Runnable{

    Socket socket;
    Thread runningOnThread;
    String guid;

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

                    if (null == guid) {
                        pw.println("What's your guid?");

                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String str = br.readLine();
                        pw.println("Hello, " + str);

                        guid = str;
                        SocketHandler.INSTANCE.addClient(this, guid);
                    }else {
                        pw.println("What do we do now Mr. " + guid + "?");
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String str = br.readLine();
                    }

                } catch (IOException e) {
                    System.out.println("Client disconnected with guid: " + guid);
                    SocketHandler.INSTANCE.closeClient(this, guid);

//                    import org.json.simple.JSONObject
//                    JSONParser parser = new JSONParser();
//                    JSONObject json = (JSONObject) parser.parse(stringToParse);

                    break;
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
