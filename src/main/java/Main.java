import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;

import components.*;

import migration.DataCollector;
import migration.PolicyImplementer;
import placement.NetworkInspector;
import placement.PlacementMaker;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static NetworkInspector networkInspector = new NetworkInspector();
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        Scanner scanner = new Scanner(System.in);
        int action;
        char flag;
        Topo topo = readTopoStorage();
        networkInspector = new NetworkInspector();
        networkInspector.setTopo(topo);
        List<SFC> sfcs = null;
        PlacementMaker placementMaker = new PlacementMaker();

        // 持久化读入拓扑和请求

        File file = new File("requests.json");
        if(file.exists()){
            String jsonString = FileUtil.readString(new File("requests.json"), Charset.forName("GBK"));
            sfcs = JSONUtil.parseArray(jsonString).toList(SFC.class);
            topo.setSfcs(sfcs);
        }
//        if(topo != null){
//            topo.setVnfs(readVNFs(sfcs, topo));
//        }
        if(sfcs == null){
            sfcs = new ArrayList<>();
        }
        boolean isDeployed = false;
        List<SFC> sfcleft = new ArrayList<>();
        int index = sfcs.size() + 1;

        while(true){
            System.out.println("请输入你需要执行的操作的序号:");
            System.out.println("1. 输入拓扑");
            System.out.println("2. 输入SFC请求");
            System.out.println("3. 新增SFC请求");
            System.out.println("4. 修改SFC请求流量率");
            System.out.println("5. 执行请求部署");
            System.out.println("6. 执行迁移算法");
            System.out.println("7. 评价当前SFC部署方案");
            System.out.println("8. 评价当前网络资源占用情况");
            System.out.println("9. 打印当前网络状况");
            System.out.println("10. 打印SFC请求情况");
            System.out.println("11. 退出(注意!推出后所有网络信息不会被保存!)");
            action = scanner.nextInt();
            switch(action) {
                case 1: {
                    if (topo != null){
                        System.out.println("当前已经存在网络拓扑,是否覆盖?(y/n):");
                        flag = scanner.next().charAt(0);
                        if(flag == 'y') {
                            topo = inputTopology();
                            networkInspector.setTopo(topo);
                            topo.setSfcs(sfcs);
                        }
                    }else {
                        topo = inputTopology();
                        networkInspector.setTopo(topo);
                        topo.setSfcs(sfcs);
                    }
                    break;
                }
                case 2:{
                    if(topo == null){
                        System.out.println("请先创建拓扑信息!");
                        break;
                    }
                    if (sfcs.size() != 0){
                        System.out.println("当前已经存在SFC请求集合,是否覆盖?(y/n):");
                        flag = scanner.next().charAt(0);
                        if(flag == 'y') {
                            sfcs = inputRequest();
                            topo.setSfcs(sfcs);
                            sfcleft.clear();
                        }
                    }else {
                        sfcs = inputRequest();
                        topo.setSfcs(sfcs);
                        sfcleft.clear();
                    }
                    index = sfcs.size()+1;
                    break;
                }
                case 3:{
                    printSFCRequestInfo();
                    sfcleft.add(inputSFC(index++));
                    if(!isDeployed){
                        sfcs.addAll(sfcleft);
                        sfcleft.clear();
                    }
                    storeSFC(sfcs, sfcleft);
                    break;
                }
                case 5:{
                    if(sfcs.size()==0 || topo == null){
                        System.out.println("请先创建拓扑和构建sfc请求");
                    }else {
                        if(topo.getVnfs()!= null && topo.getVnfs().size() != 0){
                            System.out.println("当前已经存在部署信息,是否重置?(y/n)");
                            flag = scanner.next().charAt(0);
                            if(flag == 'y') {
                                sfcs.forEach(sfc -> {
                                    sfc.setAccessToVNF0(null);
                                    sfc.setVnfList(null);
                                    sfc.setLinkList(null);
                                });
                                topo = readTopoStorage();
                                topo.setSfcs(sfcs);
                                networkInspector.setTopo(topo);
                                isDeployed = false;
                                NetworkInspector.index = 1;
                            }else {
                                break;
                            }
                        }
                        System.out.println("请选择需要执行的放置算法的序号");
                        System.out.println("1. 基于遗传算法的SFC放置算法");
                        System.out.println("2. 共享贪心的SFC放置算法");
                        int algorithmNum = scanner.nextInt();
                        if(isDeployed){
                            if(sfcleft.size() != 0){
                                topo.getVnfs().addAll(placementMaker.makePlacement(topo, sfcleft, algorithmNum));
                                topo.getSfcs().addAll(sfcleft);
                                sfcleft.clear();
//                                storeVNFs(topo.getVnfs(), sfcs, topo);
                            }else {
                                System.out.println("当前不存在还未部署的SFC请求!");
                            }
                        }else {
                            topo.setVnfs(placementMaker.makePlacement(topo, topo.getSfcs(), algorithmNum));
                            isDeployed = true;
//                            storeVNFs(topo.getVnfs(), sfcs, topo);
                        }
                    }
                    break;
                }
                case 4:{
                    if(topo == null){
                        System.out.println("请先创建拓扑信息!");
                        break;
                    }
                    changeState(topo, index, isDeployed);
                    break;
                }
                case 6:{
                    if(topo == null || topo.getVnfs() == null || topo.getVnfs().size() == 0){
                        System.out.println("请先创建拓扑信息并执行部署！");
                        break;
                    }
                    System.out.println("请选择需要执行的迁移(调整)算法的序号");
                    System.out.println("1. 标准动态VNF迁移算法");
                    System.out.println("2. 纯优化的VNF部分迁移算法");
                    System.out.println("3. 负载均衡的VNF部分迁移算法");
                    System.out.println("4. 传统VNF迁移算法");
                    int algorithmNum = scanner.nextInt();
                    PolicyImplementer policyImplementer = new PolicyImplementer(new DataCollector(topo));
                    policyImplementer.adjust(algorithmNum);
//                    storeVNFs(topo.getVnfs(), sfcs, topo);
                    break;
                }
                case 7:{
                    if (topo == null || sfcs.size() == 0 || topo.getVnfs().size() == 0){
                        System.out.println("请先创建拓扑和构建sfc请求并执行部署！");
                        break;
                    }
                    System.out.println("评价结果如下:");
                    networkInspector.evaluateSFC();
                    break;
                }
                case 8:{
                    if(topo == null){
                        System.out.println("请先创建拓扑信息!");
                        break;
                    }
                    System.out.println("评价结果如下:");
                    networkInspector.evaluateTopo();
                    break;
                }
                case 9: {
                    if(topo == null){
                        System.out.println("请先输入拓扑信息！");
                        break;
                    }
                    System.out.println("当前网络状态如下:");
                    networkInspector.printNetworkInfo();
                    break;
                }
                case 10: {
                    if(sfcs.size() == 0){
                        System.out.println("请先输入SFC请求！");
                        break;
                    }
                    System.out.println("SFC请求状况如下:");
                    networkInspector.printSFCRequestInfo();
                    break;
                }
                case 99: {
                    if (topo != null){
                        topo.setVnfs(inputVNF(topo));
                        isDeployed = true;
                    }
                }
            }

            if(action == 12)break;
        }

    }
    public static Topo inputTopology() throws InterruptedException, FileNotFoundException {

        System.out.println("请选择输入网络拓扑的方式:");
        Thread.sleep(500);
        System.out.println("1. 命令行输入");
        System.out.println("2. 文件输入(请确保topology.txt在文件目录下且格式正确!)");
        Scanner scanner = new Scanner(System.in);
        int inputWay = scanner.nextInt();
        System.out.println("该拓扑是否包括P4交换机?(y/n)");
        char c = scanner.next().charAt(0);
        if(inputWay == 2){
            File file = new File("topology.txt");
            if(file.exists()){
                scanner = new Scanner(new FileInputStream(file));
            }
        }

        if(inputWay == 1)System.out.println("请输入节点数量:");
        /**
         * 输入形式：第一行节点数量，假设每个节点的资源都是相同的
         */
        int nodeNum = scanner.nextInt();
        List<Node> nodes = new ArrayList<>();
        int flag;
        if(c == 'y'){
            if(inputWay == 1)System.out.println("按序输入每个节点是否是P4交换机:1表示是, 0表示不是");
            for (int i = 1; i <= nodeNum; i++) {
                if(inputWay == 1)System.out.print("当前节点"+i+":");
                flag = scanner.nextInt();
                nodes.add(new Node(i, Constant.nodeCpuResource / (flag == 1 ? 2 : 1), Constant.nodeMemResource / (flag == 1 ? 2 : 1), flag == 1));
            }
        }else {
            for (int i = 1; i <= nodeNum; i++) {
                nodes.add(new Node(i, Constant.nodeCpuResource, Constant.nodeMemResource));
            }
        }

        /**
         * 先输入链路的数量
         * 不需要指定链路的带宽,但是需要指定链路连接的节点是哪两个
         * 输入:1 2 表示1到2有链路
         */
        if(inputWay == 1)System.out.println("请输入链路数量:");
        int linkNum = scanner.nextInt();
        List<Link> links = new ArrayList<>();
        int num1, num2;
        if(inputWay == 1)System.out.println("请输入链路信息:(如1 2表示节点1和2中间有连接)");
        for (int i = 1; i <= linkNum; i++) {
            // 第一个节点序号
            num1 = scanner.nextInt();
            // 第二个节点序号
            num2 = scanner.nextInt();
            Link link = new Link(i, Constant.bandwidth, nodes.get(num1 - 1), nodes.get(num2 - 1), Constant.delay);
            links.add(link);
            nodes.get(num1 - 1).getPorts().add(link);
            nodes.get(num2 - 1).getPorts().add(link);
        }
        Topo topo = new Topo(nodes, links);
        storeTopo(topo);
        System.out.println("拓扑输入成功!");
        return topo;
    }
    public static List<SFC> inputRequest() throws InterruptedException {
        System.out.println("请输入SFC请求");
        Thread.sleep(500);
        System.out.println("请输入请求数量:");
        Scanner scanner = new Scanner(System.in);
        int sfcNum = scanner.nextInt();
        printSFCRequestInfo();

        /**
         * 输入形式：第一行sfc数量
         */
        List<SFC> result = new ArrayList<>();
        for (int i = 0; i < sfcNum; i++) {
            result.add(inputSFC(i+1));
        }
        storeSFC(result);
        return result;
    }

    public static void printSFCRequestInfo() {
        System.out.println("请输入sfc请求:\n样例:3 1 2 3 1 10 100\n表示需要经过3个vnf,类型1, 2, 3, 接入点为节点1，流量率10, 最大容忍延时150");
        System.out.println("类型对应关系:");
        List<String> vnfType = Constant.vnfType;
        for (int i = 0; i < vnfType.size(); i++) {
            System.out.println((i+1)+" "+vnfType.get(i));
        }
    }
    public static SFC inputSFC(int index){
        int vnfNum, type, flowRate, delay, accessNodeId;
        List<String> vnfString;
        List<String> vnfType = Constant.vnfType;
        Scanner scanner = new Scanner(System.in);
        vnfNum = scanner.nextInt();
        vnfString = new ArrayList<>();
        while (vnfNum-->0){
            type = scanner.nextInt();
            vnfString.add(vnfType.get(type - 1));
        }
        accessNodeId = scanner.nextInt();
        flowRate = scanner.nextInt();
        delay = scanner.nextInt();
        return new SFC(index, delay, flowRate, accessNodeId, vnfString);
    }


    public static void changeState(Topo topo, int index, boolean isDeployed){
        System.out.println("请输入你需要修改的SFC请求的数量和序号,如:3 1 2 3");
        Scanner scanner = new Scanner(System.in);
        List<SFC> sfcs = topo.getSfcs();
        List<SFC> sfcList = new ArrayList<>();
        int num = scanner.nextInt();
        for (int i = 0; i < num; i++) {
            int sfcId = scanner.nextInt();
            if(sfcId >= index) {
                System.out.println("不存在该SFC");
                return;
            }
            sfcList.add(sfcs.stream().filter(sfc1 -> sfc1.getSfcId() == sfcId).toList().get(0));
        }
        System.out.println("请输入新流量率:");
        int flowRate = scanner.nextInt();
        for(SFC sfc: sfcList){
            if(isDeployed){
                sfc.update(flowRate);
            }
            sfc.setFlowRate(flowRate);
        }

//        storeSFC(sfcs);
    }
    public static void storeSFC(List<SFC> sfcs){
        List<List<VNF>> vnfList = new ArrayList<>();
        List<List<Link>> linkList = new ArrayList<>();
        List<List<VirtualLink>> virtualLinkList = new ArrayList<>();
        sfcs.forEach(sfc -> {
            vnfList.add(sfc.getVnfList());
            linkList.add(sfc.getAccessToVNF0());
            virtualLinkList.add(sfc.getLinkList());
            sfc.setVnfList(null);
            sfc.setAccessToVNF0(null);
            sfc.setLinkList(null);
        });
        String jsonString = JSONUtil.toJsonPrettyStr(sfcs);
        FileUtil.writeString(jsonString, new File("requests.json"), "GBK");
        for (int i = 0; i < sfcs.size(); i++) {
            sfcs.get(i).setVnfList(vnfList.get(i));
            sfcs.get(i).setAccessToVNF0(linkList.get(i));
            sfcs.get(i).setLinkList(virtualLinkList.get(i));
        }
    }
    public static void storeSFC(List<SFC> sfcs, List<SFC> sfcleft){
        List<SFC> sfcList = new ArrayList<>(sfcs);
        sfcList.addAll(sfcleft);
        storeSFC(sfcList);
    }

    public static void storeTopo(Topo topo){
        List<Node> nodes = topo.getNodes();
        List<List<Link>> links = new ArrayList<>();
        nodes.forEach(node -> {
            links.add(node.getPorts());
            node.setPorts(null);
        });
        String jsonString = JSONUtil.toJsonPrettyStr(topo);
        FileUtil.writeString(jsonString, new File("topology.json"), "GBK");
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setPorts(links.get(i));
        }
    }

    public static Topo readTopoStorage(){
        File file = new File("topology.json");
        Topo topo = null;
        if(file.exists()){
            String jsonString = FileUtil.readString(new File("topology.json"), Charset.forName("GBK"));
            topo = JSONUtil.toBean(jsonString, Topo.class);
        }
        if(topo != null){
            List<Node> nodes = topo.getNodes();
            nodes.forEach(node -> {
                if(node.getPorts() == null) node.setPorts(new ArrayList<>());
            });
            Node fakeNode1, fakeNode2;
            for (Link link: topo.getLinks()) {
                fakeNode1 = link.getNode1();
                fakeNode2 = link.getNode2();
                link.setNode1(nodes.get(fakeNode1.getNodeId() - 1));
                link.setNode2(nodes.get(fakeNode2.getNodeId() - 1));
                link.getNode1().addLink(link);
                link.getNode2().addLink(link);
            }
        }
        return topo;
    }
    public static void storeVNFs(List<VNF> vnfs, List<SFC> sfcs,Topo topo){
        List<List<VNF>> vnfList = new ArrayList<>();
        List<List<Link>> linkList = new ArrayList<>();
        List<List<VirtualLink>> virtualLinkList = new ArrayList<>();
        List<Node> nodes = topo.getNodes();
        List<List<Link>> links = new ArrayList<>();
        List<List<VNF>> vnfList2 = new ArrayList<>();
        nodes.forEach(node -> {
            links.add(node.getPorts());
            vnfList2.add(node.getVnfList());
            node.setPorts(null);
            node.setVnfList(null);
        });
        sfcs.forEach(sfc -> {
            vnfList.add(sfc.getVnfList());
            linkList.add(sfc.getAccessToVNF0());
            virtualLinkList.add(sfc.getLinkList());
            sfc.setVnfList(null);
            sfc.setAccessToVNF0(null);
            sfc.setLinkList(null);
        });
        String jsonString = JSONUtil.toJsonPrettyStr(vnfs);
        FileUtil.writeString(jsonString, new File("result.json"), "GBK");
        for (int i = 0; i < sfcs.size(); i++) {
            sfcs.get(i).setVnfList(vnfList.get(i));
            sfcs.get(i).setAccessToVNF0(linkList.get(i));
            sfcs.get(i).setLinkList(virtualLinkList.get(i));
        }
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setPorts(links.get(i));
            nodes.get(i).setVnfList(vnfList2.get(i));
        }
    }
    public static List<VNF> readVNFs(List<SFC> sfcs, Topo topo){
        File file = new File("result.json");
        List<VNF> vnfList = null;
        if(file.exists()){
            String jsonString = FileUtil.readString(new File("result.json"), Charset.forName("GBK"));
            vnfList = JSONUtil.parseArray(jsonString,true).toList(VNF.class);
        }
        SFC sfc, s;
        if(vnfList != null && sfcs!= null){
            for(VNF vnf : vnfList){
                for(int j = 0; j < vnf.getSfcList().size(); j++){
                    s = vnf.getSfcList().get(j);
                    int finalSfcId = s.getSfcId();
                    sfc = sfcs.stream().filter(sfc1 -> sfc1.getSfcId() == finalSfcId).findAny().get();
                    sfc.setVnfList(new ArrayList<>(s.getRequiredVNF().size()));
                    for (int i = 0; i < s.getRequiredVNF().size(); i++) {
                        if(s.getRequiredVNF().get(i).equals(vnf.getVnfType())){
                            sfc.setVNF(vnf, i);
                            break;
                        }
                    }
                    vnf.getSfcList().set(j, sfc);
                }
            }
            vnfList.forEach(vnf -> {
                Node node = topo.getNodes().get(vnf.getNode().getNodeId() - 1);
                node.addVNF(vnf);
                vnf.setNode(node);
            });
            sfcs.forEach(sfc1 -> sfc1.updateLinks(topo));
        }
        return vnfList;
    }
    public static List<VNF> inputVNF(Topo topo){
        List<Node> nodes = topo.getNodes();
        List<SFC> sfcs = topo.getSfcs();
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入每个SFC的部署方式");
        int nodeNum;
        List<Node> deployResult = new ArrayList<>();
        List<VNF> result = new ArrayList<>();
        for (SFC sfc: sfcs){
            System.out.println(networkInspector.getSFCInfo(sfc));
            for (int i = 0; i < sfc.getRequiredVNF().size(); i++) {
                nodeNum = scanner.nextInt();
                deployResult.add(nodes.get(nodeNum - 1));
            }
            result.addAll(networkInspector.deploySFC(deployResult, sfc));
            deployResult.clear();
        }
        return result;
    }
}
