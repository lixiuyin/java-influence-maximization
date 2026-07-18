import java.util.HashMap;
import java.util.Vector;

public class GreedyAlgorithmMain {
    public static void main(String[] args){
        long timeStart=System.currentTimeMillis();

        // 从文件中加载数据,isLink为true表示按链接数据格式处理
        Vector<String> links = ToolBoxAPI.loadFile("data/edges.txt", true);
        Vector<String> nodes = ToolBoxAPI.loadFile("data/nodes.txt", false);
        ToolBoxAPI.HashMapService hashMapService = new ToolBoxAPI.HashMapService();

        /* 创建映射,键为节点,值为以其为起点的多条链接(起点 目标节点 类别 激活概率).
        为方便调用,可将HashMap序列化保存.若已有序列化文件,则直接读取,否则开始创建*/
        hashMapService.createOrRead(nodes, links, false);   // 不覆盖已有序列化文件
        HashMap<String, Vector<String>> map = hashMapService.getMap();  // 获取映射
        // 使用贪心算法选择规模为10的初始激活节点,每次计算平均正影响力时模拟500次
        InitialNodesSelector selector = new InitialNodesSelector(nodes, map, 10, 30);
        selector.selectInitialNodes();
        // 打印使平均正影响力增大时的计算次数数组和平均正影响力数组
        System.out.println("平均正向影响力计算次数数组：" + selector.getCalculateAverageInfluenceTimesList());
        System.out.println("平均正向影响力数组：" + selector.getPositiveAverageInfluenceList());

        long timeOver=System.currentTimeMillis();
        ToolBoxAPI.printFormattedTime(timeStart,timeOver);  // 打印运行时间
    }
}
