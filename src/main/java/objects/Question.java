package objects;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class Question {
//    public int number;
    public String title;
    public List<Answer> answers;
    public List<Answer> wrongAnswers = new ArrayList<>();
    public Answer correctAnswer;
    public String selector;

    public Question(Element e) {
        this.selector = e.cssSelector();
        Element titleElement = e.getElementsByClass("question_title").get(0);
        this.title = titleElement.text();
        answers = new ArrayList<>();
        for(Element el : e.getElementsByClass("question_answer"))
            answers.add(new Answer(el));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Question question = (Question) o;

        return title != null ? title.equals(question.title) : question.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Question{" +
                "title='" + title + '\'' +
                ", correctAnswer=" + correctAnswer +
                '}';
    }
}
