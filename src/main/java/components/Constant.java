package components;

import java.util.HashMap;
import java.util.List;

public class Constant {
    public static List<String> vnfType = List.of(new String[]{"NAT", "FW", "IDS", "LB", "DPI", "Gateway","Encryption","Proxy","VPN"});
    public static int nodeCpuResource = 200;
    public static int nodeMemResource = 200;
    public static int bandwidth = 1000;
    public static int delay = 10;

    // 负载上下界,单节点负载上界
    public static final double thresholdUP = 0.7;
    public static final double thresholdDOWN = 0.5;
    public static final double thresholdNode = 1.2;
    // cpu消耗资源跟流量率的比率
    public static HashMap<String, Integer> consumeCpuRatio = new HashMap<>();
    // mem消耗资源跟流量率的比率
    public static HashMap<String, Integer> consumeMemRatio = new HashMap<>();
    // 特定VNF消耗的cpu固定资源
    public static HashMap<String, Integer> defaultCpuConsumption = new HashMap<>();
    // 特定VNF消耗的cpu固定资源
    public static HashMap<String, Integer> defaultMemConsumption = new HashMap<>();
    // 特定VNF执行sfc的延时跟流量率的比率
    public static HashMap<String, Integer> delayRatio = new HashMap<>();

    // 迁移的代价和sfc流量率的比率
    public static HashMap<String, Double> migrateCostRatio = new HashMap<>();
    static {
        defaultCpuConsumption.put("NAT", 10);
        defaultMemConsumption.put("NAT", 10);
        delayRatio.put("NAT", 2);
        consumeCpuRatio.put("NAT",3);
        consumeMemRatio.put("NAT",3);
        migrateCostRatio.put("NAT", 2.0);

        defaultCpuConsumption.put("FW", 10);
        defaultMemConsumption.put("FW", 10);
        delayRatio.put("FW", 2);
        consumeCpuRatio.put("FW",3);
        consumeMemRatio.put("FW",3);
        migrateCostRatio.put("FW", 2.0);

        defaultCpuConsumption.put("LB", 10);
        defaultMemConsumption.put("LB", 10);
        delayRatio.put("LB", 2);
        consumeCpuRatio.put("LB",3);
        consumeMemRatio.put("LB",3);
        migrateCostRatio.put("LB", 2.0);

        defaultCpuConsumption.put("IDS", 10);
        defaultMemConsumption.put("IDS", 10);
        delayRatio.put("IDS", 2);
        consumeCpuRatio.put("IDS",3);
        consumeMemRatio.put("IDS",3);
        migrateCostRatio.put("IDS", 2.0);

        defaultCpuConsumption.put("DPI", 10);
        defaultMemConsumption.put("DPI", 10);
        delayRatio.put("DPI", 2);
        consumeCpuRatio.put("DPI",3);
        consumeMemRatio.put("DPI",3);
        migrateCostRatio.put("DPI", 2.0);

        defaultCpuConsumption.put("Gateway", 10);
        defaultMemConsumption.put("Gateway", 10);
        delayRatio.put("Gateway", 2);
        consumeCpuRatio.put("Gateway",3);
        consumeMemRatio.put("Gateway",3);
        migrateCostRatio.put("Gateway", 2.0);

        defaultCpuConsumption.put("Encryption", 10);
        defaultMemConsumption.put("Encryption", 10);
        delayRatio.put("Encryption", 2);
        consumeCpuRatio.put("Encryption",3);
        consumeMemRatio.put("Encryption",3);
        migrateCostRatio.put("Encryption", 2.0);

        defaultCpuConsumption.put("Proxy", 10);
        defaultMemConsumption.put("Proxy", 10);
        delayRatio.put("Proxy", 2);
        consumeCpuRatio.put("Proxy",3);
        consumeMemRatio.put("Proxy",3);
        migrateCostRatio.put("Proxy", 2.0);

        defaultCpuConsumption.put("VPN", 10);
        defaultMemConsumption.put("VPN", 10);
        delayRatio.put("VPN", 2);
        consumeCpuRatio.put("VPN",3);
        consumeMemRatio.put("VPN",3);
        migrateCostRatio.put("VPN", 2.0);
    }
}
