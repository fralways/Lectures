package filip.test.socket;

import filip.test.ListenerQuestion;
import filip.test.Question;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Filip on 4/1/2017.
 */

class SocketQuestion {
    private ListenerQuestion listenerQuestion;
    private Question question; //if lecturer sends his question
    private Date date; //if lecturer sends his question

    public Date getDate() {
        return date;
    }

    SocketQuestion(ListenerQuestion question){
        listenerQuestion = question;
        date = question.getDate();
    }

    SocketQuestion(Question question){
        this.question = question;
        date = new Date();
    }

    Object getQuestion() {
        HashMap<String, Object> hashQuestion = new HashMap<>();

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
        String strDate = sdfDate.format(date);
        hashQuestion.put("date", strDate);

        if (question != null) {
            hashQuestion.put("question", question);
        } else {
            hashQuestion.put("question", listenerQuestion);
        }

        return hashQuestion;
    }
}
