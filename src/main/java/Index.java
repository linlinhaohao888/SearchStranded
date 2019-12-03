import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.huaban.analysis.jieba.JiebaSegmenter;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class Index {
    List<String> stopWords;
    HashMap<String, HashMap<String, Integer>> wordFilesMap;
    HashMap<String, Integer> wordNumMap;
    JiebaSegmenter segmenter;
    Tika tika;

    public Index() {
        tika = new Tika();
        segmenter = new JiebaSegmenter();
        wordFilesMap = new HashMap<>();
        wordNumMap = new HashMap<>();
        File file = new File("./res/stopWords.txt");
        try {
            stopWords = FileUtils.readLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Acquire the collection of files that contains target words
     *
     * @param words target words
     * @return the collection (range) of files
     */
    ArrayList<String> fileRange(ArrayList<String> words) {
        //TODO
        return null;
    }

    /**
     * Indexing all the files within the assigned path
     *
     * @param path file path
     * @return whether the index is created successfully
     */
    boolean createIndex(String path) {
        // traverse the root path
        File targetFile = new File(path);
        if (targetFile.exists()) {
            File[] files = targetFile.listFiles();
            assert files != null;
            for (File file : files) {
                String filePath = file.getPath();
                if (file.isDirectory()) {
                    createIndex(filePath);
                } else {
                    try {
                        String content = tika.parseToString(file);
                        ArrayList<String> words = (ArrayList<String>) segmenter.sentenceProcess(content);
                        words.removeAll(stopWords);
                        for (String word : words) {
                            if (!wordFilesMap.containsKey(word)) {
                                HashMap<String, Integer> fileCntMap = new HashMap<>();
                                fileCntMap.put(filePath, 1);
                                wordFilesMap.put(word, fileCntMap);
                                wordNumMap.put(word, 1);
                            } else {
                                HashMap<String, Integer> fileCntMap = wordFilesMap.get(word);
                                if (!fileCntMap.containsKey(filePath)) {
                                    fileCntMap.put(filePath, 1);
                                } else {
                                    fileCntMap.put(filePath, fileCntMap.get(filePath) + 1);
                                }

                                wordNumMap.put(word, wordNumMap.get(word) + 1);
                            }
                        }
                    } catch (IOException | TikaException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return true;
    }

    void fileListener() {
        //TODO
    }
}
