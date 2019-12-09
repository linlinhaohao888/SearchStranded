import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileListener extends Thread {
    FileAlterationMonitor monitor;
    FileAlterationObserver observer;
    Index index;

    public FileListener(Index index, String path) {
        this.setDaemon(true);
        this.monitor = new FileAlterationMonitor(100);
        this.index = index;

        monitor.removeObserver(observer);

        observer = new FileAlterationObserver(path);
        observer.addListener(new IndexedFileAlterationListener());
        monitor.addObserver(observer);
    }

    public void end() {
        while (this.isAlive())
            this.interrupt();
    }

    @Override
    public void run() {
        try {
            monitor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            if (this.isInterrupted()) {
                try {
                    monitor.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    class IndexedFileAlterationListener implements FileAlterationListener {

        @Override
        public void onStart(FileAlterationObserver fileAlterationObserver) {

        }

        @Override
        public void onDirectoryCreate(File file) {

        }

        @Override
        public void onDirectoryChange(File file) {

        }

        @Override
        public void onDirectoryDelete(File file) {

        }

        @Override
        public void onFileCreate(File file) {
            index.createIndexes(Collections.singletonList(file).toArray(new File[0]));
            index.writeMapToFile(index.getCurIndex());
        }

        @Override
        public void onFileChange(File file) {
            index.updateIndexes(new ArrayList<>(Collections.singletonList(file.getPath())), false);
            index.writeMapToFile(index.getCurIndex());
        }

        @Override
        public void onFileDelete(File file) {
            index.updateIndexes(new ArrayList<>(Collections.singletonList(file.getPath())), false);
            index.writeMapToFile(index.getCurIndex());
        }

        @Override
        public void onStop(FileAlterationObserver fileAlterationObserver) {

        }
    }
}
