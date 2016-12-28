package filip.test;

import org.postgresql.jdbc.PgArray;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import static filip.test.StaticKeys.*;

/**
 * Created by Filip on 12/27/2016.
 */
public class Question {
    String guid;
    String question;
    ArrayList<String> answers;
    int correctIndex;
    int duration;

    Question(Map<String, Object> parameters, String guid) throws Exception{
        checkIfCorrectEntry(parameters);
        String question = (String) parameters.get("question");
        int correctIndex = Integer.parseInt((String)parameters.get("correctindex"));
        int duration = Integer.parseInt((String)parameters.get("duration"));
        ArrayList<String> answers = (ArrayList<String>)parameters.get("answers");
        this.guid = guid;
        this.question = question;
        this.answers = answers;
        this.correctIndex = correctIndex;
        this.duration = duration;
    }

    Question(ResultSet rs) throws Exception{
        List<HashMap<String,Object>> questionDictionaryList = Utilities.convertResultSetToList(rs);

        if (questionDictionaryList.size() == 0){
            throw new Exception(EXCEPTION_BADREQUEST);
        }

        HashMap<String,Object> questionDictionary = questionDictionaryList.get(0);
        String question = (String) questionDictionary.get("question");
        int correctIndex = (Integer)questionDictionary.get("correctindex");
        int duration = (Integer) questionDictionary.get("duration");

        PgArray answersPgArray = (PgArray)questionDictionary.get("answers");
        String[] answersStringArray = (String[])answersPgArray.getArray();
        ArrayList<String> answers = new ArrayList<>();
        answers.addAll(Arrays.asList(answersStringArray));

        this.guid = (String) questionDictionary.get("guid");
        this.question = question;
        this.answers = answers;
        this.correctIndex = correctIndex;
        this.duration = duration;
    }

    void checkIfCorrectEntry(Map<String, Object> parameters) throws Exception {
        boolean badEntry = true;
        if (String.class.isInstance(parameters.get("question")) && String.class.isInstance(parameters.get("duration")) &&
                String.class.isInstance(parameters.get("correctindex")) && parameters.get("answers") != null){
            ArrayList<String> answers = (ArrayList<String>)parameters.get("answers");

            int correctIndex = Integer.parseInt((String) parameters.get("correctindex"));
            Integer.parseInt((String) parameters.get("duration")); //just a check if its parsable

            if (answers.size() > 0) {
                badEntry = false;
                for (int i = 0; i < answers.size(); i++) {
                    String answer = answers.get(i);
                    if (!String.class.isInstance(answer)) {
                        badEntry = true;
                        break;
                    }
                }
            }//if else, then badEntry already = true

            if (correctIndex > answers.size() - 1 || correctIndex < 0){
                badEntry = true;
            }
        }

        if (badEntry) {
            throw new Exception(EXCEPTION_BADREQUEST);
        }
    }
}
