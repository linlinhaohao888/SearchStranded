import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class Index {
    // resource file paths
    private final static String prefix = ".\\res\\";
    private final static String pathStopWords = ".\\res\\stopWords.txt";
    private final static String fileModifiedPath = ".\\res\\fileModified";
    private final static String indexNamesPath = ".\\res\\indexNames";

    List<String> stopWords;
    JiebaSegmenter segmenter;
    Tika tika;
    ArrayList<String> skipPaths;

    // hash map of word -> (file path -> occurrences in the file)
    HashMap<String, HashMap<String, Integer>> wordFilesMap;
    HashMap<String, Long> fileModifiedMap;
    HashMap<String, String> indexFileNameMap;
    String curIndex;
    String targetIndex;

    /**
     * Initialization
     */
    public Index() {
        tika = new Tika();
        segmenter = new JiebaSegmenter();
        wordFilesMap = new HashMap<>();
        fileModifiedMap = new HashMap<>();
        indexFileNameMap = new HashMap<>();

        // initialize stop words
        try {
            File file = new File(pathStopWords);
            stopWords = FileUtils.readLines(file, Charset.defaultCharset());

            File fileModified = new File(fileModifiedPath);
            if (fileModified.exists()) {
                FileInputStream fileInputStream = new FileInputStream(fileModifiedPath);
                fileModifiedMap = (HashMap<String, Long>) deserialize(fileInputStream.readAllBytes());
            }

            File indexNames = new File(indexNamesPath);
            if (indexNames.exists()) {
                FileInputStream fileInputStream = new FileInputStream(indexNamesPath);
                indexFileNameMap = (HashMap<String, String>) deserialize(fileInputStream.readAllBytes());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Indexing all the files within the assigned path
     *
     * @param file assigned path
     */
    void createIndex(File file) throws Exception {
        if(!file.exists())
            throw new Exception("File does not exist");
        skipPaths = new ArrayList<>();
        targetIndex = file.getPath();

        File indexFile = hasIndex(file.getPath());
        if (indexFile != null) {
            wordFilesMap = loadIndex(indexFile);
            updateIndexes(new ArrayList<>(Collections.singletonList(curIndex)));
            writeMapToFile(getIndexFileName(curIndex), curIndex);
            return;
        }

        if (file.isDirectory())
            skipPaths = loadExistingIndexToParent(indexFileNameMap.values().toArray(new String[0]), file.getPath());
        updateIndexes(skipPaths);

        ArrayList<File> files = new ArrayList<>(Collections.singletonList(file));
        createIndexes(files.toArray(new File[0]));
        curIndex = file.getPath();
        writeMapToFile(getIndexFileName(file.getPath()), file.getPath());
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

                    if (!filePath.startsWith(targetIndex))
                        continue;

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
     * update indexes of the files within the paths
     * @param paths target paths
     */
    private void updateIndexes(ArrayList<String> paths) {
        for (String filePath : paths) {
            File file = new File(filePath);
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                ArrayList<String> fileNames = new ArrayList<>();
                assert children != null;
                for (File child : children) {
                    fileNames.add(child.getPath());
                }
                updateIndexes(fileNames);
            } else if (fileModifiedMap.get(filePath) != file.lastModified()) {
                for (Map.Entry<String, HashMap<String, Integer>> fileCntMap : wordFilesMap.entrySet()) {
                    fileCntMap.getValue().remove(filePath);
                }
                indexingFile(file);
                fileModifiedMap.put(filePath, file.lastModified());
            }
        }
    }

    /**
     * load existing target index
     * @param file target index
     * @return loaded index
     */
    private HashMap<String, HashMap<String, Integer>> loadIndex(File file) {
        // load the indexes into memory as hash map
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            HashMap<String, HashMap<String, Integer>> designatedWordFilesMap =
                    (HashMap<String, HashMap<String, Integer>>) deserialize(fileInputStream.readAllBytes());
            fileInputStream.close();

            return designatedWordFilesMap;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * forming an index file name from a path
     * @param path file path
     * @return formed index file name
     */
    private String getIndexFileName(String path) {
        return prefix + DigestUtils.md5Hex(path);
    }

    /**
     * when indexing a folder, load existing child folders/files' indexes to avoid redundant indexing
     * @param indexes existing indexes
     * @param parentPath parent folder
     * @return all children that have been loaded
     */
    private ArrayList<String> loadExistingIndexToParent(String[] indexes, String parentPath) {
        ArrayList<String> paths = new ArrayList<>();
        if (indexes == null)
            return paths;
        for (String indexPath : indexes) {
            File index = new File(indexPath);
            File indexFile = new File(getIndexFileName(indexPath));
            if (indexPath.startsWith(parentPath)) {
                paths.add(index.getPath());
                HashMap<String, HashMap<String, Integer>> wordIndex = loadIndex(indexFile);
                if (wordIndex == null)
                    return paths;
                for (Map.Entry<String, HashMap<String, Integer>> entry : wordIndex.entrySet()) {
                    String word = entry.getKey();
                    HashMap<String, Integer> fileCntMap = entry.getValue();
                    if (!wordFilesMap.containsKey(word)) {
                        wordFilesMap.put(word, fileCntMap);
                    } else {
                        for (Map.Entry<String, Integer> fileCount : fileCntMap.entrySet()) {
                            HashMap<String, Integer> oldFileCntMap = wordFilesMap.get(word);
                            if (!oldFileCntMap.containsKey(fileCount.getKey())) {
                                oldFileCntMap.put(fileCount.getKey(), fileCount.getValue());
                            } else {
                                oldFileCntMap.put(fileCount.getKey(), oldFileCntMap.get(fileCount.getKey()) + fileCount.getValue());
                            }
                        }
                    }
                }

                String indexName = indexFile.getPath();
                boolean result = indexFile.delete();
                if (result)
                    indexFileNameMap.remove(indexName);
            }
        }

        return paths;
    }

    /**
     * Indexing all the files within the assigned paths
     *
     * @param files assigned paths
     */
    private void createIndexes(File[] files) {
        // traverse the root path
        if (files == null)
            return;
        for (File file : files) {
            if (skipPaths.contains(file.getPath()))
                continue;
            if (file.isDirectory()) {
                System.out.println("Indexing " + file.getPath());
                createIndexes(file.listFiles());
            } else {
                System.out.println("Indexing " + file.getPath());
                fileModifiedMap.put(file.getPath(), file.lastModified());
                indexingFile(file);
            }
        }
    }

    /**
     * whether parent folders have been indexed
     * @param path current index
     * @return parent folders that have been indexed, null if no such folder exists
     */
    private File hasIndex(String path) {
        while (path != null) {
            File file = new File(path);
            File indexFile = new File(getIndexFileName(path));
            if (indexFile.exists()) {
                curIndex = path;
                return indexFile;
            }

            path = file.getParent();
        }

        return null;
    }

    /**
     * serialize the hash maps (i.e., indexes) to files
     */
    private void writeMapToFile(String path, String origin) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            fileOutputStream.write(serialize(wordFilesMap));
            fileOutputStream.close();

            indexFileNameMap.put(path, origin);
            fileOutputStream = new FileOutputStream(indexNamesPath);
            fileOutputStream.write(serialize(indexFileNameMap));
            fileOutputStream.close();

            fileOutputStream = new FileOutputStream(fileModifiedPath);
            fileOutputStream.write(serialize(fileModifiedMap));
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * indexing a single file
     *
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
                } else {
                    HashMap<String, Integer> fileCntMap = wordFilesMap.get(word);
                    if (!fileCntMap.containsKey(filePath)) {
                        fileCntMap.put(filePath, 1);
                    } else {
                        fileCntMap.put(filePath, fileCntMap.get(filePath) + 1);
                    }
                }
            }
        } catch (IOException | TikaException ignored) {
        }
    }

    /**
     * serialize an object to bytes
     *
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
     *
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
