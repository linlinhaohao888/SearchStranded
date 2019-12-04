import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

class IndexTest {
    @Test
    void segmentTest() {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        ArrayList<String> EngWords = (ArrayList<String>) segmenter.sentenceProcess("Hello world.");
        ArrayList<String> CHWords = (ArrayList<String>) segmenter.sentenceProcess("你好世界。");

        System.out.println(EngWords);
        System.out.println(CHWords);
    }

    @Test
    void stopWordsTest() throws IOException {
        File file = new File("./res/stopWords.txt");
        List<String> words = FileUtils.readLines(file, Charset.defaultCharset());

        System.out.println(words);
    }

    @Test
    void indexTest() {
        Index index = new Index();
        try {
            index.createIndex(new File("C:\\Users\\linli\\Downloads\\test"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        ArrayList<String> words = new ArrayList<>(Arrays.asList("封杀", "算法", "practical"));

        ArrayList<String> paths = index.getFileRange(words);
        System.out.println(paths);
    }

    @Test
    void sortTest() {
        String test = "Hello";
        test = test.toLowerCase();
        System.out.println(test);
        ArrayList<String> result = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ArrayList<Integer> evaluation = new ArrayList<>(Arrays.asList(3,1,2));
        result.sort(Comparator.comparingInt(path -> {
            int index = result.indexOf(path);
            return evaluation.get(index);
        }));

        System.out.println(result);
    }

    @Test
    void utilTest() {
        File file = new File("C://");
        String parent = file.getParent();
        if (new File("./res/hi").exists()) {
            System.out.println("Exists");
        }
    }
}
