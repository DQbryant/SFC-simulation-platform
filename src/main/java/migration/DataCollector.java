package migration;

import components.*;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class DataCollector {
    private Topo topo;

    public DataCollector(Topo topo) {
        this.topo = topo;
    }

    public double getCpuLoad(){
        double sumCpuUsed = 0, sumCpu = 0;
        List<Node> nodes = topo.getNodes();
        for (Node node: nodes){
            sumCpuUsed += node.getCpuResourceRequired();
            sumCpu += node.getCpuResource();
        }
        return sumCpuUsed / sumCpu;
    }
    public double getMemLoad(){
        double  sumMemUsed = 0, sumMem = 0;
        List<Node> nodes = topo.getNodes();
        for (Node node: nodes){
            sumMemUsed += node.getMemResourceRequired();
            sumMem += node.getMemResource();
        }
        return sumMemUsed / sumMem;
    }
    public List<Node> getOverloadedNodes(){
        return topo.getNodes().stream().filter(Node::isOverload).collect(Collectors.toList());
    }
    public List<Node> getOverloadedNodes(double threshold){
        return topo.getNodes().stream().filter(node -> node.getCpuResourceRequired() >= threshold * node.getCpuResource() ||
                node.getMemResourceRequired() >= threshold * node.getMemResource()).collect(Collectors.toList());
    }
    public List<Node> getOverloadedNodesUnder(double threshold){
        return topo.getNodes().stream().filter(node -> node.isOverload() && node.getCpuResourceRequired() < threshold * node.getCpuResource() &&
                node.getMemResourceRequired() < threshold * node.getMemResource()).collect(Collectors.toList());
    }
    public List<Node> getNotOverloadedNodes(){
        return topo.getNodes().stream().filter(node -> !node.isOverload()).collect(Collectors.toList());
    }
    public List<Node> getNodeUnder(double threshold){
        return topo.getNodes().stream().filter(node -> node.getUsedCpuResource() <= threshold * node.getCpuResource() &&
                node.getUsedMemResource() <= threshold * node.getMemResource()).collect(Collectors.toList());
    }
    public List<Node> getNodeUpper(double threshold){
        return topo.getNodes().stream().filter(node -> node.getUsedCpuResource() > threshold * node.getCpuResource() ||
                node.getUsedMemResource() > threshold * node.getMemResource()).collect(Collectors.toList());
    }
    public boolean nodeHostsVNF(Node node, VNF vnf){
        int cpuRequired = vnf.getCpuConsumed();
        int memRequired = vnf.getMemConsumed();
        if(node.hasVNF(vnf.getVnfType())){
            cpuRequired -= vnf.getDefaultCpuConsumption();
            memRequired -= vnf.getDefaultMemConsumption();
        }else if(node.isP4Node() && node.getVnfList().size() != 0)return false;
        return node.getRestCpuResource() >= cpuRequired && node.getRestMemResource() >= memRequired;
    }
    public Map<SFC, Integer> worthMigrateSFCs(Node node,VNF vnf){
        return vnf.getSfcList().stream().collect(Collectors.toMap(sfc -> sfc,sfc -> delayIfChangeVNF(sfc, vnf, node) - sfc.getDelay(true)));
    }
    public void addVNFToNode(Node node, VNF vnf){
        VNF vnf1 = node.getVNFByType(vnf.getVnfType());
        if(vnf1 != null){
//            System.out.println(vnf.getVnfId() + " " + vnf1.getVnfId()+" "+node.getNodeId());
            VNF.merge(vnf1, vnf, topo);
        }else {
            node.addVNF(vnf);
            vnf.setNode(node);
            vnf1 = vnf;
        }
        vnf1.getSfcList().forEach(sfc -> sfc.updateLinks(topo));
    }
    public List<VNF> getCompleteVNFs(Node node){
        int overLoadCpu = node.getCpuResourceRequired() - node.getCpuResource();
        int overLoadMem = node.getMemResourceRequired() - node.getMemResource();
        return node.getVnfList().stream().filter(vnf -> vnf.getCpuConsumed() >= overLoadCpu && vnf.getMemConsumed() >= overLoadMem).collect(Collectors.toList());
    }
    //这里简单地计算总量，后续可以改
    public VNF getMaxVNF(Node node){
        return node.getVnfList().stream().max(Comparator.comparingInt(vnf -> vnf.getCpuConsumed()+ vnf.getMemConsumed())).get();
    }
    //表示这个VNF迁移到node会不会导致SFC超时
    public boolean overDelay(VNF vnf, Node node){
        List<SFC> sfcs = vnf.getSfcList();
        for (SFC sfc: sfcs){
            if(delayIfChangeVNF(sfc, vnf ,node) > sfc.getMaxDelay()) return true;
        }
        return false;
    }
    public int delayIfChangeVNF(SFC sfc, VNF vnf, Node node){
        List<VNF> vnfs;
        int delay, i ,flowRate;
        List<Link> leftLinks, rightLinks = null;
        VirtualLink leftLink, rightLink;
        delay = sfc.getDelay(true);
        vnfs = sfc.getVnfList();
        flowRate = sfc.getFlowRate();
        for (i = 0; i < vnfs.size(); i++) {
            if(vnfs.get(i) == vnf)break;
        }
        if(i == 0){
            leftLinks = sfc.getAccessToVNF0() == null ? new ArrayList<>() : sfc.getAccessToVNF0();
            rightLink = sfc.getLinkList().get(i);
            rightLinks = rightLink == null? new ArrayList<>() : rightLink.getUsedLinks();
            delay -= leftLinks.stream().mapToInt(Link::getDelay).sum();
            delay -= rightLinks.stream().mapToInt(Link::getDelay).sum();
            leftLinks = VirtualLink.getLinksBetweenNode(topo.getNodes().get(sfc.getAccessNodeId() - 1), node, flowRate);
            rightLinks = VirtualLink.getLinksBetweenNode(node, vnfs.get(1).getNode(), flowRate);
        }else if(i == vnfs.size()-1){
            leftLink = sfc.getLinkList().get(i - 1);
            leftLinks = leftLink == null ? new ArrayList<>() : leftLink.getUsedLinks();
            delay -= leftLinks.stream().mapToInt(Link::getDelay).sum();
            leftLinks = VirtualLink.getLinksBetweenNode(vnfs.get(i-1).getNode(), node, flowRate);
        }else {
            leftLink = sfc.getLinkList().get(i-1);
            leftLinks = leftLink == null ? new ArrayList<>(): leftLink.getUsedLinks();
            rightLink = sfc.getLinkList().get(i);
            rightLinks = rightLink == null ? new ArrayList<>() : rightLink.getUsedLinks();
            delay -= leftLinks.stream().mapToInt(Link::getDelay).sum();
            delay -= rightLinks.stream().mapToInt(Link::getDelay).sum();
            leftLinks = VirtualLink.getLinksBetweenNode(vnfs.get(i-1).getNode(), node, flowRate);
            rightLinks = VirtualLink.getLinksBetweenNode(node, vnfs.get(i+1).getNode(),flowRate);
        }
        if(leftLinks == null){
            leftLinks = new ArrayList<>();
        }
        if(rightLinks == null){
            rightLinks = new ArrayList<>();
        }
        delay += leftLinks.stream().mapToInt(Link::getDelay).sum();
        delay += rightLinks.stream().mapToInt(Link::getDelay).sum();
        return delay;
    }

    public boolean worthMerge(VNF srcVNF, VNF dstVNF) {
        Node dstNode = dstVNF.getNode();
        if (!nodeHostsVNF(dstNode, srcVNF)) return false;
        if(!overDelay(srcVNF, dstNode)){
            List<SFC> sfcList = srcVNF.getSfcList();
            double migrationCost = srcVNF.getMigrateCost();
            double delayChange = sfcList.stream().mapToDouble(sfc -> delayIfChangeVNF(sfc, srcVNF, dstNode) - sfc.getDelay(false)).sum();
            return srcVNF.getDefaultCpuConsumption() + srcVNF.getDefaultMemConsumption() >= 0.5*migrationCost + delayChange;
        }
        return false;
    }
}
