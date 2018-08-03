package objects;

import org.jsoup.nodes.Element;

public class Answer {
    public String content;
    public String selector;

    public Answer(Element el) {
        selector = el.cssSelector();
        content = el.text();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Answer answer = (Answer) o;

        return content != null ? content.equals(answer.content) : answer.content == null;
    }

    @Override
    public int hashCode() {
        return content != null ? content.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "content='" + content + '\'' +
                '}';
    }
}
