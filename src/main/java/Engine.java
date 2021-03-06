import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;


public class Engine {
    private final static String pathStopWords = "./res/stopWords.txt";

    private String currentPath;
    private JiebaSegmenter segmenter;
    private List<String> stopWords;
    private Index index;
    private Tika tika;
    private String punctuations;

    /**
     * initialization
     */

    public Engine() {
        segmenter = new JiebaSegmenter();
        index = new Index();
        tika = new Tika();
        punctuations = "!?,.():;'\"！？，。：；“”‘’《》<>";
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
        queryWords.removeAll(Collections.singleton(" "));
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

                        int startPos = pos - 1, endPos = pos + word.length();
                        for (; startPos > 0; startPos--) {
                            if (punctuations.indexOf(content.charAt(startPos)) != -1) {
                                startPos++;
                                break;
                            }
                        }
                        for (; endPos < content.length(); endPos++) {
                            if (punctuations.indexOf(content.charAt(startPos)) != -1) {
                                endPos--;
                                break;
                            }
                        }

//                        String part = "..." + content.substring(Math.max(0, pos - 5), Math.min(content.length(), pos + word.length() + 5)) + "...";
//                        part = part.replace("\n", " ");
                        String part = content.substring(startPos, endPos);
                        parts.add(part);
                    }
                }
//                if (parts.isEmpty())
//                {
//                    parts.add(content.substring(0, Math.min(content.length(), 10)));
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String toShow = mergeParts(parts, queryWords);

            res.add(new Result(path, toShow));

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

    static public String mergeParts(ArrayList<String> parts, ArrayList<String> words) {
        Set<String> non_repeat = new HashSet<>(parts);
        StringBuilder res = new StringBuilder("<html>");
        for (String sentence : non_repeat) {
            res.append("...");
            ArrayList<ArrayList<Integer>> pos = new ArrayList<>();
            for (String w : words) {
                int start_pos = sentence.indexOf(w);
                if (start_pos != -1) {
                    ArrayList<Integer> division = new ArrayList<>();
                    division.add(start_pos);
                    division.add(start_pos + w.length());
                    pos.add(division);
                }
            }
            pos.sort(Comparator.comparing(o -> o.get(0)));
            int start = 0, end = sentence.length();
            StringBuilder redlined = new StringBuilder();
            for (ArrayList<Integer> div : pos) {
                redlined.append(sentence, start, div.get(0)).append("<font color=\"#FF0000\">").append(sentence, div.get(0), div.get(1)).append("</font>");
                start = div.get(1);
            }
            redlined.append(sentence, start, end);
            res.append(redlined);
        }
        res.append("...</html>");
        return res.toString();
    }


}
