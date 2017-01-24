package filip.test;

import org.postgresql.jdbc.PgArray;

import java.net.HttpURLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static filip.test.StaticKeys.EXCEPTION_BADREQUEST;

/**
 * Created by Filip on 12/27/2016.
 */
public class Lecture {
    String guid;
    String title;
    String description;
    String unique_id;
    ArrayList<String> questionIds;
    List<HashMap<String,Object>> questions;

    Lecture(Map<String, Object> parameters, String guid) throws ExceptionHandler{
        checkIfCorrectEntry(parameters);
        String title = (String) parameters.get("title");
        String description = (String) parameters.get("description");
        String unique_id = (String) parameters.get("unique_id");
        this.guid = guid;
        this.title = title;
        this.description = description;
        this.unique_id = unique_id;
    }

    Lecture(Map<String, Object> parameters) throws ExceptionHandler{
        checkIfCorrectEntry(parameters);
        String title = (String) parameters.get("title");
        String description = (String) parameters.get("description");
        String guid = (String) parameters.get("guid");
        String unique_id = (String) parameters.get("unique_id");
        this.guid = guid;
        this.title = title;
        this.description = description;
        this.unique_id = unique_id;
    }

    Lecture(ResultSet rs) throws ExceptionHandler{
        try{
            List<HashMap<String,Object>> questionDictionaryList = Utilities.convertResultSetToList(rs);

            if (questionDictionaryList.size() == 0){
                throw new ExceptionHandler("Lecture: does not exist", HttpURLConnection.HTTP_INTERNAL_ERROR);
            }

            HashMap<String,Object> questionDictionary = questionDictionaryList.get(0);
            String title = (String) questionDictionary.get("title");
            String description = (String) questionDictionary.get("description");
            String unique_id = (String) questionDictionary.get("unique_id");

            PgArray answersPgArray = (PgArray)questionDictionary.get("questions");
            if (answersPgArray != null) {
                String[] answersStringArray = (String[]) answersPgArray.getArray();
                ArrayList<String> questions = new ArrayList<>();
                questions.addAll(Arrays.asList(answersStringArray));
                this.questionIds = questions;
            }

            this.guid = (String) questionDictionary.get("guid");
            this.title = title;
            this.description = description;
            this.unique_id = unique_id;
        }catch (ExceptionHandler e){
            throw e;
        }catch (SQLException e){
            throw new ExceptionHandler("Lecture: db error", HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    void updateQuestionsWithPulledValues(List<HashMap<String,Object>> questions){
        this.questions = questions;
        this.questionIds = null;
    }

    void checkIfCorrectEntry(Map<String, Object> parameters) throws ExceptionHandler {
        boolean badEntry = true;
        if (String.class.isInstance(parameters.get("title")) && String.class.isInstance(parameters.get("description"))){
            //all good
        }else {
            throw new ExceptionHandler("Lecture: bad params", HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }
}
