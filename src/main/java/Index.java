import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class Index {
    // resource file paths
    private final static String pathWordFiles = "./res/wordFilesMap.txt";
    private final static String pathWordNum = "./res/wordNumMap.txt";
    private final static String pathStopWords = "./res/stopWords.txt";

    List<String> stopWords;
    JiebaSegmenter segmenter;
    Tika tika;

    // hash map of word -> (file path -> occurrences in the file)
    HashMap<String, HashMap<String, Integer>> wordFilesMap;
    // hash map of word -> total occurrences
    HashMap<String, Integer> wordNumMap;

    /**
     * Initialization
     * @param willIndex set to true if you want to index again despite existing indexes
     */
    public Index(boolean willIndex) {
        tika = new Tika();
        segmenter = new JiebaSegmenter();
        wordFilesMap = new HashMap<>();
        wordNumMap = new HashMap<>();

        // initialize stop words
        try {
            File file = new File(pathStopWords);
            stopWords = FileUtils.readLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // index the whole disk if no index exists or you choose to do so
        File fileWordFiles = new File(pathWordFiles);
        File fileWordNum = new File(pathWordNum);
        if (!fileWordFiles.exists() || !fileWordNum.exists() || willIndex) {
            File[] files = File.listRoots();
//            File[] files = new File[1];
//            files[0] = new File("C:\\Users\\linli\\Downloads\\test");
            createIndex(files);
            writeMapToFile();
        }

        // load the indexes into memory as hash map
        try {
            FileInputStream fileInputStream = new FileInputStream(fileWordFiles);
            wordFilesMap = (HashMap<String, HashMap<String, Integer>>) deserialize(fileInputStream.readAllBytes());
            fileInputStream.close();

            fileInputStream = new FileInputStream(fileWordNum);
            wordNumMap = (HashMap<String, Integer>) deserialize(fileInputStream.readAllBytes());
            fileInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Acquire the collection of files that contains target words
     *
     * @param words target words
     * @return the collection (range) of files
     */
    ArrayList<String> getFileRange(ArrayList<String> words) {
        ArrayList<String> result = new ArrayList<>();
        // evaluation of the corresponding file
        // score of a file = sum of the occurrence times of all input words in the file
        ArrayList<Integer> evaluation = new ArrayList<>();

        for (String word : words) {
            word = word.toLowerCase();
            if (wordFilesMap.containsKey(word)) {
                HashMap<String, Integer> fileCounts = wordFilesMap.get(word);
                for (Map.Entry<String, Integer> fileCount : fileCounts.entrySet()) {
                    String filePath = fileCount.getKey();
                    Integer cnt = fileCount.getValue();
                    if (!result.contains(filePath)) {
                        result.add(filePath);
                        evaluation.add(cnt);
                    } else {
                        int index = result.indexOf(filePath);
                        evaluation.set(index, evaluation.get(index) + cnt);
                    }
                }
            }
        }

        // sort result according to evaluation
        result.sort(Comparator.comparingInt(path -> {
            int index = result.indexOf(path);
            return evaluation.get(index);
        }));
        return result;
    }

    /**
     * Indexing all the files within the assigned paths
     *
     * @param files assigned paths
     */
    void createIndex(File[] files) {
        // traverse the root path
        if (files == null)
            return;
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Indexing " + file.getPath());
                createIndex(file.listFiles());
            } else {
                indexingFile(file);
            }
        }
    }

    /**
     * serialize the hash maps (i.e., indexes) to files
     */
    private void writeMapToFile() {
        try {
            File fileWordFiles = new File(pathWordFiles);
            File fileWordNum = new File(pathWordNum);

            FileOutputStream fileOutputStream = new FileOutputStream(fileWordFiles);
            fileOutputStream.write(serialize(wordFilesMap));
            fileOutputStream.close();

            fileOutputStream = new FileOutputStream(fileWordNum);
            fileOutputStream.write(serialize(wordNumMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * indexing a single file
     * @param file the file to be indexed
     */
    private void indexingFile(File file) {
        String filePath = file.getPath();
        try {
            String content = tika.parseToString(file) + " " + file.getName();
            content = content.toLowerCase();
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
        } catch (IOException | TikaException ignored) {
        }
    }

    /**
     * serialize an object to bytes
     * @param obj the object to be serialized
     * @return the result bytes
     * @throws IOException IO exception
     */
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(obj);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        objectOutputStream.close();
        byteArrayOutputStream.close();
        return bytes;
    }

    /**
     * deserialize an object to bytes
     * @param str the bytes to be serialized
     * @return the result object
     * @throws IOException IO exception
     */
    private Object deserialize(byte[] str) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Object object = objectInputStream.readObject();
        objectInputStream.close();
        byteArrayInputStream.close();
        return object;
    }

    void fileListener() {
        //TODO
    }
}
