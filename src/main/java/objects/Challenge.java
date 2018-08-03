package objects;

import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

public class Challenge {
    public String link;
    public String title, shortLink;
    public List<Question> questions = new ArrayList<>();

    public Challenge(WebDriver baseUri) {
        link = baseUri.getCurrentUrl();
        title = baseUri.getTitle();
        String[] split = link.split("/");
        shortLink = split[split.length-1];
    }

    @Override
    public boolean equals(Object o) {
        if(shortLink == null){
            String[] split = link.split("/");
            shortLink = split[split.length-1];
        }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Challenge challenge = (Challenge) o;

        if (title != null ? !title.equals(challenge.title) : challenge.title != null) return false;
        return shortLink != null ? shortLink.equals(challenge.shortLink) : challenge.shortLink == null;
    }

    @Override
    public int hashCode() {
        if(shortLink == null){
            String[] split = link.split("/");
            shortLink = split[split.length-1];
        }

        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (shortLink != null ? shortLink.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Challenge{" +
                "link='" + link + '\'' +
                ", title='" + title + '\'' +
                ", shortLink='" + shortLink + '\'' +
                ", questions=" + questions +
                '}';
    }
}
