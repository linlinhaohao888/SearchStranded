import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;

public class GUI extends JFrame {
    private File file;
    private JTextField textArea = new JTextField();
    private JLabel label = new JLabel("Please select a file for search first");
    private JTable table = new JTable();
    private JButton button = new JButton("Search");
    private Engine engine = new Engine();
    private PipedInputStream pipedIS = new PipedInputStream();

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.setVisible(true);
    }

    private GUI() {
        super();
        initUI();
    }

    private void initUI() {
        textArea.setEnabled(false);
        button.setEnabled(false);
        JPanel upPanel = new JPanel();
        JPanel downPanel = new JPanel();

        JScrollPane scrollPaneTable = new JScrollPane();
        scrollPaneTable.setViewportView(table);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textArea, scrollPaneTable);
        splitPane.setDividerLocation(30);
        splitPane.setDividerSize(1);

        upPanel.setLayout(new BorderLayout());
        downPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        upPanel.add(splitPane);
        downPanel.add(label);
        downPanel.add(button);

        add(upPanel, BorderLayout.CENTER);
        add(downPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem menuItem = new JMenuItem("Open");
        menuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fileChooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                label.setText("Indexing " + file.getPath());
                menu.setEnabled(false);
                textArea.setEnabled(false);
                button.setEnabled(false);

                Thread thread = new Thread(() -> {
                    engine.setCurrentPath(file.getPath());
                    textArea.setEnabled(true);
                    button.setEnabled(true);
                    menu.setEnabled(true);
                    label.setText("Current path: " + file.getPath());
                });
                thread.setDaemon(true);
                thread.start();
            }
        });
        menu.add(menuItem);

        button.addActionListener(e -> {
            String sentence = textArea.getText();
            ArrayList<Result> results = engine.search(sentence);
            onAcquireResults(results);
        });
        button.setMnemonic(KeyEvent.VK_ENTER);

        setTitle("SearchStranded");
        setSize(800, 600);
        setLocation(200, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void onAcquireResults(ArrayList<Result> results) {
        Object[][] blankData = {};
        ((DefaultTableModel) table.getModel()).setDataVector(blankData, blankData);
        String[] columnNames = new String[]{"Path", "Content"};
        String[][] data = new String[results.size()][2];
        for(int i = 0; i < results.size(); i++) {
            data[i][0] = results.get(i).getFilename();
            data[i][1] = results.get(i).getContent();
        }

        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.setDataVector(data, columnNames);
        table.setModel(tableModel);
    }
}
