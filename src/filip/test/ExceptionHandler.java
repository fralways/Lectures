package filip.test;

/**
 * Created by Filip on 12/30/2016.
 */
public class ExceptionHandler extends Exception {
    int statusCode;
    String message;

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ExceptionHandler(String message){
        super(message);
        this.message = message;
    }

    public ExceptionHandler(String message, int statusCode){
        super(message);
        this.message = message;
        this.statusCode = statusCode;
    }

    public void setStatusCode(int statusCode){
        this.statusCode = statusCode;
    }
}
