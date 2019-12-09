import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
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
            while (true) {
                InputStreamReader is = new InputStreamReader(System.in);
                BufferedReader br = new BufferedReader(is);
                String word = br.readLine();
                if (word.equals("exit"))
                    break;
                ArrayList<String> paths = index.getFileRange(new ArrayList<>(Collections.singleton(word)));
                System.out.println(paths);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
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

    @Test
    void fileListenerTest() throws IOException, InterruptedException {
        String filePath = "C:\\Users\\linli\\Downloads";
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Paths.get(filePath).register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE);

        WatchKey key = watchService.take();
        List<WatchEvent<?>> watchEvents = key.pollEvents();
        for (WatchEvent<?> event : watchEvents) {
            Path fileName = (Path) event.context();
            System.out.println(fileName.toFile().getPath());
            if(StandardWatchEventKinds.ENTRY_MODIFY == event.kind() ||
                    StandardWatchEventKinds.ENTRY_DELETE == event.kind()){
                System.out.println("Hi");
            }
        }
        key.reset();
    }

    @Test
    void mapTest() {
        HashMap<String, HashMap<String, String>> test = new HashMap<>();
        HashMap<String, String> entry = new HashMap<>();
        entry.put("b", "c");
        test.put("a", entry);
        test.get("a").put("b", "d");
        System.out.println(test);
    }

    @Test
    void listenerTest() {
        Index index = new Index();
        try {
            index.createIndex(new File("C:\\Users\\linli\\Downloads\\test"));
            FileListener fileListener = new FileListener(index, "C:\\Users\\linli\\Downloads\\test");
            fileListener.start();
            fileListener.end();
            fileListener = new FileListener(index, "C:\\Users\\linli\\Downloads\\test");
            fileListener.start();
            fileListener.end();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
