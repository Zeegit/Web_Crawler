package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Queue;

public class WebCrawler extends JFrame {
    //JTextArea textArea;
    JTextField urlTextField;
    //JLabel titleLabel;
    //JTable table;
    JTextField exportUrlTextField;
    //private DefaultTableModel model;
    ArrayList<Links> wcLinks;

    JTextField depthTextField;
    JCheckBox depthCheckBox;

    JTextField timelimitTextField;
    JCheckBox timelimitCheckBox;

    ExecutorService executor;
    long timeOnStart;
    JTextField workersField;

    JLabel labelParsedPages;
    JLabel labelElapsedTime;
    JToggleButton runButton;

    private Queue<Links> queue;

    int theadCount;
    Logger logger = Logger.getLogger(WebCrawler.class.getName());


    public WebCrawler() throws IOException {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 900);
        setTitle("WebCrawler");
        setLayout(new GridLayout(7, 1));

        add(createPanel1());
        add(createPanel2());
        add(createPanel3());
        add(createPanel4());
        add(createPanel5());
        add(createPanel6());
        add(createPanel7());

        setVisible(true);

        Handler fileHandler = new FileHandler("default.log");
        logger.addHandler(fileHandler);
        fileHandler.setFormatter(new SimpleFormatter());
    }

    JComponent createPanel1() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel urlLabel = new JLabel("Start URL:");

        urlTextField = new JTextField();
        urlTextField.setName("UrlTextField");
        urlTextField.setColumns(30);

        runButton = new JToggleButton("Run");
        runButton.setName("RunButton");
        runButton.addActionListener(new RunButtonActionListener());

        topPanel.add(urlLabel);
        topPanel.add(urlTextField);
        topPanel.add(runButton);


        return topPanel;
    }

    JComponent createPanel2() {
        JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label2 = new JLabel("Workers:");

        workersField = new JTextField();
        workersField.setColumns(30);
        workersField.setText("5");

        panel2.add(label2);
        panel2.add(workersField);

        return panel2;
    }

    JComponent createPanel3() {
        JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label3 = new JLabel("Maximum depth:");

        depthTextField = new JTextField();
        depthTextField.setName("DepthTextField");
        depthTextField.setColumns(30);
        depthTextField.setText("50");

        depthCheckBox = new JCheckBox("Enabled");
        depthCheckBox.setName("DepthCheckBox");
        depthCheckBox.setSelected(true);

        panel3.add(label3);
        panel3.add(depthTextField);
        panel3.add(depthCheckBox);

        return panel3;
    }

    JComponent createPanel4() {
        JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label31 = new JLabel("Time limit:");
        JLabel label32 = new JLabel("seconds:");

        timelimitTextField = new JTextField();
        timelimitTextField.setName("TimelimitTextField");
        timelimitTextField.setColumns(30);
        timelimitTextField.setText("120");

        timelimitCheckBox = new JCheckBox("Enabled");
        timelimitCheckBox.setSelected(true);

        panel4.add(label31);
        panel4.add(timelimitTextField);
        panel4.add(label32);
        panel4.add(timelimitCheckBox);

        return panel4;
    }

    JComponent createPanel5() {
        JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label51 = new JLabel("Elapsed time:");
        labelElapsedTime = new JLabel("0:00");

        panel5.add(label51);
        panel5.add(labelElapsedTime);

        return panel5;
    }

    JComponent createPanel6() {
        JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label61 = new JLabel("Parsed pages:");
        labelParsedPages = new JLabel("0");
        labelParsedPages.setName("ParsedLabel");

        panel6.add(label61);
        panel6.add(labelParsedPages);

        return panel6;
    }

    JComponent createPanel7() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel exportlLabel = new JLabel("Export:");

        exportUrlTextField = new JTextField();
        exportUrlTextField.setName("ExportUrlTextField");
        exportUrlTextField.setColumns(30);

        JButton exportButton = new JButton("Save");
        exportButton.setName("ExportButton");
        exportButton.addActionListener(new ExportButtonActionListener());

        bottomPanel.add(exportlLabel);
        bottomPanel.add(exportUrlTextField);
        bottomPanel.add(exportButton);

        return bottomPanel;
    }



    public synchronized void addTheadCount() {
        this.theadCount++;
    }

    public synchronized void subTheadCount() {
        this.theadCount--;
    }

    public synchronized void setTheadCount(int theadCount) {
        this.theadCount = theadCount;
    }

    public class ExportButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            String fileName = exportUrlTextField.getText();
            if (!fileName.equals("")) {
                File file = new File(fileName);
                file.delete();
                try (PrintWriter printWriter = new PrintWriter(file)) {
                    for (int i = 0; i < wcLinks.size(); i++) {
                        printWriter.println(wcLinks.get(i).linkPage);
                        printWriter.println(wcLinks.get(i).titlePage);

                    }
                } catch (IOException e) {
                    System.out.printf("An exception occurs %s", e.getMessage());
                }

            }
        }
    }

    public class RunButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            runButton.setText("Stop");
            wcLinks = new ArrayList<>();
            timeOnStart = System.currentTimeMillis();
            setTheadCount(0);
            queue = new ConcurrentLinkedQueue<>();

            executor = Executors.newFixedThreadPool(Integer.parseInt(workersField.getText()));
            // первая ссылка
            queue.add(new Links(urlTextField.getText(), "", 0));

            Checker task = new Checker();
            task.execute();

        }
    }


    public class Checker extends SwingWorker<String, String> {

        @Override
        protected String doInBackground() throws Exception {

            while (!overime()) {
                labelParsedPages.setText(String.valueOf(wcLinks.size()));
                labelElapsedTime.setText(time2Text(System.currentTimeMillis() - timeOnStart));
                // запускать потоки, если количество меньше заданного
                while (getTheadCount() < Integer.parseInt(workersField.getText()) && queue.size() > 0) {
                    executor.submit(new SeacherThread("", 0));
                }
                if (getTheadCount() == 0 && queue.size() == 0) {
                    labelParsedPages.setText(String.valueOf(wcLinks.size()));
                    labelElapsedTime.setText(time2Text(System.currentTimeMillis() - timeOnStart));
                    break;
                }
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            labelParsedPages.setText(String.valueOf(wcLinks.size()));
            labelElapsedTime.setText(time2Text(System.currentTimeMillis() - timeOnStart));
            runButton.setSelected(false);
            runButton.setText("Run");
        }

    }

    private synchronized int getTheadCount() {
        return theadCount;
    }

    class SeacherThread extends Thread {

        private String getURLThread(String url) {
            try {
                if (url != null && !url.equals("")) {
                    URL myUrl = new URL(url);
                    URLConnection myUrlCon = myUrl.openConnection();
                    myUrlCon.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
                    String connType = myUrlCon.getContentType();

                    if (connType != null && connType.contains("text/html")) {
                        InputStream inputStream = myUrl.openStream();
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        final StringBuilder stringBuilder = new StringBuilder();

                        String nextLine;
                        while ((nextLine = reader.readLine()) != null) {
                            stringBuilder.append(nextLine);
                            stringBuilder.append("\n");
                        }
                        return stringBuilder.toString();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        private String createLinkThread(String baseURL, String link0) {
            String link1 = link0
                    .replaceAll("href=", "")
                    .replaceAll("HREF=", "")
                    .replaceAll("\"", "")
                    .replaceAll("'", "");

            if (link1.contains("http") || link1.contains("https")) {
                return link1;
            } else if (!(link1.contains("http") || link1.contains("https")) && link1.contains(".")) {

                while (link1 != null && !link1.equals("") && link1.substring(0, 1).equals("/")) {
                    link1 = link1.substring(1);
                }
                if (link1.equals("")) {
                    return "";
                }
                String s = baseURL.substring(0, baseURL.indexOf(":"));
                link1 = s + "://" + link1;
            } else { // if (!link1.contains("/")) {
                while (link1 != null && !link1.equals("") && link1.substring(0, 1).equals("/")) {
                    link1 = link1.substring(1);
                }
                int pos = baseURL.lastIndexOf("/");
                link1 = baseURL.substring(0, pos) + "/" + link1;
            }
            return link1;

        }

        private String findTitleThread(String text) {
            Pattern doublePattern = Pattern.compile("<title>\\S.+<\\/title>");
            Matcher matcher = doublePattern.matcher(text);

            if (matcher.find()) {
                return matcher.group()
                        .replaceAll("<title>", "")
                        .replaceAll("</title>", "")
                        .trim();
            } else {
                return "";
            }
        }

        public SeacherThread(String url, int depth) {
        }

        @Override
        public void run() {
            // Уже обработали в другом потоке
            try {
                addTheadCount();
                Links q = queue.poll();

                if (q != null && !existLink(q.linkPage)) {
                    String url = q.linkPage;
                    int depth = q.depth;

                    // Новая страница
                    String text = getURLThread(url);

                    // textArea.append(text);
                    if (!text.equals("")) {
                        String title = findTitleThread(text);

                        // В результаты просмотра
                        if (depth > 0) {
                            addLink(new Links(url, title, depth));
                        }

                        if (depth + 1 <= getMaxDepth()) {
                            // ищем сылк
                            Pattern p = Pattern.compile("href=\"(.+)\"");
                            Matcher m = p.matcher(text);

                            m.reset();
                            while (m.find()) {
                                String link = createLinkThread(url, m.group());
                                if (link != null && !link.equals("") && !existLink(link)) {

                                    /*URL myUrl = new URL(link);
                                    URLConnection myUrlCon = myUrl.openConnection();
                                    myUrlCon.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
                                    String connType = myUrlCon.getContentType();

                                    if (connType != null && connType.contains("text/html")) */
                                    {
                                        //logger.info(m.group() + "\n" + link);
                                        queue.add(new Links(link, "", depth + 1));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                subTheadCount();
            }
        }
    }

    private boolean overime() {
        long timeWork = System.currentTimeMillis() - timeOnStart;
        long sec = timeWork / 1000L;
        return sec > getMaxWorkTime();
    }


    private long getMaxWorkTime() {
        return timelimitCheckBox.isSelected() ? Integer.parseInt(timelimitTextField.getText()) : Long.MAX_VALUE;
    }

    private synchronized int getMaxDepth() {
        return depthCheckBox.isSelected() ? Integer.parseInt(depthTextField.getText()) : Integer.MAX_VALUE;

    }

    private synchronized void addLink(Links link) {
        if (!existLink(link)) {
            wcLinks.add(link);
        }
    }

    private synchronized boolean existLink(Links link) {
        return wcLinks.contains(link);
    }

    private synchronized boolean existLink(String linkText) {
        Links link = new Links(linkText, "", 0);
        return wcLinks.contains(link);
    }

    String time2Text(long workTime) {
        long min = workTime / 60000L;
        long sec = (workTime - min * 60000L) / 1000L;
        long ms = workTime - min * 60000L - sec * 1000L;
        return min + ":" + sec;
        //min + " min. " + sec + " sec. " + ms + " ms.";
    }
}
