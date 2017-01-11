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
                String responseMessage;
                do {
                    responseMessage = br.readLine();
                    System.out.println("server says:" + responseMessage);
                    if (responseMessage == null){
                        break;
                    }
                }while (responseMessage.contains("\"type\":\"message\"") && responseMessage.contains("\"method\":"));
            }

            badCommand = false;
            System.out.println("");
            System.out.println("choose command: ");
            System.out.println("1x: login");
            System.out.println("2x: lecture start");
            System.out.println("3x: lecture end");
            System.out.println("40x: listen to lecture");
            System.out.println("41x: stop listen lecture");
            System.out.println("50x: send question to listeners");
            System.out.println("51x: send question to lecturer");
            System.out.println("52x: send listener question to listeners");
            System.out.println("53x: send answer to question");
            System.out.println("54x: get answers");
            System.out.println("900: close socket");
            System.out.println("901: open socket");
            System.out.println("999: read new line from server (could freeze client)");

            String userInput = userInputBR.readLine();
            switch (userInput){
                case "999":
                    break; // skip
                case "10": {
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "login");
                    message.put("params", "83528b7a13aff88491a7772acddb6d22");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "11": {
                    //bad guid
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "login");
                    message.put("params", "83528b7a13aff88491a7ddb6d22");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "12": {
                    //bad params
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "7aba4784-ae26-4424-8b43-5b89369bb0b5");
                    message.put("method", "login");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "20": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "ilc69h");
                    params.put("password", "nekipass");
                    message.put("method", "startLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "21": {
                    //start without password
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "ilc69h");
                    message.put("method", "startLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "22": {
                    //start without password another one
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "kmttmu");
                    message.put("method", "startLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "23": {
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
                case "24": {
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
                case "25": {
                    //bad params (no params)
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "startLecture");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "30": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "ilc69h");
                    message.put("method", "stopLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "31": {
                    //stop other one
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "kmttmu");
                    message.put("method", "stopLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "400": {
                    //listen with password
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "ilc69h");
                    params.put("password", "nekipass");
                    message.put("method", "listenLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "401": {
                    //listen without password
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "ilc69h");
                    message.put("method", "listenLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "402": {
                    //bad id
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", "ed93aa4");
                    params.put("password", "nekipass");
                    message.put("method", "listenLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "410": {
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "stopListenLecture");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "500": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("lectureId", "ilc69h");
                    params.put("questionId", "8377a0c6-882a-48f1-91d4-09165b7b5154");
                    message.put("method", "sendQuestionToListeners");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "501": {
                    //bad lecture id
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("lectureId", "ilc69");
                    params.put("questionId", "8377a0c6-882a-48f1-91d4-09165b7b5154");
                    message.put("method", "sendQuestionToListeners");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "502": {
                    //bad question id
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("lectureId", "ilc69h");
                    params.put("questionId", "8377a0c6-882a-48f1-91d4-09165b7b515");
                    message.put("method", "sendQuestionToListeners");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "510": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("lectureId", "ilc69h");
                    params.put("questionText", "Some text here is inserted");
                    message.put("method", "sendQuestionToLecturer");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "520": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("lectureId", "ilc69h");
                    params.put("questionText", "Some text here is inserted");
                    message.put("method", "sendListenerQuestionToListeners");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "530": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    params.put("questionId", "8377a0c6-882a-48f1-91d4-09165b7b5154");
                    params.put("answerIndex", 1);
                    message.put("method", "sendAnswerToQuestion");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "540": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    params.put("questionId", "8377a0c6-882a-48f1-91d4-09165b7b5154");
                    message.put("method", "getAnswersToQuestion");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "900":{
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "close");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "901":{
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
