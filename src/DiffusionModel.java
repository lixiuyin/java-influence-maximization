import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DiffusionModel 类用于构建扩散模型，计算指定初始激活节点的平均正向影响力。
 */
public class DiffusionModel {
    private final Queue<String> initialNodes;  // 初始激活节点
    private final HashMap<String, Integer> initialStatus;   // 初始激活状态
    private final HashMap<String, Vector<String>> map;  // 节点链接
    private int[] averagePositiveInfluenceArray;  // 前n次模拟的平均正向影响力数组
    private static int calculationCount = 0;    // 平均正向影响力的计算次数,静态变量
    private static final Object lock = new Object(); // 锁对象，用于保证线程安全

    /**
     * 构造函数，初始化其中4个成员变量。
     * @param initialNodes 初始激活节点队列
     * @param initialStatus 初始激活状态映射
     * @param map 节点链接映射
     */
    public DiffusionModel(final Queue<String> initialNodes,
                          final HashMap<String, Integer> initialStatus,
                          final HashMap<String, Vector<String>> map) {
        this.initialNodes = initialNodes;
        this.initialStatus = initialStatus;
        this.map = map;
        this.averagePositiveInfluenceArray = new int[0];    // 初始化为长度为0的数组
    }

    /**
     * 执行扩散操作。
     */
    private void diffuse(Queue<String> newlyActivatedNodes, HashMap<String, Integer> currentStatus) {
        while (!newlyActivatedNodes.isEmpty()) {    // 扩散条件:新近激活节点队列非空
            Queue<String> henceActivatedNodes = new LinkedList<>();
            // 遍历新近激活节点,计算、更新相邻节点的激活状态
            for (String newlyActivatedNode : newlyActivatedNodes) { // 遍历新近激活节点
                Vector<String> links = map.get(newlyActivatedNode); // 获取链接

                if (links != null) {
                    for (String link : links) { // 遍历链接
                        String[] parts = link.split(" ");
                        // 获取目标节点、类别、激活概率
                        String targetNode = parts[1];
                        int linkCategory = Integer.parseInt(parts[2]);
                        double probability = Double.parseDouble(parts[3]);
                        // 激活条件:目标未激活且激活概率大于产生的随机数
                        if (Objects.equals(currentStatus.get(targetNode), 0) && (probability >= Math.random())) {
                            int targetStatus = linkCategory * currentStatus.get(newlyActivatedNode); // 计算目标状态
                            currentStatus.put(targetNode, targetStatus); // 更新目标状态
                            henceActivatedNodes.add(targetNode); // 加入本轮节点队列
                        }
                    }
                }
            }
            newlyActivatedNodes = henceActivatedNodes;  // 传递引用,原节点失去激活能力
        }
    }

    /**
     * 模拟多次，计算平均正向影响力。
     * @param numberOfSimulations 模拟次数
     * @param plotGraph 是否绘制图表
     * @return 平均正向影响力
     */
    public int calculateAverageInfluence(final int numberOfSimulations, // 模拟次数
                                         final boolean plotGraph) {     // 是否绘图
        int totalPositiveEffect = 0;    // 总正向影响力
        // 申请前n次平均正向影响力数组的存储空间并传递引用
        this.averagePositiveInfluenceArray = new int[numberOfSimulations];

        // 大量独立模拟，使用并行流多线程并行化模拟
        totalPositiveEffect = IntStream.range(0, numberOfSimulations)
                .parallel() // 创建整数并行流
                .map(n -> { // 对流中每个整数进行映射操作,即执行模拟操作
                    // 创建副本,不同内存地址
                    HashMap<String, Integer> currentStatus = new HashMap<>(this.initialStatus);
                    Queue<String> newlyActivatedNodes = new LinkedList<>(this.initialNodes);
                    diffuse(newlyActivatedNodes, currentStatus);    // 开始扩散
                    int iterPositiveEffectCount = 0;    // 一次模拟的正向影响力
                    for (int status : currentStatus.values()) { // 遍历当前激活状态,为1则正向影响力+1
                        if (status == 1) {
                            iterPositiveEffectCount++;
                        }
                    }
                    synchronized (lock) {   // 同步锁,保证线程安全
                        this.averagePositiveInfluenceArray[n] = iterPositiveEffectCount; // 记录本次模拟的正向影响力
                    }
                    return iterPositiveEffectCount;
                })
                .sum();  // 正向影响力累计值

        // 计算前n次模拟的平均正向影响力
        for (int n = 0; n < numberOfSimulations; n++) {
            // 获取averagePositiveInfluenceArray的前n项和
            int priorNPositiveEffect = IntStream.rangeClosed(0, n)
                    .map(i -> this.averagePositiveInfluenceArray[i])
                    .sum();
            this.averagePositiveInfluenceArray[n] =
                    priorNPositiveEffect / (n + 1); // 前n次模拟的平均正向影响力
        }

        // 返回平均正向影响力
        int averagePositiveEffect = totalPositiveEffect / numberOfSimulations;
        synchronized (lock) {
            calculationCount++;    // 计算平均正向影响力的次数+1
        }
        if (plotGraph) {
            // 绘制前n次模拟的平均正向影响力随模拟次数的变化图
            ToolBoxAPI.plotGraph(IntStream.rangeClosed(1, numberOfSimulations).toArray(), this.averagePositiveInfluenceArray);
        }
        return averagePositiveEffect;
    }

    /**
     * 静态成员方法，获取从类的构造到析构这一生命周期内所计算平均正向影响力的次数。
     * @return 计算次数
     */
    public static int getCalculationCount() {
        return calculationCount;
    }

    /**
     * 获取前 n 次模拟的平均正影响力组成的数组,用于观察模拟次数对平均正影响力的影响。
     * @return 前 n 次模拟的平均正影响力的数组
     */
    public int[] getAveragePositiveInfluenceArray() {
        return averagePositiveInfluenceArray;
    }
}