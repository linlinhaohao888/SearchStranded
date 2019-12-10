import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class Engine {
    private final static String pathStopWords = "./res/stopWords.txt";

    private String currentPath;
    private JiebaSegmenter segmenter;
    private List<String> stopWords;
    private Index index;
    private Tika tika;

    /**
     * initialization
     */

    public Engine() {
        segmenter = new JiebaSegmenter();
        index = new Index();
        tika = new Tika();
        try {
            File file = new File(pathStopWords);
            stopWords = FileUtils.readLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param query query sentence
     * @return list of file and sentence containing key words
     */

    public ArrayList<Result> search(String query) {
        ArrayList<String> queryWords = (ArrayList<String>) segmenter.sentenceProcess(query);
        queryWords.removeAll(stopWords);
        ArrayList<String> filePaths = index.getFileRange(queryWords);
        ArrayList<Result> res = new ArrayList<>();
        for (String path : filePaths) {
            File file = new File(path);
            ArrayList<String> parts = new ArrayList<>();
            try {
                String content = tika.parseToString(file);
                content = content.toLowerCase();
                for (String word : queryWords) {
                    String lowerVer = word.toLowerCase();
                    int pos = content.indexOf(lowerVer);
                    if (pos >= 0) {
                        String part = "..." + content.substring(Math.max(0, pos - 5), Math.min(content.length(), pos + word.length() + 5)) + "...";
                        part = part.replace("\n", " ");
                        parts.add(part);
                    }
                }
                if (parts.isEmpty()) {
                    parts.add(content.substring(0, Math.min(content.length(), 10)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            res.add(new Result(path, parts));

        }

        return res;
    }

    /**
     * @param path current file path
     */

    public void setCurrentPath(String path) {
        if (path.equals(currentPath))
            return;
        try {
            File file = new File(path);
            index.createIndex(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
