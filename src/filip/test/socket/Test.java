package filip.test.socket;

/**
 * Created by Filip on 10/4/2016.
 */

import filip.test.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Test {

    Socket socket;
    BufferedReader br;
    PrintWriter out;
    BufferedReader userInputBR;

    static int listenerCount = 0;

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
                }while (responseMessage.contains("\"type\":\"message\"") && responseMessage.contains("\"LOGIN\":"));
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
            System.out.println("55x: get last question lecturer sent");
            System.out.println("56x: get questions listeners sent");
            System.out.println("57x: get number of listeners");
            System.out.println("900: close socket");
            System.out.println("901: open socket");
            System.out.println("999: read new line from server (could freeze client)");

            String userInput = userInputBR.readLine();

            String lectureId = "a6psc5";
//            String lectureId = "ukbmja";
            String questionId = "4988ae3f-5af3-4de1-9452-597d03d7ff80";
            switch (userInput){
                case "999":
                    break; // skip
                case "10": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    message.put("method", "login");
                    params.put("guid", "83528b7a13aff88491a7772acddb6d22");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "11": {
                    //login as listener
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    message.put("method", "login");
//                    params.put("guid", "SOMELISTENERID" + ClientSocketHandler.listenerCountTEST++);
                    params.put("guid", "SOMELISTENERID");
                    params.put("listener", true);
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "20": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", lectureId);
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
                    params.put("id", lectureId);
                    message.put("method", "startLecture");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "30": {
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "stopLecture");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "400": {
                    //listen with password
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", lectureId);
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
                    params.put("id", lectureId);
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
                    params.put("questionId", questionId);
                    message.put("method", "sendQuestionToListeners");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "510": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("question", "Some text here is ins");
                    message.put("method", "sendQuestionToLecturer");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "520": {
                    System.out.println("enter question guid: ");
                    String responseMessage = userInputBR.readLine();


                    Map<String, Object> message = new HashMap<>();
                    Map<String, String> params = new HashMap<>();
                    params.put("id", responseMessage);
                    message.put("method", "sendListenerQuestionToListeners");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "530": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    params.put("questionId", questionId);
                    params.put("answerIndex", 0);
                    message.put("method", "sendAnswerToQuestion");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "540": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    params.put("questionId", questionId);
                    message.put("method", "getAnswersToQuestion");
                    message.put("params", params);
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "550": {
                    Map<String, Object> message = new HashMap<>();
                    message.put("method", "getLastQuestion");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "560": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    message.put("method", "getListenerQuestions");
                    String json = Utilities.mapToJson(message);
                    out.println(json);
                    break;
                }
                case "570": {
                    Map<String, Object> message = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    message.put("method", "getNumOfListeners");
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
