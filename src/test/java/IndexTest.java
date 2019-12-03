import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
}
