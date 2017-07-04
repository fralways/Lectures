package filip.test;

import java.net.HttpURLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Filip on 7/2/2017.
 */
public class ListenerQuestion {
    private String guid;
    private String question;
    private String lectureId;
    private Date date;
    private boolean shared;

    public String getGuid() {
        return guid;
    }

    public String getQuestion() { return question; }

    public String getLectureId() { return lectureId; }

    public Date getDate() { return date; }

    ListenerQuestion(Map<String, Object> parameters, String guid, String lectureId, Date date, boolean shared) throws ExceptionHandler{
        checkIfCorrectEntry(parameters);
        String question = (String) parameters.get("question");
        this.guid = guid;
        this.question = question;
        this.lectureId = lectureId;
        this.date = date;
        this.shared = shared;
    }

    ListenerQuestion(ResultSet rs) throws ExceptionHandler{
        try {
            List<HashMap<String, Object>> questionDictionaryList = Utilities.convertResultSetToList(rs);

            if (questionDictionaryList.size() == 0) {
                throw new ExceptionHandler("ListenerQuestion: does not exist", HttpURLConnection.HTTP_NOT_FOUND);
            }

            HashMap<String, Object> questionDictionary = questionDictionaryList.get(0);
            String question = (String) questionDictionary.get("question");
            String lectureId = (String) questionDictionary.get("lecture_id");
            Date date = (Date) questionDictionary.get("date");

            boolean shared = (questionDictionary.get("shared") != null) ? (boolean) questionDictionary.get("shared"): false;

            this.guid = (String) questionDictionary.get("guid");
            this.question = question;
            this.lectureId = lectureId;
            this.date = date;
            this.shared = shared;
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler("ListenerQuestion: dbmigration error", HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    static List<ListenerQuestion> getQuestions(ResultSet rs) throws ExceptionHandler{
        try {
            List<HashMap<String, Object>> questionDictionaryList = Utilities.convertResultSetToList(rs);

            if (questionDictionaryList.size() == 0) {
                throw new ExceptionHandler("ListenerQuestion: does not exist", HttpURLConnection.HTTP_NOT_FOUND);
            }

            List<ListenerQuestion> questions = new ArrayList<>();

            for (HashMap<String, Object> questionDictionary : questionDictionaryList) {
                String guid = (String) questionDictionary.get("guid");
                String question = (String) questionDictionary.get("question");
                String lectureId = (String) questionDictionary.get("lecture_id");
                Date date = (Date) questionDictionary.get("date");
                boolean shared = (boolean) questionDictionary.get("shared");
                HashMap<String, Object> params = new HashMap<>();
                params.put("question", question);

                ListenerQuestion newQuestion = new ListenerQuestion(params, guid, lectureId, date, shared);
                questions.add(newQuestion);
            }

            return questions;
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler("ListenerQuestion: dbmigration error", HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    void checkIfCorrectEntry(Map<String, Object> parameters) throws ExceptionHandler {
        if (parameters.get("question") instanceof String) {

        }else {
            throw new ExceptionHandler("ListenerQuestion: bad params", HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }

    String questionToJson(){
        return Utilities.mapToJson(this);
    }
}
