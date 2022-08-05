package components;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class Node {
    private int nodeId;
    private int cpuResource;
    private int memResource;
    private List<Link> ports;
    private int usedCpuResource;
    private int usedMemResource;
    private List<VNF> vnfList;
    private boolean isP4Node;
    public boolean addVNF(VNF vnf){
        if(!vnfList.contains(vnf)){
            if(vnf.getDefaultCpuConsumption() > cpuResource - usedCpuResource || vnf.getDefaultMemConsumption() > memResource - usedMemResource) return false;
            vnf.setProvidedCpuResource(Math.min(vnf.getCpuConsumed(), getRestCpuResource()));
            vnf.setProvidedMemResource(Math.min(vnf.getMemConsumed(), getRestMemResource()));
            vnfList.add(vnf);
            usedCpuResource += vnf.getProvidedCpuResource();
            usedMemResource += vnf.getProvidedMemResource();
            return true;
        }
        return false;
    }

    public Node() {
    }

    public boolean removeVNF(VNF vnf){
        if(vnfList.contains(vnf)){
//            System.out.println(1);
            vnfList.remove(vnf);
            usedCpuResource -= vnf.getProvidedCpuResource();
            usedMemResource -= vnf.getProvidedMemResource();
            List<VNF> vnfList1 = new ArrayList<>(vnfList);
            for (VNF vnf1 : vnfList1){
                if(vnf1.getProvidedCpuResource() < vnf1.getCpuConsumed()|| vnf1.getProvidedMemResource() < vnf1.getCpuConsumed()){
                    update(vnf1);
                }
            }
            return true;
        }
        return false;
    }
    public boolean hasVNF(String type){
        return vnfList.stream().filter(vnf -> vnf.getVnfType().equals(type)).count() == 1;
    }
    public int getCpuResourceRequired(){
        return vnfList.stream().mapToInt(VNF::getCpuConsumed).sum();
    }
    public int getMemResourceRequired(){
        return vnfList.stream().mapToInt(VNF::getMemConsumed).sum();
    }
    public boolean isOverload(){
        int needCpuResource = vnfList.stream().mapToInt(VNF::getCpuConsumed).sum();
        int needMemResource = vnfList.stream().mapToInt(VNF::getMemConsumed).sum();
        return cpuResource < needCpuResource || memResource < needMemResource;
    }
    public VNF getVNFByType(String type){
        for (VNF vnf :vnfList) {
            if(vnf.getVnfType().equals(type)){
                return vnf;
            }
        }
        return null;
    }

    public boolean addLink(Link link) {
        return ports.add(link);
    }
    public boolean removeLink(Link link){
        return ports.remove(link);
    }

    public Node(int nodeId, int cpuResource, int memResource) {
        this.nodeId = nodeId;
        this.cpuResource = cpuResource;
        this.memResource = memResource;
        ports = new ArrayList<>();
        usedCpuResource = 0;
        usedMemResource = 0;
        vnfList = new ArrayList<>();
        isP4Node = false;
    }
    public Node(int nodeId, int cpuResource, int memResource, boolean isP4Node) {
        this.nodeId = nodeId;
        this.cpuResource = cpuResource;
        this.memResource = memResource;
        ports = new ArrayList<>();
        usedCpuResource = 0;
        usedMemResource = 0;
        vnfList = new ArrayList<>();
        this.isP4Node = isP4Node;
    }
    public List<Node> getNeighbors(){
        List<Node> nodes = new ArrayList<>();
        for(Link link : getPorts()){
            nodes.add(link.getAnotherNode(this));
        }
        return nodes;
    }
    public int getRestCpuResource(){
        return cpuResource - usedCpuResource;
    }
    public int getRestMemResource(){
        return memResource - usedMemResource;
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeId=" + nodeId +
                ", cpuResource=" + cpuResource +
                ", memResource=" + memResource +
                ", usedCpuResource=" + usedCpuResource +
                ", usedMemResource=" + usedMemResource +
                ", isP4Node=" + isP4Node +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return nodeId == node.nodeId && cpuResource == node.cpuResource && memResource == node.memResource && usedCpuResource == node.usedCpuResource && usedMemResource == node.usedMemResource && isP4Node == node.isP4Node;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, cpuResource, memResource, usedCpuResource, usedMemResource, isP4Node);
    }

    public void update(VNF vnf) {
        usedCpuResource -= vnf.getProvidedCpuResource();
        usedMemResource -= vnf.getProvidedMemResource();
        vnf.setProvidedCpuResource(Math.min(vnf.getCpuConsumed(), getRestCpuResource()));
        vnf.setProvidedMemResource(Math.min(vnf.getMemConsumed(), getRestMemResource()));
        usedCpuResource += vnf.getProvidedCpuResource();
        usedMemResource += vnf.getProvidedMemResource();
    }
}
