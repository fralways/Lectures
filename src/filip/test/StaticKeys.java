package filip.test;

/**
 * Created by Filip on 8/10/2016.
 */

class StaticKeys {
    final static String EXCEPTION_METHODNOTSUPPORTED = "http method not supported";
    final static String EXCEPTION_BADTOKEN = "bad token";
    final static String EXCEPTION_BADREQUEST = "bad params";

    //jwt
    final static String JWT_SECRET = "jsyvi-123jsdcxna!-ks";

    //socket methods
    final static String SOCKET_LOGIN = "login";
    final static String SOCKET_STARTLECTURE = "startLecture";
    final static String SOCKET_STOPLECTURE = "stopLecture";
    final static String SOCKET_LISTENLECTURE = "listenLecture";
    final static String SOCKET_STOPLISTENLECTURE = "stopListenLecture";
    final static String SOCKET_SENDQUESTIONTOLISTENERS = "sendQuestionToListeners";
    final static String SOCKET_SENDQUESTIONTOLECTURER = "sendQuestionToLecturer";
    final static String SOCKET_SENDLISTENERQUESTIONTOLISTENERS = "sendListenerQuestionToListeners";
    final static String SOCKET_SENDANSWERTOQUESTION = "sendAnswerToQuestion";
    final static String SOCKET_GETANSWERSTOQUESTION = "getAnswersToQuestion";
    final static String SOCKET_GETLASTQUESTION = "getLastQuestion";
    final static String SOCKET_CLOSE = "close";




}
