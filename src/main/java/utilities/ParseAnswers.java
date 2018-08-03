package utilities;

import objects.Challenge;
import objects.Question;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ParseAnswers {
    public static void main(String[] args) throws Exception{
//        printChallenge(getChallengeByShortLink("salesforce1_mobile_app_intro"));
        getChallengeByQuestion("mobile app")
                .forEach(a->printChallenge(a));
    }

    private static void printChallenge(Challenge challenge) {
        System.out.println(challenge.title+": "+challenge.shortLink);
        for (Question q : challenge.questions) {
            System.out.println("\t"+q.title);
            System.out.println("\t\t"+q.correctAnswer.content);
        }
    }

    public static Challenge getChallengeByShortLink(String shortLink) throws IOException {
        return getAllChallenges().stream()
                        .filter(a->a!=null&&a.shortLink!=null&&a.shortLink.equals(shortLink))
                        .collect(Collectors.toList()).get(0);
    }

    public static List<Challenge> getChallengeByQuestion(String question) throws IOException {
        return getAllChallenges().stream()
                .filter(a->a!=null&&a.toString().toUpperCase().contains(question.toUpperCase()))
                .collect(Collectors.toList());
    }


    public static List<Challenge> getAllChallenges() throws IOException {
        return FileOptions.getAllFiles("answers").stream().map(a -> {
            try {
                return FileOptions.getGson().fromJson(FileOptions.readFileIntoString(a.getAbsolutePath()),Challenge.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(a->a!=null&&a.title!=null).collect(Collectors.toList());
    }
}
