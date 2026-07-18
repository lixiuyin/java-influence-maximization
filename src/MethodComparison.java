import java.util.*;

public class MethodComparison {
/**
 * MethodComparison 类用于比较根据出度、正出度及贪心算法这三种方法求解正影响力最大化问题的效果。
 */
    private final DiffusionModel model;
    private final int averageInfluence;

    /**
     * 构造函数，初始化3个成员变量。
     * @param allNodes 所有节点组成的向量
     * @param initialActivatedNodes 初始激活节点队列
     * @param numberOfSimulations 计算每个初始激活点集的平均正影响力时的模拟次数
     */
    public MethodComparison(final Vector<String> allNodes,
                            final HashMap<String, Vector<String>> map,
                            final Queue<String> initialActivatedNodes,
                            final int numberOfSimulations) {
        // 调用静态方法获取初始激活状态、初始化扩散模型
        HashMap<String, Integer> status = ToolBoxAPI.getInitialStatus(initialActivatedNodes, allNodes);
        this.model = new DiffusionModel(initialActivatedNodes, status, map);
        // 调用扩散模型实例方法计算平均正向影响力
        this.averageInfluence = model.calculateAverageInfluence(numberOfSimulations, false);
    }

    /**
     * 获取前 n 次模拟的平均正影响力组成的数组
     * @return 前 n 次模拟的平均正影响力数组
     */
    public int[] getIterPositiveEffectCounts() {
        return model.getAveragePositiveInfluenceArray();
    }

    /**
     * 规范化打印描述和平均正向影响力
     */
    private static void printMethodComparison(String description,
                                              Vector<String> nodes,
                                              HashMap<String, Vector<String>> map,
                                              Queue<String> initialActivatedNodes,
                                              int simulations) {
        System.out.println(description + new MethodComparison(nodes, map, initialActivatedNodes, simulations).averageInfluence);
    }

    public static void main(String[] args) {
        long timeStart = System.currentTimeMillis();
        Vector<String> links = ToolBoxAPI.loadFile("data/edges.txt", true);
        Vector<String> nodes = ToolBoxAPI.loadFile("data/nodes.txt", false);
        ToolBoxAPI.HashMapService hashMapService = new ToolBoxAPI.HashMapService();
        hashMapService.createOrRead(nodes, links, false);
        HashMap<String, Vector<String>> map = hashMapService.getMap();
        // 打印(正)出度最高的十个点和对应(正)出度
        // ToolBoxAPI.printOutDegreeDetails(map, false);
        // ToolBoxAPI.printOutDegreeDetails(map, true);
        // 获取各自的初始激活节点
        Queue<String> group1 = new LinkedList<>(Arrays.asList("184", "15", "1832", "163", "1116", "1397", "1981", "1300", "165", "1810"));
        Queue<String> group2 = new LinkedList<>(Arrays.asList("184", "15", "1832", "1116", "163", "1300", "1287", "165", "1981", "1397"));
        Queue<String> group3 = new LinkedList<>(Arrays.asList("184", "15", "1832", "1116", "163", "1287", "1397", "1300", "165", "13382"));
        Queue<String> group4 = new LinkedList<>(Arrays.asList("184", "15", "1832", "1116", "163", "1397", "1556", "165", "1287", "1810"));
        Queue<String> group5 = new LinkedList<>(Arrays.asList("184", "15", "1832", "163", "1116", "165", "1287", "1397", "1556", "1808"));
        Queue<String> outDegreeNodes = new LinkedList<>(ToolBoxAPI.getTop10OutDegreeNodes(map).keySet());
        Queue<String> positiveOutDegreeNodes = new LinkedList<>(ToolBoxAPI.getTop10PositiveOutDegreeNodes(map).keySet());
        // 打印各种方法的平均正向影响力(模拟次数为20000)
        printMethodComparison("贪心算法组一: ", nodes, map, group1, 20000);
        printMethodComparison("贪心算法组二: ", nodes, map, group2, 20000);
        printMethodComparison("贪心算法组三: ", nodes, map, group3, 20000);
        printMethodComparison("贪心算法组四: ", nodes, map, group4, 20000);
        printMethodComparison("贪心算法组五: ", nodes, map, group5, 20000);
        printMethodComparison("出度最高的10个点: ", nodes, map, outDegreeNodes, 20000);
        printMethodComparison("正出度最高的10个点: ", nodes, map, positiveOutDegreeNodes, 20000);
        // 画前n次模拟的平均正影响力随模拟次数变化的图
        // ToolBoxAPI.plotGraph(methodComparator1.getIterPositiveEffectCounts());
        // ToolBoxAPI.plotGraph(methodComparator2.getIterPositiveEffectCounts());
        ToolBoxAPI.printFormattedTime(timeStart, System.currentTimeMillis());
    }
}
