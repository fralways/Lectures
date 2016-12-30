package filip.test;

/**
 * Created by Filip on 12/30/2016.
 */
public class ExceptionHandler extends Exception {
    int statusCode;
    String message;

    ExceptionHandler(String message){
        super(message);
        this.message = message;
    }

    ExceptionHandler(String message, int statusCode){
        super(message);
        this.message = message;
        this.statusCode = statusCode;
    }

    void setStatusCode(int statusCode){
        this.statusCode = statusCode;
    }
}
