package migration;

import components.*;
import placement.NetworkInspector;

import java.util.*;
import java.util.stream.Collectors;

public class PolicyImplementer {
    static int indexOfVNF;
    DataCollector dataCollector;
    static Set<VNF> vnfTemp = new HashSet<>();
    static Set<VNF> vnfNext = new HashSet<>();

    public PolicyImplementer(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
        indexOfVNF = dataCollector.getTopo().getVnfs().size();
    }
    public void adjust(int algorithmNum){
        switch (algorithmNum){
            case 1 : migrate(Constant.thresholdUP, Constant.thresholdDOWN);break;
            case 2 : migrateWhenMiddleLoad(1, 0);break;
            case 3 : migrateWhenMiddleLoad(1, 1);break;
            case 4 : migrateWhenHighLoad();break;
        }
    }
    public void migrate(double thresholdUP, double thresholdDown){
//        migrateWhenLowLoad(0.2);
        if(dataCollector.getCpuLoad() >= thresholdUP || dataCollector.getMemLoad() >= thresholdUP){
            System.out.println("执行了高负载迁移");
            migrateWhenHighLoad();
        }else if(dataCollector.getCpuLoad() > thresholdDown || dataCollector.getMemLoad() > thresholdDown){
            System.out.println("执行了中负载迁移");
            migrateWhenMiddleLoad(thresholdDown, thresholdUP);
        }else {
            System.out.println("执行了低负载迁移");
            migrateWhenLowLoad(0.2);
        }
    }


    public void migrateWhenHighLoad(){
        Topo topo = dataCollector.getTopo();
        List<Node> nodes = dataCollector.getOverloadedNodes();
        List<VNF> vnfList;
        List<Node> nodesNotOverload = dataCollector.getNotOverloadedNodes();
        nodesNotOverload.sort(Comparator.comparingInt(node -> -node.getRestCpuResource()-node.getRestMemResource()));
        // flag表示有没有找到期待的解
        boolean flag;
        for(Node node: nodes){
            flag = false;
            vnfList = dataCollector.getCompleteVNFs(node);
            if(vnfList == null || vnfList.size() == 0){
                List<VNF> vnfs = new ArrayList<>(node.getVnfList());
                vnfs.sort(Comparator.comparingDouble(vnf -> (-1)*getWeight(vnf)));
                for (VNF vnf : vnfs) {
                    flag = false;
                    Node node2 = null;
                    for (Node node1 : nodesNotOverload) {
                        if (dataCollector.nodeHostsVNF(node1, vnf)) {
                            if (!dataCollector.overDelay(vnf, node1)) {
                                // 把VNF放到目标节点上不会自动把源节点取消掉。
                                node.removeVNF(vnf);
                                dataCollector.addVNFToNode(node1, vnf);
                                flag = true;
                                break;
                            }
                            if (node2 == null) {
                                node2 = node1;
                            }
                        }
                    }
                    if(!flag && node2 != null){
                        node.removeVNF(vnf);
                        dataCollector.addVNFToNode(node2, vnf);
                    }
                    if (!node.isOverload()) break;
                }
            }else {
                vnfList.sort(Comparator.comparingDouble(vnf -> (-1)*getWeight(vnf)));
                Node node2 = null;
                VNF vnf1 = null;
                for (VNF vnf : vnfList) {
                    for (Node node1 : nodesNotOverload) {
                        if (dataCollector.nodeHostsVNF(node1, vnf)) {
//                            System.out.println(node.getNodeId()+" "+vnf.getVnfId()+"  "+node1.getNodeId());
                            if (!dataCollector.overDelay(vnf, node1)) {
                                System.out.println("Node:"+node.getNodeId()+",VNF"+vnf.getVnfId()+",dstNode"+node1.getNodeId());
                                node.removeVNF(vnf);
                                dataCollector.addVNFToNode(node1, vnf);
                                flag = true;
                                break;
                            } else if (node2 == null) {
                                node2 = node1;
                                vnf1 = vnf;
                            }
                        }
                    }
                    if (flag) break;
                }
                if(!flag && node2 !=null){
                    System.out.println("Node:"+node.getNodeId()+",VNF"+vnf1.getVnfId()+",dstNode"+node2.getNodeId());
                    node.removeVNF(vnf1);
                    dataCollector.addVNFToNode(node2, vnf1);
                }
            }
            if(!node.isOverload()){
                nodesNotOverload = dataCollector.getNotOverloadedNodes();
                nodesNotOverload.sort(Comparator.comparingInt(node1 -> -node1.getRestCpuResource()-node1.getRestMemResource()));
            }
        }
    }
    public double getWeight(VNF vnf){
        return vnf.getCpuConsumed() + vnf.getMemConsumed() - vnf.getMigrateCost();
    }
    public void migrateWhenMiddleLoad(double downThreshold, double upThreshold) {
        Topo topo = dataCollector.getTopo();
        List<Node> nodes = dataCollector.getOverloadedNodes(Constant.thresholdNode);
        List<VNF> vnfList;
        List<Node> nodesNotOverload = dataCollector.getNotOverloadedNodes();
        nodesNotOverload.sort(Comparator.comparingInt(node -> node.getRestCpuResource() + node.getRestMemResource()));
        Collections.reverse(nodesNotOverload);
        boolean flag;
        for (Node node : nodes) {
            vnfList = dataCollector.getCompleteVNFs(node);
            if (vnfList == null || vnfList.size() == 0) {
                List<VNF> vnfs = new ArrayList<>(node.getVnfList());
                vnfs.sort(Comparator.comparingDouble(vnf -> (-1) * getWeight(vnf)));
                for (VNF vnf : vnfs) {
                    for (Node node1 : nodesNotOverload) {
                        if (dataCollector.nodeHostsVNF(node1, vnf)) {
                            if (!dataCollector.overDelay(vnf, node1)) {
                                // 把VNF放到目标节点上不会自动把源节点取消掉。
                                node.removeVNF(vnf);
                                dataCollector.addVNFToNode(node1, vnf);
                                break;
                            }
                        }
                    }
                    if (!node.isOverload()) break;
                }
            }else {
                flag = false;
                vnfList.sort(Comparator.comparingDouble(vnf -> (-1)*getWeight(vnf)));
                for (VNF vnf : vnfList) {
                    for (Node node1 : nodesNotOverload) {
                        if (dataCollector.nodeHostsVNF(node1, vnf)) {
                            if (!dataCollector.overDelay(vnf, node1)) {
                                node.removeVNF(vnf);
                                dataCollector.addVNFToNode(node1, vnf);
                                nodesNotOverload.add(node);
                                flag = true;
                                break;
                            }
                        }
                    }
                    if (flag) break;
                }
            }
        }
        nodes = dataCollector.getOverloadedNodesUnder(Constant.thresholdNode);
        nodesNotOverload = dataCollector.getNotOverloadedNodes();
        nodesNotOverload.sort(Comparator.comparingInt(node -> -node.getRestCpuResource() - node.getRestMemResource()));
        migrate(nodes, nodesNotOverload, 1.0,1.0,topo);


        nodes = dataCollector.getNodeUpper(upThreshold);
        nodesNotOverload = dataCollector.getNodeUnder(downThreshold);
        nodes.sort(Comparator.comparingDouble(node -> -getLoad(node)));
        nodesNotOverload.sort(Comparator.comparingInt(node -> -node.getRestCpuResource() - node.getRestMemResource()));
        migrate(nodes, nodesNotOverload, downThreshold,upThreshold, topo);
    }
    public void migrateWhenLowLoad(double a){
        List<VNF> vnfList = dataCollector.getTopo().getVnfs();
        List<VNF> vnfNeedMerge;
        Map<String, List<VNF>> vnfMap = vnfList.stream().collect(Collectors.groupingBy(VNF::getVnfType));
        List<VNF> vnfs;
        for(String s: vnfMap.keySet()){
            vnfList = vnfMap.get(s);
            vnfNeedMerge = vnfList.stream().filter(vnf -> vnf.getDefaultCpuConsumption() >= a * vnf.getProvidedCpuResource() &&
                    vnf.getDefaultMemConsumption() >= a * vnf.getProvidedMemResource()).collect(Collectors.toList());
            vnfs = new ArrayList<>(vnfNeedMerge);
            for(VNF vnf : vnfNeedMerge){
                if(vnf.getDefaultCpuConsumption() >= a * vnf.getProvidedCpuResource() &&
                        vnf.getDefaultMemConsumption() >= a * vnf.getProvidedMemResource()){        //加入这个条件是因为可能有的实例被当作迁移的目标之后就不再满足条件了。
                    for(VNF vnf1 : vnfList){
                        if (vnf != vnf1 && vnfs.contains(vnf1)){
                            if(dataCollector.worthMerge(vnf, vnf1)){
                                vnf.getNode().removeVNF(vnf);
                                dataCollector.addVNFToNode(vnf1.getNode(), vnf);
                                vnfs.remove(vnf);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    public double getLoad(Node node){
        return 0.5 * node.getUsedCpuResource() / node.getCpuResource() + 0.5 * node.getUsedMemResource() / node.getMemResource();
    }
    public void migrate(List<Node> srcNodes, List<Node> dstNodes, double downThreshold,double upThreshold, Topo topo){
        Map<SFC, Integer> map;
        int cpuResource, memResource;
        VNF vnf1;
        List<SFC> finalSfc = new ArrayList<>();
        NetworkInspector networkInspector = new NetworkInspector(topo);
        List<VNF> vnfList;
        Map<VNF, Map<Node, List<SFC>>> tempMap = new HashMap<>();
        for (Node node : srcNodes){
            vnfList = new ArrayList<>(node.getVnfList());
            vnfList.sort(Comparator.comparingInt(vnf -> -vnf.getMemConsumed()- vnf.getCpuConsumed()));
            for (VNF vnf : vnfList){
                for (Node node1 : dstNodes){
                    if(node != node1 && underThreshold(node1, downThreshold)){
                        // 如果
                        map = dataCollector.worthMigrateSFCs(node1, vnf);
                        vnf1 = node1.getVNFByType(vnf.getVnfType());
                        if(vnf1 != null){
                            cpuResource = 0;
                            memResource = 0;
                        }else {
                            cpuResource = vnf.getDefaultCpuConsumption();
                            memResource = vnf.getDefaultMemConsumption();
                        }
                        for (SFC sfc : map.keySet()){
                            // 如果值得迁移就放过去
                            if(-map.get(sfc) >= 0.5*vnf.getMigrateCost(sfc) + 0.5*(0.5*cpuResource + 0.5*memResource)){
                                finalSfc.add(sfc);
                            } else if (downThreshold == 1.0 && map.get(sfc) <= 0) {
                                finalSfc.add(sfc);
                            } else if (downThreshold == 1.0 && map.get(sfc) + sfc.getDelay(true) < sfc.getMaxDelay()) {
                                tempMap.computeIfAbsent(vnf, k -> new HashMap<>());
                                tempMap.get(vnf).computeIfAbsent(node1, k -> new ArrayList<>());
                                tempMap.get(vnf).get(node1).add(sfc);
                            }
                        }
                        if(vnf1 == null) vnf1 = new VNF(indexOfVNF, vnf.getVnfType());
                        finalSfc.sort(Comparator.comparingInt(SFC::getFlowRate));
                        Collections.reverse(finalSfc);
                        for (SFC sfc : finalSfc){
                            // 如果值得迁移就放过去
                            if(networkInspector.addASFCtoANode(node1, vnf1, sfc)){
                                node1.addVNF(vnf1);
                                vnf1.setNode(node1);
                                vnf1.addSFC(sfc);
                                vnf.removeSFC(sfc);
                                if(!topo.getVnfs().contains(vnf1)){
                                    topo.getVnfs().add(vnf1);
                                    indexOfVNF++;
                                }
                                sfc.changeVNF(vnf, vnf1, topo);
                                sfc.updateLinks(topo);
                            }
                        }
                    }
                    finalSfc.clear();
                    // 如果这个时候已经不过载了，就先停止，因为后续还有不过载时的处理方案。
                    if((upThreshold == 1.0 && underThreshold(node, upThreshold)) || vnf.getSfcList().size() == 0)break;
                }
                if(vnf.isEmpty()){
                    node.removeVNF(vnf);
                    topo.getVnfs().remove(vnf);
                }
                if(upThreshold == 1.0 && underThreshold(node, upThreshold))break;
            }
            if (node.isOverload()){
                for (VNF vnf: tempMap.keySet()){
                    for (Node node1: tempMap.get(vnf).keySet()){
                        for(SFC sfc : tempMap.get(vnf).get(node1)){
                            vnf1 = node1.getVNFByType(vnf.getVnfType());
                            if(vnf1 == null) vnf1 = new VNF(topo.getVnfs().size(), vnf.getVnfType());
                            if(networkInspector.addASFCtoANode(node1, vnf1, sfc)){
                                node1.addVNF(vnf1);
                                vnf1.setNode(node1);
                                vnf1.addSFC(sfc);
                                vnf.removeSFCWithoutRemove(sfc);
                                if(!topo.getVnfs().contains(vnf1)){
                                    topo.getVnfs().add(vnf1);
                                    indexOfVNF++;
                                }
                                sfc.changeVNF(vnf, vnf1, topo);
                                sfc.updateLinks(topo);
                            }
                            if(!node.isOverload())break;
                        }
                        if(!node.isOverload())break;
                    }
                    if(vnf.isEmpty()){
                        node.removeVNF(vnf);
                        topo.getVnfs().remove(vnf);
                    }
                    if(!node.isOverload())break;
                }
            }
//            tempVNFList = node.getVnfList().stream().filter(VNF::isEmpty).toList();
//            tempVNFList.forEach(node::removeVNF);
//            topo.getVnfs().removeAll(tempVNFList);
            tempMap.clear();
            dstNodes = dataCollector.getNodeUnder(downThreshold);
            dstNodes.sort(Comparator.comparingInt(node1 -> -node1.getRestCpuResource() - node1.getRestMemResource()));
        }
    }
    public boolean underThreshold(Node node, double threshold){
        return node.getCpuResourceRequired() <= threshold * node.getCpuResource() &&
                node.getMemResourceRequired() <= threshold * node.getMemResource();
    }
}
