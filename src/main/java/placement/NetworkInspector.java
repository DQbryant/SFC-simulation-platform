package placement;

import components.*;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ��������������ṩһЩ��װ�õĶ���������ĺ���
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
        info += "�ڵ�"+node.getNodeId()+",cpuʹ�����:"+node.getUsedCpuResource()+"/"+node.getCpuResource()+"/"+node.getCpuResourceRequired()+
                ",�ڴ�ʹ�����:"+node.getUsedMemResource()+"/"+node.getMemResource()+"/"+node.getMemResourceRequired();
        StringBuilder add = new StringBuilder(",������:");
        List<Node> neighbours = node.getNeighbors();
        for (Node n: neighbours) {
            if(n.isP4Node()) add.append("P4");
            add.append("�ڵ�").append(n.getNodeId()).append(",");
        }
        add.append("ӵ��VNF��Ϣ:").append(node.getVnfList().size() == 0? "��" : "");
        for (VNF vnf : node.getVnfList()) {
            add.append("\n").append(getVNFInfo(vnf));
        }
        return info + add;
    }
    public String getVNFInfo(VNF vnf){
        StringBuilder stringBuilder = new StringBuilder();
        if(vnf.getNode().isP4Node())stringBuilder.append("P4-NF");
        else stringBuilder.append("VNF");
        stringBuilder.append(vnf.getVnfId()).append(",����:").append(vnf.getVnfType()).append(",���ڽڵ�:");
        if(vnf.getNode().isP4Node()) stringBuilder.append("P4�ڵ�");
        else stringBuilder.append("�ڵ�");
        return stringBuilder.toString()+vnf.getNode().getNodeId()+",cpu��Դʹ��:"+
                vnf.getCpuConsumed()+"/"+vnf.getProvidedCpuResource()+",�ڴ���Դռ��:"+vnf.getMemConsumed()+"/"+vnf.getProvidedMemResource();
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
        info.append("SFC").append(sfc.getSfcId()).append("��ҪVNF:");
        List<String> vnfType = sfc.getRequiredVNF();
        info.append(vnfType.get(0));
        for (int i = 1; i < vnfType.size(); i++) {
            info.append("->").append(vnfType.get(i));
        }
        info.append(", �����:�ڵ�").append(sfc.getAccessNodeId());
        info.append(", ������:").append(sfc.getFlowRate()).append(", �������ʱ��:").append(sfc.getMaxDelay());
        if(sfc.getVnfList() != null && sfc.getVnfList().size() != 0){
            info.append(",��ǰsfcʱ��:").append(sfc.getDelay(false));
            info.append("\nVNFӳ��״��:");
            sfc.getVnfList().forEach(vnf -> info.append("\n").append(getVNFInfo(vnf)).append(",�ڸ�NF�Ĵ���ʱ��:").append(vnf.getDelay(sfc, false)));
        }
        return info.toString();
    }
    public void printSFCRequestInfo(){
        topo.getSfcs().forEach(sfc -> System.out.println(getSFCInfo(sfc)));
    }

    // �ú����ж�һ��SFC
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
            System.out.println("�ڵ�"+node.getNodeId()+",cpuʹ��:"+node.getUsedCpuResource()+"/"+node.getCpuResource()+",�ڴ�ʹ��:"+node.getUsedMemResource()+"/"+node.getMemResource());
            cpuAvg += 1.0*node.getUsedCpuResource() / node.getCpuResource();
            memAvg += 1.0*node.getUsedMemResource() / node.getMemResource();
            cpuAll += node.getCpuResourceRequired();
            memAll += node.getMemResourceRequired();
        }
        cpuAvg /= nodes.size();
        memAvg /= nodes.size();
        cpuAll /= nodes.stream().mapToDouble(Node::getCpuResource).sum();
        memAll /= nodes.stream().mapToDouble(Node::getMemResource).sum();
        System.out.println("ʵ��������Դ��Ҫ��cpuռ����:"+cpuAll+",�ڴ�ռ����:"+memAll);
        System.out.println("ƽ��cpuռ����:"+cpuAvg+",ƽ���ڴ�ռ����:"+memAvg);
        double cpu=0,mem=0,cpu1, mem1;
        for (Node node: nodes){
            cpu1 = 1.0*node.getUsedCpuResource() / node.getCpuResource() - cpuAvg;
            mem1 = 1.0*node.getUsedMemResource() / node.getMemResource() - memAvg;
            cpu += cpu1*cpu1;
            mem += mem1*mem1;
        }
        cpu /= (nodes.size()-1);
        mem /= (nodes.size()-1);
        System.out.println("cpuռ���ʷ���:"+cpu+",�ڴ�ռ���ʷ���"+mem);
    }
    public void evaluateSFC(){
        double sum = 0.0,avg = 0.0;
        int delay, delayEnough;
        for (SFC sfc : topo.getSfcs()) {
            delay = sfc.getDelay(false);
            delayEnough =sfc.getDelay(true);
            sum += 1.0*delayEnough / sfc.getMaxDelay();
            avg += 1.0*delayEnough;
            System.out.println("SFC"+sfc.getSfcId()+"ʱ��:"+delay+"/"+sfc.getMaxDelay()+ (delay>1000? (",����ʱ��(��Դ����):"+sfc.getDelay(true)):""));
        }
        sum /= topo.getSfcs().size();
        avg /= topo.getSfcs().size();
        System.out.println("����(��Դ����)ƽ��ʱ��:"+avg+",ƽ��ʱ�������:"+sum);
    }
    // �ú�����û�����ڵ㲻�ܷ���SFC�Ĵ�����ִ�иú���֮ǰ������ر�֤�ڵ����Դ�㹻
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
