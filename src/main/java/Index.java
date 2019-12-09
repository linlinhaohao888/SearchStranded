import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class Index {
    // resource file paths
    private final static String prefix = "./res/";
    private final static String pathStopWords = "./res/stopWords.txt";
    private final static String fileModifiedPath = "./res/fileModified";
    private final static String indexNamesPath = "./res/indexNames";

    private List<String> stopWords;
    private JiebaSegmenter segmenter;
    private Tika tika;
    private ArrayList<String> skipPaths;
    private FileListener fileListener;

    // hash map of word -> (file path -> occurrences in the file)
    private HashMap<String, HashMap<String, Integer>> wordFilesMap;
    // hash map of indexed file path -> (contained file path -> last modified timestamp)
    private HashMap<String, HashMap<String, Long>> fileModifiedMap;
    // hash map of md5 indexed file path -> indexed file path
    private HashMap<String, String> indexFileNameMap;

    public String getCurIndex() {
        return curIndex;
    }

    private String curIndex;
    private String targetIndex;

    /**
     * Initialization
     */
    @SuppressWarnings("unchecked")
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
                byte[] content = new byte[fileInputStream.available()];
                int read = fileInputStream.read(content);
                fileModifiedMap = (HashMap<String, HashMap<String, Long>>) deserialize(content);
            }

            File indexNames = new File(indexNamesPath);
            if (indexNames.exists()) {
                FileInputStream fileInputStream = new FileInputStream(indexNamesPath);
                byte[] content = new byte[fileInputStream.available()];
                int read = fileInputStream.read(content);
                indexFileNameMap = (HashMap<String, String>) deserialize(content);
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
        if (!file.exists())
            throw new Exception("File does not exist");
        skipPaths = new ArrayList<>();
        targetIndex = file.getPath();

        File indexFile = hasIndex(file.getPath());
        if (indexFile != null) {
            wordFilesMap = loadIndex(indexFile);
            updateIndexes(new ArrayList<>(Collections.singletonList(curIndex)), true);
        } else {
            curIndex = file.getPath();
            if (!fileModifiedMap.containsKey(curIndex))
                fileModifiedMap.put(curIndex, new HashMap<>());
            if (file.isDirectory())
                skipPaths = loadExistingIndexToParent(file.getPath());
            createIndexes(Collections.singletonList(file).toArray(new File[0]));
            writeMapToFile(file.getPath());
        }

        if (fileListener != null)
            fileListener.end();
        fileListener = new FileListener(this, curIndex);
        fileListener.start();
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
     *
     * @param paths target paths
     */
    void updateIndexes(ArrayList<String> paths, boolean writeTag) {
        for (String filePath : paths) {
            File file = new File(filePath);
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                ArrayList<String> fileNames = new ArrayList<>();
                assert children != null;
                for (File child : children) {
                    fileNames.add(child.getPath());
                }
                updateIndexes(fileNames, false);
            } else if (!file.exists()) {
                // file has been deleted
                for (Map.Entry<String, HashMap<String, Integer>> fileCntMap : wordFilesMap.entrySet()) {
                    fileCntMap.getValue().remove(filePath);
                }
            } else if (fileModifiedMap.get(curIndex).containsKey(filePath) &&
                    fileModifiedMap.get(curIndex).get(filePath) != file.lastModified()) {
                // file has been modified
                for (Map.Entry<String, HashMap<String, Integer>> fileCntMap : wordFilesMap.entrySet()) {
                    fileCntMap.getValue().remove(filePath);
                }
                indexingFile(file);
                fileModifiedMap.get(curIndex).put(filePath, file.lastModified());
            } else if (!fileModifiedMap.get(curIndex).containsKey(filePath)) {
                // file is newly created
                indexingFile(file);
                fileModifiedMap.get(curIndex).put(filePath, file.lastModified());
            }
        }

        if (writeTag) {
            for (String path : paths)
                writeMapToFile(path);
        }
    }

    /**
     * load existing target index
     *
     * @param file target index
     * @return loaded index
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, HashMap<String, Integer>> loadIndex(File file) {
        // load the indexes into memory as hash map
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] content = new byte[fileInputStream.available()];
            int read = fileInputStream.read(content);
            HashMap<String, HashMap<String, Integer>> designatedWordFilesMap =
                    (HashMap<String, HashMap<String, Integer>>) deserialize(content);
            fileInputStream.close();

            return designatedWordFilesMap;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * forming an index file name from a path
     *
     * @param path file path
     * @return formed index file name
     */
    private String getIndexFileName(String path) {
        return prefix + DigestUtils.md5Hex(path);
    }

    /**
     * when indexing a folder, load existing child folders/files' indexes to avoid redundant indexing
     *
     * @param parentPath parent folder
     * @return all children that have been loaded
     */
    private ArrayList<String> loadExistingIndexToParent(String parentPath) {
        Collection<String> indexes = indexFileNameMap.values();
        ArrayList<String> paths = new ArrayList<>();
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
                                oldFileCntMap.put(fileCount.getKey(), oldFileCntMap.get(fileCount.getKey())
                                        + fileCount.getValue());
                            }
                        }
                    }
                }

                String oldIndex = curIndex;
                curIndex = indexPath;
                updateIndexes(new ArrayList<>(Collections.singleton(indexPath)), false);
                curIndex = oldIndex;
                String indexName = indexFile.getPath();
                boolean result = indexFile.delete();
                if (result) {
                    fileModifiedMap.get(parentPath).putAll(fileModifiedMap.get(indexPath));
                    fileModifiedMap.remove(indexPath);
                    indexFileNameMap.remove(indexName);
                }
            }
        }

        return paths;
    }

    /**
     * Indexing all the files within the assigned paths
     *
     * @param files assigned paths
     */
    void createIndexes(File[] files) {
        // traverse the root path
        if (files == null)
            return;
        for (File file : files) {
            if (skipPaths.contains(file.getPath()))
                continue;
            if (file.isDirectory()) {
                createIndexes(file.listFiles());
            } else {
                fileModifiedMap.get(curIndex).put(file.getPath(), file.lastModified());
                indexingFile(file);
            }
        }
    }

    /**
     * whether parent folders have been indexed
     *
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
    void writeMapToFile(String origin) {
        try {
            String path = getIndexFileName(origin);
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
        System.out.println("Indexing " + file.getPath());
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

    private void fileListener(String filePath) throws IOException, InterruptedException {
        File file = new File(filePath);
        if (!file.isDirectory())
            filePath = file.getParent();
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Paths.get(filePath).register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE);

        while (true) {
            WatchKey key = watchService.take();
            List<WatchEvent<?>> watchEvents = key.pollEvents();
            for (WatchEvent<?> event : watchEvents) {
                Path eventFile = (Path) event.context();
                String eventFileName = eventFile.toFile().getPath();
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    createIndexes(Collections.singletonList(eventFile.toFile()).toArray(new File[0]));
                }
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    updateIndexes(new ArrayList<>(Collections.singletonList(filePath)), true);
                }
                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    updateIndexes(new ArrayList<>(Collections.singletonList(filePath)), true);
                    break;
                }
            }
            key.reset();
        }
    }
}
