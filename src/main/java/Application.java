import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import objects.Answer;
import objects.Challenge;
import objects.Question;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import utilities.FileOptions;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Application {
    public static void main(String[] args) throws InterruptedException, IOException {
// Optional, if not specified, WebDriver will search your path for chromedriver.
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/src/main/resources/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=/Users/jarndt/Library/Application Support/Google/Chrome/Profile 1");
//        options.addArguments("user-data-dir=/Users/jarndt/Library/Application Support/Google/Chrome/Default");
        options.addArguments("--start-maximized");
        loadCompletedTrails();
        List<String> trails = FileOptions.readFileIntoListString("links/trailheads.txt");
        new File(completed).mkdirs();
        FileOptions.getAllFiles(completed).forEach(a->{
            try {
                trails.remove(FileOptions.readFileIntoString(a.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        FileOptions.getAllFiles("links/no_questions").forEach(a->{
            try { //TODO change from complete link to just the end part of the link
                trails.remove(FileOptions.readFileIntoString(a.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //TODO concurrency
//        List<WebDriver> drivers = new ArrayList<>();
//        List<List<String>> linkList = new ArrayList<>();
//        int v = 2, vv = trails.size()/v;
//        FileOptions.runConcurrentProcess(IntStream.range(0,v).boxed().map(a->(Callable)()->{
//            drivers.add(new ChromeDriver(options));
//            linkList.add(trails.subList(a*vv,(a+1)*vv));
//            return null;
//        }).collect(Collectors.toList()));
//
//        FileOptions.runConcurrentProcess(IntStream.range(0,v).boxed().map(
//           a->(Callable)()->{
//               WebDriver driver = drivers.get(a);
//               login(driver);
//               for(String aa : linkList.get(a)){
//                   getAllAnswers(driver, aa);
//                   FileOptions.writeToFileOverWrite(completed+"/"+UUID.randomUUID().toString()+".txt",aa);
//               }
//               driver.quit();
//               return null;
//           }
//        ).collect(Collectors.toList()), v);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        login(driver,username,password)
        for(String a : trails){
            getAllAnswers(driver, a);
            FileOptions.writeToFileOverWrite(completed+"/"+a.replace("/","_")+".txt",a);
        }
        driver.quit();
    }

    public static List<Challenge> completedTrails = new ArrayList<>();

    public static String completed = "links/completed";

    private static void loadCompletedTrails() throws IOException {
        Gson gson = new Gson();
        completedTrails = FileOptions.getAllFiles("answers").stream().map(a -> {
            try {
                return gson.fromJson(FileOptions.readFileIntoString(a.getAbsolutePath()),Challenge.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(a->a!=null && a.title != null).collect(Collectors.toList());
    }

    public static void getAllAnswers(WebDriver driver, String url) throws IOException, InterruptedException {
        try{driver.navigate().to(url);}
        catch (Exception e){}
        new File("links/no_questions").mkdirs();
        Document page = Jsoup.parse(driver.getPageSource());
        if(page.select("#challenge .question").size() == 0) {
            if(page.select("#challenge > div").size() > 0)
                FileOptions.writeToFileOverWrite(completed+"/"+url.replace("/","_")+".txt",url);
            else
                FileOptions.writeToFileOverWrite("links/no_questions/"+url.replace("/","_"),url);
            return;
        }
        WebElement element = driver.findElement(By.id("challenge"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        Challenge challenge = parseQuestion(driver,driver.getPageSource());
        List<Challenge> v = completedTrails.stream().filter(a ->a.equals(challenge)).collect(Collectors.toList());
        if(!v.isEmpty())
            answerQuestions(v.get(0),driver);
        else
            answerQuestions(challenge,driver);
    }

    public static Random r = new Random();

    private static void answerQuestions(Challenge challenge, WebDriver driver) throws InterruptedException, IOException {
        for(Question q : challenge.questions){
            if(q.correctAnswer != null) {
                scrollElementVisible(driver, By.cssSelector(q.correctAnswer.selector));
                driver.findElement(By.cssSelector(q.correctAnswer.selector)).click();
                continue;
            }
            Answer a = q.answers.get(r.nextInt(q.answers.size()));
            q.correctAnswer = a;
            scrollElementVisible(driver, By.cssSelector(a.selector));
            driver.findElement(By.cssSelector(a.selector)).click();
        }
        while (true){
            try{
                driver.findElement(By.cssSelector("#challenge > section > div.check-quiz-actions > button")).click();
                //TODO wait for button to finish loading
                break;
            }catch (Exception e){}
        }
        Thread.sleep(5000);

        Elements errors = Jsoup.parse(driver.getPageSource()).select("#challenge .error");
        for(Element error : errors){
            Question wrong = new Question(error);
            for(Question q : challenge.questions)
                if(q.equals(wrong)){
                    q.wrongAnswers.add(q.correctAnswer);
                    q.answers.remove(q.correctAnswer);
                    q.correctAnswer = null;
                }
        }

        if(errors.size() > 0)
            answerQuestions(challenge,driver);
        else
            writeAnswers(challenge);
    }

    private static void writeAnswers(Challenge challenge) throws IOException {
        String dir = "answers";
        new File(dir).mkdirs();
        FileOptions.writeToFileOverWrite(dir+"/"+challenge.title.replace(" ","_").replace("/","_")+".json",
                new Gson().toJson(challenge));
    }

    public static void scrollElementVisible(WebDriver driver, By by){
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true); window.scrollBy(0,-300)", driver.findElement(by));
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    private static Challenge parseQuestion(WebDriver driver, String pageSource) {
        Document src = Jsoup.parse(pageSource);
        Challenge challenge = new Challenge(driver);
        Elements v = src.getElementById("challenge").getElementsByClass("question");
        for(Element e : v)
            challenge.questions.add(new Question(e));
        return challenge;
    }

    private static void login(WebDriver driver,String email, String password) throws IOException {//"1187827aB"
        driver.get("https://trailhead.salesforce.com/en/");
        loadCookies(driver);
        driver.navigate().refresh();
        ((JavascriptExecutor)driver).executeScript("var aTags = document.getElementsByTagName(\"Button\");\n" +
                "var searchText = \"Login\";\n" +
                "var found;\n" +
                "\n" +
                "for (var i = 0; i < aTags.length; i++) {\n" +
                "  if (aTags[i].textContent == searchText) {\n" +
                "    found = aTags[i];\n" +
                "    break;\n" +
                "  }\n" +
                "} return found.click();");
        ((JavascriptExecutor)driver).executeScript("var aTags = document.getElementsByTagName(\"a\");\n" +
                "var searchText = \"Log in with Google+\";\n" +
                "var found;\n" +
                "\n" +
                "for (var i = 0; i < aTags.length; i++) {\n" +
                "  if (aTags[i].textContent == searchText) {\n" +
                "    found = aTags[i];\n" +
                "    break;\n" +
                "  }\n" +
                "} return found.click();");
//        while(true) {
//            try {
//                WebElement e  = driver.findElement(By.cssSelector("#main-wrapper > header > div.container-fluid.desktop-header > div > a.btn.btn-primary"));//.click();
//                e.click();
//                break;
//            }catch (Exception e){
//                try {
//                    driver.findElement(By.cssSelector("#main-wrapper > header > div.slds-container_x-large.slds-container_center.slds-grid.slds-p-horizontal_medium.slds-show_large > div.slds-m-vertical_medium > a.slds-button.slds-button_link.slds-p-horizontal--small"))
//                            .click();
//                    break;
//                }catch (Exception ee) {
//                    try {
//                        if (driver.findElement(By.cssSelector("#main-wrapper > header > div.container-fluid.desktop-header > nav > div.desktop-header__display-name"))
//                                .getText().equals("Jonathan Arndt"))
//                            return;
//                    } catch (Exception eee) {
//                        try {
//                            if (driver.findElement(By.cssSelector("#main-wrapper > header > div.slds-container_x-large.slds-container_center.slds-grid.slds-p-horizontal_medium.slds-show_large > nav > div > div > div.Va\\28 m\\29.slds-m-right_x-small.slds-m-top_x_small.slds-show_inline-block > button"))
//                                    .getText().trim().equals("Jonathan Arndt"))
//                                return;
//                        } catch (Exception eeee) {
//                        }
//                    }
//                }
//            }
//        }
//        while(true) {
//            try {
//                driver.findElement(By.cssSelector("#signin_modal > div > div > div > div.span6.text-center.th-modal--panel.th-modal--panel__social > ul > li:nth-child(2) > a")).click();
//                break;
//            }catch (Exception e){}
//        }
        try {
            if (driver.findElement(By.cssSelector("#atomic > header > div.slds-container_x-large.slds-container_center.slds-grid.slds-p-horizontal_medium.slds-show_large > nav > div > div > div.Va\\28 m\\29.slds-m-right_x-small.slds-m-top_x_small.slds-show_inline-block > button"))
                    .getText().trim().equals("Jonathan Arndt"))
                return;
        } catch (Exception eeee) {
        }
        driver.findElement(By.cssSelector("#identifierId")).sendKeys(email);
        driver.findElement(By.cssSelector("#identifierNext > content > span")).click();
        WebElement e = null;
        while(true) {
            try {
                e = driver.findElement(By.cssSelector("#password > div.aCsJod.oJeWuf > div > div.Xb9hP > input"));//.sendKeys("w011432936W");                break;
                break;
            }catch (Exception ee){}
        }
        while(true) {
            try {
                e.sendKeys(password);
                e.sendKeys(Keys.ENTER);
                break;
            }catch (Exception ee){}
        }
        while (true){
            try {
                driver.findElement(By.cssSelector("#atomic > header > div.container-fluid.desktop-header > a > img"));
                break;
            }catch (Exception ee){}
        }
        saveCookies(driver);
    }

    private static final String cookiesFile = "Cookies.data";

    private static void saveCookies(WebDriver driver) {
        File file = new File(cookiesFile);
        try {
            // Delete old file if exists
            file.delete();
            file.createNewFile();
            FileWriter fileWrite = new FileWriter(file);
            BufferedWriter Bwrite = new BufferedWriter(fileWrite);
            // loop for getting the cookie information
            Bwrite.write(new Gson().toJson(driver.manage().getCookies()));
            Bwrite.flush();
            Bwrite.close();
            fileWrite.close();
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadCookies(WebDriver driver) throws IOException {
        File file = new File(cookiesFile);
        if(!file.exists())
            return;
        FileReader fileReader = new FileReader(file);
        BufferedReader Buffreader = new BufferedReader(fileReader);
        String strline;
        StringBuilder builder = new StringBuilder("");
        while((strline=Buffreader.readLine())!=null)
            builder.append(strline);
        Set<Cookie> cookies = new Gson().fromJson(builder.toString(),new TypeToken<Set<Cookie>>(){}.getType());
        for(Cookie c : cookies)
            driver.manage().addCookie(c);
    }
}
