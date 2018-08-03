package utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;

public class PageParser {
    public static void main(String[] args) throws InterruptedException, IOException {
        getLinks();
    }

    public static void getLinks() throws InterruptedException, IOException {
        new File("links").mkdirs();
        String base = "https://trailhead.salesforce.com/en/modules";
        Document doc = Jsoup.connect(base).get();
        for(Element e : doc.select(".units-list > li > a"))
            FileOptions.writeToFileAppend("links/trailheads.txt",base+e.attr("href").replace("/modules",""));
    }
}
