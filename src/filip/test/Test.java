package filip.test;

/**
 * Created by Filip on 10/4/2016.
 */



        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.io.PrintWriter;
        import java.net.Socket;
        import java.util.HashMap;
        import java.util.Map;
        import java.util.Objects;

public class Test {

    Socket socket;
    BufferedReader br;
    PrintWriter out;
    BufferedReader userInputBR;

    final String host = "localhost";
    final int portNumber = 8210;
    boolean badCommand = false;

    void startSocket() throws IOException {
        System.out.println("Creating socket to '" + host + "' on port " + portNumber);
        socket = new Socket(host, portNumber);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        userInputBR = new BufferedReader(new InputStreamReader(System.in));
    }

    void doSomething() throws IOException {
        while (true) {
            if (!badCommand) {
                String responseMessage = br.readLine();
                System.out.println("server says:" + responseMessage);
            }

            badCommand = false;
            System.out.println("");
            System.out.println("choose command: ");
            System.out.println("1x: login");
            System.out.println("2x: lecture start");
            System.out.println("90: close socket");
            System.out.println("91: open socket");
            System.out.println("999: read new line from server (could freeze client");

            String userInput = userInputBR.readLine();
            switch (userInput){
                case "999":
                    break; // skip
                case "10":
                    out.println("83528b7a13aff88491a7772acddb6d22");
                    break;
                case "11":
                    out.println("bad user guid");
                    break;
                case "20": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "7aba4784-ae26-4424-8b43-5b89369bb0b5");
                    params.put("password", "nekipass");
                    message.put("method", "startLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "21": {
                    //bad method
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "0000");
                    message.put("method", "startle");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "22": {
                    //bad lecture id
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "0000");
                    params.put("password", "nekipass");
                    message.put("method", "startLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "23": {
                    //bad params (no params)
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "startLecture");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "90":{
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "close");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "91":{
                    startSocket();
                    break;
                }
                default:
                    //do nothing;
                    System.out.println("command not recognized");
                    badCommand = true;
                    break;
            }
        }
    }

    public static void main(String args[]) throws IOException {
        Test client = new Test();
        client.startSocket();
        client.doSomething();
    }
}
