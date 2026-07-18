import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.*;
import java.awt.*;
/**
 * 提供一组实用工具静态方法和两个静态内部类，用于文件加载、
 * 序列化或反序列化HashMap、数据计算和图形绘制等任务。
 */
public class ToolBoxAPI {
     /**
     * 从文件中加载数据。
     * @param path 文件路径
     * @param isLink 是否处理为链接数据
     * @return 加载的数据
     */
    public static Vector<String> loadFile(String path, boolean isLink) {
        Vector<String> lines = new Vector<>();
        try (FileReader in = new FileReader(path); LineNumberReader reader = new LineNumberReader(in)) {
            String str;
            int count = 0;
            while ((str = reader.readLine()) != null) {
                String[] parts = str.split(" ");
                if (isLink && parts.length == 4) {
                    lines.add(String.join(" ", parts));
                } else if (!isLink && parts.length == 1) {
                    lines.add(str);
                } else {
                    System.err.println("Invalid line: " + str);
                }
                count++;
            }
            System.out.println("共读到" + count + "条数据");
        } catch (FileNotFoundException e) {
            System.err.println("文件未找到: " + path);
        } catch (IOException e) {
            System.err.println("读取文件时发生错误: " + path);
        }
        return lines;
    }

    /**
     * 将毫秒级的时间差格式化为"小时 分钟 秒 毫秒"的形式。
     * @param timeStart 起始时间
     * @param timeOver 结束时间
     * @return 格式化后的时间字符串
     */
    public static void printFormattedTime(long timeStart, long timeOver) {
        long time = timeOver - timeStart;
        long hour = time / 3600000;
        long minute = (time % 3600000) / 60000;
        long second = (time % 60000) / 1000;
        long millisecond = time % 1000;
        System.out.println("运行时间: " + hour + "小时" + minute + "分钟" + second + "秒" + millisecond + "毫秒");
    }

    /**
     * HashMap相关服务，包括序列化、反序列化和获取HashMap实例。
     */
    public static class HashMapService {
        private static final String PATH = "data/adjacency-map.ser";
        private HashMap<String, Vector<String>> map = new HashMap<>();

        public void createOrRead(Vector<String> keys, Vector<String> values, boolean save) {
            File file = new File(PATH);
            if (file.exists()) {
                System.out.println("序列化文件已存在,将直接读取");
                map = deserialize();
            } else {
                createMap(keys, values);
                System.out.println("创建完成");
                if (save) {
                    serialize();
                }
            }
        }

        private void createMap(Vector<String> keys, Vector<String> values) {
            for (String key : keys) {
                Vector<String> toNodes = new Vector<>();
                for (String value : values) {
                    String[] parts = value.split(" ");
                    if (parts[0].equals(key)) {
                        toNodes.add(value);
                    }
                }
                map.put(key, toNodes);
            }
        }

        public void serialize() {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(PATH))) {
                out.writeObject(map);
                System.out.println("序列化完成,储存在" + PATH);
            } catch (IOException e) {
                System.err.println("序列化失败:" + e.getMessage());
            }
        }

        public HashMap<String, Vector<String>> deserialize() {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(PATH))) {
                Object obj = in.readObject();
                if (obj instanceof HashMap) {
                    map = (HashMap<String, Vector<String>>) obj;
                    System.out.println("反序列化完成,已载入类实例");
                } else {
                    System.err.println("反序列化失败: 读取的对象不是HashMap实例");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("反序列化失败: " + e.getMessage());
            }
            return map;
        }

        public HashMap<String, Vector<String>> getMap() {
            if (map.isEmpty()) {
                map = deserialize();
            }
            if (map == null) {
                System.err.println("返回失败");
            } else {
                System.out.println("已返回");
            }
            return map;
        }
    }

    /**
     * 获取HashMap中出度前十的节点。
     * @param map 包含节点链接信息的HashMap
     * @return 出度前十的节点及其出度
     */
    public static HashMap<String, Integer> getTop10OutDegreeNodes(HashMap<String, Vector<String>> map) {
        HashMap<String, Integer> outDegree = new HashMap<>();
        map.forEach((key, links) -> outDegree.put(key, links.size()));
        return getSortedTop10(outDegree);
    }

    /**
     * 获取HashMap中正向出度前十的节点。
     * @param map 包含节点链接信息的HashMap
     * @return 正向出度前十的节点及其出度
     */
    public static HashMap<String, Integer> getTop10PositiveOutDegreeNodes(HashMap<String, Vector<String>> map) {
        HashMap<String, Integer> positiveOutDegree = new HashMap<>();
        map.forEach((key, links) -> {
            int positiveCount = (int) links.stream()
                    .map(toNode -> toNode.split(" ")[2])
                    .filter(category -> category.equals("1"))
                    .count();
            positiveOutDegree.put(key, positiveCount);
        });
        return getSortedTop10(positiveOutDegree);
    }

    /**
    * 打印(正)出度最高的十个点及其(正)出度。
    */
    public static void printOutDegreeDetails(HashMap<String, Vector<String>> map, boolean isPositive) {
        if (isPositive) {
            System.out.println("正出度最高的十个点:");
            ToolBoxAPI.getTop10PositiveOutDegreeNodes(map).forEach((k, v) -> System.out.println("节点: "+ k + "   出度: " + v));
        } else {
            System.out.println("出度最高的十个点:");
            ToolBoxAPI.getTop10OutDegreeNodes(map).forEach((k, v) -> System.out.println("节点: "+ k + "   正出度: " + v));
        }}
    /**
     * 获取排序后的前十个元素。
     * @param degreeMap 包含元素及其度数的HashMap
     * @return 排序后的前十个元素及其度数
     */
    public static HashMap<String, Integer> getSortedTop10(HashMap<String, Integer> degreeMap) {
        return degreeMap.entrySet().stream()
                .sorted((o1, o2) -> o2.getValue() - o1.getValue())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 获取初始节点的激活状态。
     * @param InitialNodes 初始节点队列
     * @param allNodes 所有节点
     * @return 初始节点的激活状态
     */
    public static HashMap<String, Integer> getInitialStatus(Queue<String> InitialNodes, Vector<String> allNodes) {
        HashMap<String, Integer> initialStatus = new HashMap<>();
        for (String node : allNodes) {
            initialStatus.put(node, InitialNodes.contains(node) ? 1 : 0);
        }
        return initialStatus;
    }

    /**
     * 绘制折线图。
     * @param xData x轴数据
     * @param yData y轴数据
     */
    public static void plotGraph(int[] xData, int[] yData) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Graph Plot");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new GraphPanel(xData, yData));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * 内部类，用于绘制折线图。
     */
    private static class GraphPanel extends JPanel {
        private final int[] xData;
        private final int[] yData;

        public GraphPanel(int[] xData, int[] yData) {
            this.xData = downsample(xData);
            this.yData = downsample(yData);
            setPreferredSize(new Dimension(800, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            // 绘制坐标轴
            g2.setColor(Color.BLACK);
            g2.drawLine(50, height - 50, width - 50, height - 50); // x 轴
            g2.drawLine(50, 50, 50, height - 50); // y 轴

            // 获取最大和最小值
            int maxDataValue = Arrays.stream(yData).max().orElse(1);
            int minDataValue = Arrays.stream(yData).min().orElse(0);

            // 计算刻度间隔
            int xInterval = (width - 100) / (xData.length - 1);
            int yInterval = (height - 100) / (maxDataValue - minDataValue);

            // 绘制数据点和折线
            g2.setColor(Color.BLUE);
            for (int i = 0; i < xData.length - 1; i++) {
                int x1 = 50 + i * xInterval;
                int y1 = height - 50 - (yData[i] - minDataValue) * yInterval;
                int x2 = 50 + (i + 1) * xInterval;
                int y2 = height - 50 - (yData[i + 1] - minDataValue) * yInterval;
                g2.fillOval(x1 - 2, y1 - 2, 4, 4); // 绘制数据点
                g2.drawLine(x1, y1, x2, y2); // 绘制折线
            }
        }

        private int[] downsample(int[] data) {
            if (data.length <= 500) {
                return data;
            }
            int[] downsampled = new int[500];
            double interval = (double) data.length / 500;
            for (int i = 0; i < 500; i++) {
                downsampled[i] = data[(int) (i * interval)];
            }
            return downsampled;
        }
    }
    public static void plotGraph(int[] yData){
        int[] xData = new int[yData.length];
        for (int i = 0; i < yData.length; i++) {
            xData[i] = i;
        }
        plotGraph(xData, yData);
    }
}
