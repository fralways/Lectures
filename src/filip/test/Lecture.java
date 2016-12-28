package filip.test;

import org.postgresql.jdbc.PgArray;

import java.sql.ResultSet;
import java.util.*;

import static filip.test.StaticKeys.EXCEPTION_BADREQUEST;

/**
 * Created by Filip on 12/27/2016.
 */
public class Lecture {
    String guid;
    String title;
    String description;
    ArrayList<String> questionIds;
    List<HashMap<String,Object>> questions;

    Lecture(Map<String, Object> parameters, String guid) throws Exception{
        checkIfCorrectEntry(parameters);
        String title = (String) parameters.get("title");
        String description = (String) parameters.get("description");
        this.guid = guid;
        this.title = title;
        this.description = description;
    }

    Lecture(Map<String, Object> parameters) throws Exception{
        checkIfCorrectEntry(parameters);
        String title = (String) parameters.get("title");
        String description = (String) parameters.get("description");
        String guid = (String) parameters.get("guid");
        this.guid = guid;
        this.title = title;
        this.description = description;
    }

    Lecture(ResultSet rs) throws Exception{
        List<HashMap<String,Object>> questionDictionaryList = Utilities.convertResultSetToList(rs);

        if (questionDictionaryList.size() == 0){
            throw new Exception(EXCEPTION_BADREQUEST);
        }

        HashMap<String,Object> questionDictionary = questionDictionaryList.get(0);
        String title = (String) questionDictionary.get("title");
        String description = (String) questionDictionary.get("description");

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
    }

    void updateQuestionsWithPulledValues(List<HashMap<String,Object>> questions){
        this.questions = questions;
        this.questionIds = null;
    }

    void checkIfCorrectEntry(Map<String, Object> parameters) throws Exception {
        boolean badEntry = true;
        if (String.class.isInstance(parameters.get("title")) && String.class.isInstance(parameters.get("description"))){
            //all good
        }else {
            throw new Exception(EXCEPTION_BADREQUEST);
        }
    }

    void getQuestions() throws Exception {
        //ResultSet rs = dbHandler.getLectureQuestions(guid);
    }
}
