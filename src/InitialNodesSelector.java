import java.util.*;
import java.util.concurrent.*;

/**
 * InitialNodesSelector 类用于使用贪心算法选择初始激活节点。
 */
public class InitialNodesSelector {
    private final Vector<String> allNodes;
    private final HashMap<String, Vector<String>> map;
    private final int targetInitialSize;
    private final int numberOfSimulations;
    private final Queue<String> candidateInitialNodes;
    private final Queue<String> selectedNodes;
    private final ArrayList<Integer> averageInfluenceCalculationCounts = new ArrayList<>();
    private final ArrayList<Integer> positiveInfluenceList = new ArrayList<>();

    /**
     * 使用 所有节点、节点链接、目标初始激活点规模 初始化成员变量。
     * @param allNodes 所有节点组成的向量
     * @param map 节点链接映射
     * @param targetInitialSize 目标初始激活点规模
     * @param numberOfSimulations 计算每个初始激活点集的平均正影响力时的模拟次数
     */
    public InitialNodesSelector(final Vector<String> allNodes,
                                final HashMap<String, Vector<String>> map,
                                final int targetInitialSize,
                                final int numberOfSimulations) {
        this.allNodes = allNodes;
        this.map = map;
        this.targetInitialSize = targetInitialSize;
        this.numberOfSimulations = numberOfSimulations;
        this.candidateInitialNodes = new LinkedList<>(allNodes);    // 避免引用传递
        this.selectedNodes = new LinkedList<>();    // 避免引用传递
    }

    /**
     * 计算临时平均正影响力
     * @param tempSelectedNodes 临时已选的初始激活点链表队列
     * @return 临时平均正影响力
     */
    private int getTemporaryInfluence(Queue<String> tempSelectedNodes) {
        // 映射:当前临时已选的初始激活点的激活状态
        HashMap<String, Integer> tempActivatedStatus = new HashMap<>();
        for (String node : allNodes) {
            // 将已选的初始激活点的激活状态设为1,其余为0
            tempActivatedStatus.put(node, tempSelectedNodes.contains(node) ? 1 : 0);
        }
        // 申明并初始化扩散模型,计算并返回平均正向影响力
        DiffusionModel diffusionModel  = new DiffusionModel(tempSelectedNodes, tempActivatedStatus, map);
        return diffusionModel.calculateAverageInfluence(numberOfSimulations, false);
    }

    /**
     * InfluenceResult 静态内部类用于存储线程并发任务中的增大点、平均正向影响力和计算次数。
     */
    private static class InfluenceResult {
        String candidate;
        int influence;
        int calculationCount;
        InfluenceResult(String candidate, int influence, int calculationCount) {
            this.candidate = candidate;
            this.influence = influence;
            this.calculationCount = calculationCount;
        }
    }

    /**
     * 获取并发任务集合
     * @return 并发任务集合
     */
    private List<Callable<InfluenceResult>> getCallables() {
        List<Callable<InfluenceResult>> tasks = new ArrayList<>();
        for (String candidate : candidateInitialNodes) {
            // 创建"临时已选的初始激活点链表队列"并将该候选初始激活点加入
            Queue<String> tempSelectedNodes = new LinkedList<>(selectedNodes);
            tempSelectedNodes.offer(candidate);
            // 将任务加入集合
            tasks.add(() -> {
                // 计算临时平均正影响力和计算次数,返回结果类实例的引用
                int tempInfluence = getTemporaryInfluence(tempSelectedNodes);
                int calculationCount = DiffusionModel.getCalculationCount();
                return new InfluenceResult(candidate, tempInfluence, calculationCount);
            });
        }
        return tasks;   // 返回并发任务集合
    }

    /**
     * selectInitialNodes 方法使用贪心算法选择初始激活点。
     * 选择过程中,会打印每使平均正向影响力增大时的点、平均正影响力和计算次数。
     * 选择过程中,会打印每个确定新的初始激活点时的点、平均正影响力和计算次数。
     * 选择完成后,会打印最终选择的10个初始激活点和对应的平均正影响力。
     */
    public void selectInitialNodes() {
        int maxInfluence = 0;   // 最大平均正向影响力
        int[] maxInfluenceArray = new int[targetInitialSize + 1]; // 各点平均正向影响力
        // 并行化候选节点的评估过程
        // 创建一个固定总核心数大小的线程池
        System.out.println("当前系统可用核心数量: " + Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            // 贪心算法选择初始激活点
            for (int nodeIndex = 1; nodeIndex < targetInitialSize + 1; nodeIndex++) {
                String bestNewNode = null;
                int bestNewInfluence = maxInfluence;    // 每轮起点为上一轮的最大正向影响力

                // 创建并发任务集合
                List<Callable<InfluenceResult>> tasks = getCallables();

                // 执行并发任务并获取结果
                List<Future<InfluenceResult>> results = executor.invokeAll(tasks);

                // 处理并发任务结果
                for (Future<InfluenceResult> result : results) {
                    InfluenceResult influenceResult = result.get();
                    // 获取并发任务结果
                    String candidate = influenceResult.candidate;
                    int tempInfluence = influenceResult.influence;
                    int calculationCount = influenceResult.calculationCount;

                    // 执行贪心
                    if (tempInfluence > bestNewInfluence) { // 若临时平均正影响力更大
                        bestNewInfluence  = tempInfluence;   // 更新最大平均正影响力
                        bestNewNode = candidate;    // 更新对应的初始激活点
                        // 记录并打印每次增大时的计算次数和平均正向影响力，打印相关情况
                        averageInfluenceCalculationCounts.add(calculationCount);
                        positiveInfluenceList.add(bestNewInfluence);
                        printSelectionDetails(bestNewNode, bestNewInfluence, calculationCount);
                    }
                }

                if (bestNewNode != null) {
                    selectedNodes.offer(bestNewNode);   // 存入"已选的初始激活点"队列
                    candidateInitialNodes.remove(bestNewNode);  // 从候选初始激活点中移除
                    maxInfluence = bestNewInfluence; // 更新为本轮得到的最大正向影响力

                    maxInfluenceArray[nodeIndex] = maxInfluence; // 记录平均正向影响力
                    printCurrentSelection(nodeIndex, maxInfluence);   // 打印当前进度
                } else {
                    // 模拟次数太少导致平均正影响力不准确，就抛出异常，强制终止程序
                    throw new RuntimeException("未找到使正向影响力增大的点，请增加模拟次数");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();  // 关闭线程池
        }

        candidateInitialNodes.clear();  // 清空候选初始激活点,循环结束
        System.out.println("规模为" + targetInitialSize + "且正向影响力最大的初始激活点集为: " + selectedNodes);
        System.out.println("相应的平均正向影响力数组: " + Arrays.toString(Arrays.copyOfRange(maxInfluenceArray, 1, 11)));
    }

    /**
     * 记录平均正影响每次增大时的进度
     * @param bestNewNode 能使平均正影响力增大的初始激活点
     * @param bestNewInfluence 新的平均正向影响力
     */
    private void printSelectionDetails(String bestNewNode, int bestNewInfluence, int calculationCount) {
        System.out.println("若选择点: " + bestNewNode + ", 平均正向影响力增大为: " + bestNewInfluence + ", 计算平均正向影响力的次数: " + calculationCount);
    }

    /**
     * 记录每次确定一个新的初始激活点时的进度
     * @param nodeIndex 当前初始激活点的序号
     * @param maxInfluence 当前的最大平均正向影响力
     */
    private void printCurrentSelection(int nodeIndex, int maxInfluence) {
        System.out.println("已确定" + nodeIndex + "个初始激活点: " + selectedNodes + ", 当前平均正向影响力: " + maxInfluence + ", 计算平均正向影响力的次数: " + DiffusionModel.getCalculationCount());
    }

    /**
     * 获取使平均正影响力增大时的计算次数数组
     * @return 计算次数数组
     */
    public ArrayList<Integer> getCalculateAverageInfluenceTimesList() {
        return averageInfluenceCalculationCounts;
    }

    /**
     * 获取使平均正影响力增大时的平均正向影响力数组
     * @return 平均正向影响力数组
     */
    public ArrayList<Integer> getPositiveAverageInfluenceList() {
        return positiveInfluenceList;
    }
}