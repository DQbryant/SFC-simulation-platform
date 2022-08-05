package placement;

import components.*;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 网络监视器，能提供一些封装好的对网络操作的函数
 */
@Data
public class NetworkInspector {
    private Topo topo;
    public static int index = 1;

    public NetworkInspector(Topo topo) {
        this.topo = topo;
    }

    public NetworkInspector() {
    }

    public List<VNF> getVNFsByType(String type){
        return topo.getVnfs() == null? null : topo.getVnfs().stream().filter(vnf -> vnf.getVnfType().equals(type)).collect(Collectors.toList());
    }

    public String getNodeInfo(Node node){
        String info = node.isP4Node() ? "P4":"";
        info += "节点"+node.getNodeId()+",cpu使用情况:"+node.getUsedCpuResource()+"/"+node.getCpuResource()+"/"+node.getCpuResourceRequired()+
                ",内存使用情况:"+node.getUsedMemResource()+"/"+node.getMemResource()+"/"+node.getMemResourceRequired();
        StringBuilder add = new StringBuilder(",连接了:");
        List<Node> neighbours = node.getNeighbors();
        for (Node n: neighbours) {
            if(n.isP4Node()) add.append("P4");
            add.append("节点").append(n.getNodeId()).append(",");
        }
        add.append("拥有VNF信息:").append(node.getVnfList().size() == 0? "无" : "");
        for (VNF vnf : node.getVnfList()) {
            add.append("\n").append(getVNFInfo(vnf));
        }
        return info + add;
    }
    public String getVNFInfo(VNF vnf){
        StringBuilder stringBuilder = new StringBuilder();
        if(vnf.getNode().isP4Node())stringBuilder.append("P4-NF");
        else stringBuilder.append("VNF");
        stringBuilder.append(vnf.getVnfId()).append(",类型:").append(vnf.getVnfType()).append(",所在节点:");
        if(vnf.getNode().isP4Node()) stringBuilder.append("P4节点");
        else stringBuilder.append("节点");
        return stringBuilder.toString()+vnf.getNode().getNodeId()+",cpu资源使用:"+
                vnf.getCpuConsumed()+"/"+vnf.getProvidedCpuResource()+",内存资源占用:"+vnf.getMemConsumed()+"/"+vnf.getProvidedMemResource();
    }
    public void printNetworkInfo(){
        List<Node> nodes = topo.getNodes();
        for (Node node: nodes){
            System.out.println(getNodeInfo(node));
            System.out.println();
        }
    }
    public String getSFCInfo(SFC sfc){
        StringBuffer info = new StringBuffer();
        info.append("SFC").append(sfc.getSfcId()).append("需要VNF:");
        List<String> vnfType = sfc.getRequiredVNF();
        info.append(vnfType.get(0));
        for (int i = 1; i < vnfType.size(); i++) {
            info.append("->").append(vnfType.get(i));
        }
        info.append(", 接入点:节点").append(sfc.getAccessNodeId());
        info.append(", 流量率:").append(sfc.getFlowRate()).append(", 最大容忍时延:").append(sfc.getMaxDelay());
        if(sfc.getVnfList() != null && sfc.getVnfList().size() != 0){
            info.append(",当前sfc时延:").append(sfc.getDelay(false));
            info.append("\nVNF映射状况:");
            sfc.getVnfList().forEach(vnf -> info.append("\n").append(getVNFInfo(vnf)).append(",在该NF的处理时延:").append(vnf.getDelay(sfc, false)));
        }
        return info.toString();
    }
    public void printSFCRequestInfo(){
        topo.getSfcs().forEach(sfc -> System.out.println(getSFCInfo(sfc)));
    }

    // 该函数判断一个SFC
    public boolean addASFCtoANode(Node node, VNF vnf, SFC sfc){
        if(node.getVnfList().contains(vnf)){
            int restCpuResource = node.getCpuResource() - node.getUsedCpuResource();
            int restMemResource = node.getMemResource() - node.getUsedMemResource();
            return restCpuResource >= sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnf.getVnfType()) &&
                    restMemResource >= sfc.getFlowRate()*Constant.consumeMemRatio.get(vnf.getVnfType());
        }else {
            if(node.isP4Node() && node.getVnfList().size()!=0 && !node.hasVNF(vnf.getVnfType())) return false;
            int restCpuResource = node.getCpuResource() - node.getUsedCpuResource();
            int restMemResource = node.getMemResource() - node.getUsedMemResource();
            return restCpuResource >= vnf.getCpuConsumed() + sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnf.getVnfType()) &&
                    restMemResource >= vnf.getMemConsumed() + sfc.getFlowRate()*Constant.consumeMemRatio.get(vnf.getVnfType());
        }
    }
    public void evaluateTopo(){
        List<Node> nodes = topo.getNodes();
        double cpuAvg = 0, memAvg = 0, cpuAll = 0, memAll = 0;
        for (Node node: nodes){
            if(node.isP4Node()) System.out.print("P4");
            System.out.println("节点"+node.getNodeId()+",cpu使用:"+node.getUsedCpuResource()+"/"+node.getCpuResource()+",内存使用:"+node.getUsedMemResource()+"/"+node.getMemResource());
            cpuAvg += 1.0*node.getUsedCpuResource() / node.getCpuResource();
            memAvg += 1.0*node.getUsedMemResource() / node.getMemResource();
            cpuAll += node.getCpuResourceRequired();
            memAll += node.getMemResourceRequired();
        }
        cpuAvg /= nodes.size();
        memAvg /= nodes.size();
        cpuAll /= nodes.stream().mapToDouble(Node::getCpuResource).sum();
        memAll /= nodes.stream().mapToDouble(Node::getMemResource).sum();
        System.out.println("实际请求资源需要的cpu占用率:"+cpuAll+",内存占用率:"+memAll);
        System.out.println("平均cpu占用率:"+cpuAvg+",平均内存占用率:"+memAvg);
        double cpu=0,mem=0,cpu1, mem1;
        for (Node node: nodes){
            cpu1 = 1.0*node.getUsedCpuResource() / node.getCpuResource() - cpuAvg;
            mem1 = 1.0*node.getUsedMemResource() / node.getMemResource() - memAvg;
            cpu += cpu1*cpu1;
            mem += mem1*mem1;
        }
        cpu /= (nodes.size()-1);
        mem /= (nodes.size()-1);
        System.out.println("cpu占用率方差:"+cpu+",内存占用率方差"+mem);
    }
    public void evaluateSFC(){
        double sum = 0.0,avg = 0.0;
        int delay, delayEnough;
        for (SFC sfc : topo.getSfcs()) {
            delay = sfc.getDelay(false);
            delayEnough =sfc.getDelay(true);
            sum += 1.0*delayEnough / sfc.getMaxDelay();
            avg += 1.0*delayEnough;
            System.out.println("SFC"+sfc.getSfcId()+"时延:"+delay+"/"+sfc.getMaxDelay()+ (delay>1000? (",理论时延(资源充足):"+sfc.getDelay(true)):""));
        }
        sum /= topo.getSfcs().size();
        avg /= topo.getSfcs().size();
        System.out.println("理论(资源充足)平均时延:"+avg+",平均时延满意度:"+sum);
    }
    // 该函数并没有做节点不能放置SFC的处理，在执行该函数之前，请务必保证节点的资源足够
    public List<VNF> deploySFC(List<Node> nodes, SFC sfc){
        int accessNodeId = sfc.getAccessNodeId();
        Node node1 = nodes.get(0);
        List<Node> nodeList = topo.getNodes();
        if(node1.getNodeId() == accessNodeId){
            sfc.setAccessToVNF0(null);
        }else {
            sfc.setAccessToVNF0(VirtualLink.getLinksBetweenNode(nodeList.get(accessNodeId - 1), node1, sfc.getFlowRate()));
        }
        VNF vnf;
        List<String> requiredVNF = sfc.getRequiredVNF();
        Node node;
        String requiredType;
        for (int i = 0; i < nodes.size(); i++) {
            node = nodes.get(i);
            requiredType = requiredVNF.get(i);
            vnf = node.getVNFByType(requiredType);
            if (vnf == null) {
                vnf = new VNF(index++, requiredType);
                vnf.addSFCWithoutUpdate(sfc);
                node.addVNF(vnf);
                vnf.setNode(node);
            }else {
                vnf.addSFC(sfc);
            }
            sfc.setVNF(vnf, i);
        }
        List<VNF> vnfs = sfc.getVnfList();
        VirtualLink virtualLink;
        for (int i = 0; i < vnfs.size() - 1; i++) {
            virtualLink = VirtualLink.getVirtualLink(vnfs.get(i), vnfs.get(i + 1), sfc.getFlowRate());
            sfc.setLink(virtualLink, i);
        }
        return vnfs;
    }
}
