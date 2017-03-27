package filip.test;

import org.postgresql.jdbc.PgArray;

import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 12/27/2016.
 */
public class Question {
    private String guid;
    private String question;
    private ArrayList<String> answers;
    private int correctIndex;
    private int duration;

    public String getGuid() {
        return guid;
    }

    public String getQuestion() {
        return question;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public int getDuration() {
        return duration;
    }

    public ArrayList<String> getAnswers() {
        return answers;
    }

    Question(Map<String, Object> parameters, String guid) throws ExceptionHandler{
        checkIfCorrectEntry(parameters);
        String question = (String) parameters.get("question");
        Double correctIndex = (Double)parameters.get("correctindex");
        Double duration = (Double)parameters.get("duration");
        ArrayList<String> answers = (ArrayList<String>)parameters.get("answers");
        this.guid = guid;
        this.question = question;
        this.answers = answers;
        this.correctIndex = correctIndex.intValue();
        this.duration = duration.intValue();
    }

    Question(ResultSet rs) throws ExceptionHandler{
        try {
            List<HashMap<String, Object>> questionDictionaryList = Utilities.convertResultSetToList(rs);

            if (questionDictionaryList.size() == 0) {
                throw new ExceptionHandler("Question: does not exist", HttpURLConnection.HTTP_NOT_FOUND);
            }

            HashMap<String, Object> questionDictionary = questionDictionaryList.get(0);
            String question = (String) questionDictionary.get("question");
            int correctIndex = (Integer) questionDictionary.get("correctindex");
            Double duration = (Double) questionDictionary.get("duration");

            PgArray answersPgArray = (PgArray) questionDictionary.get("answers");
            String[] answersStringArray = (String[]) answersPgArray.getArray();
            ArrayList<String> answers = new ArrayList<>();
            answers.addAll(Arrays.asList(answersStringArray));

            this.guid = (String) questionDictionary.get("guid");
            this.question = question;
            this.answers = answers;
            this.correctIndex = correctIndex;
            this.duration = duration.intValue();
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler("Question: dbmigration error", HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    void checkIfCorrectEntry(Map<String, Object> parameters) throws ExceptionHandler {
        boolean badEntry = true;
        if (parameters.get("question") instanceof String && parameters.get("duration") instanceof Double &&
            parameters.get("correctindex") instanceof Double && parameters.get("answers") instanceof ArrayList){

            ArrayList<String> answers = (ArrayList<String>)parameters.get("answers");
            Double correctIndex = (Double) parameters.get("correctindex");

            //check answers type
            if (answers.size() > 0) {
                for (String answer : answers) {
                    badEntry = false;
                    if (!(answer instanceof String)) {
                        badEntry = true;
                        break;
                    }
                }
            }else {
                throw new ExceptionHandler("Question: need to provide at least one answer");
            }

            if (correctIndex > answers.size() - 1 || correctIndex < 0){
                throw new ExceptionHandler("Question: bad index");
            }
        }

        if (badEntry) {
            throw new ExceptionHandler("Question: bad params", HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }

    String questionToJson(){
        return Utilities.mapToJson(this);
    }
}
