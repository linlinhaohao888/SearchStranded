import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EngineTest {

    @Test
    void stringIndexTest()
    {
        String originalString = "abcbcdadsta";
        String searchString = originalString;
        String regexString = "a";
        Pattern datePattern = Pattern.compile(regexString);
        Matcher dateMatcher = datePattern.matcher(searchString);
        int beEndIndex = 0;
        while(dateMatcher.find()) {
            String subString = dateMatcher.group();
            System.out.print("子串:"+subString+"  ");
            int subIndex = searchString.indexOf(subString);
            System.out.print("位置:"+(subIndex + beEndIndex)+"  ");
            int subLength = subString.length();
            System.out.println("长度:"+subLength);
            beEndIndex = subIndex + subLength + beEndIndex;
            searchString = originalString.substring(beEndIndex);
            dateMatcher = datePattern.matcher(searchString);
        }
        System.out.println("end");
    }

    @Test
    void searchTest()
    {
        Engine engine = new Engine();
        engine.setCurrentPath("./test/");
        ArrayList<Result> res = engine.search("结构");
        int a = 1;
    }

    @Test
    void charTest()
    {
        String content = "a，";
        content+="...";
        System.out.print(content);
    }

    @Test
    void engineTest()
    {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("abcgh");
        parts.add("wghsab");
        ArrayList<String> words = new ArrayList<>();
        words.add("ab");
        words.add("gh");
        Engine tool = new Engine();
        System.out.print(tool.mergeParts(parts, words));
    }
}
