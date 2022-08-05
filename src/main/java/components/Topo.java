package components;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Topo {
    private List<Node> nodes;
    private List<Link> links;
    private List<VNF> vnfs;
    private List<SFC> sfcs;
    private int index;
    public Topo() {
        nodes = new ArrayList<>();
        links = new ArrayList<>();
        vnfs = new ArrayList<>();
    }

    public Topo(List<Node> nodes, List<Link> links) {
        this.nodes = nodes;
        this.links = links;
        vnfs = new ArrayList<>();
    }

    public void addNode(Node node){
        nodes.add(node);
    }
    public void removeNode(Node node){
        nodes.remove(node);
    }
    public void addLink(Node node1, Node node2, int bandwidth, int delay){
        Link link = new Link(bandwidth, delay);
        link.setNode1(node1);
        link.setNode2(node2);
        node1.addLink(link);
        node2.addLink(link);
        links.add(link);
    }
    public boolean removeLinkIfExists(Node node1, Node node2){
        for (Link link:links) {
            if(link.connects(node1, node2)){
                links.remove(link);
                return true;
            }
        }
        return false;
    }
    public boolean addVNF(int index, VNF vnf){
        vnfs.add(vnf);
        return nodes.get(index).addVNF(vnf);
    }
    public boolean removeVNF(int index,VNF vnf){
        vnfs.remove(vnf);
        return nodes.get(index).removeVNF(vnf);
    }

    @Override
    public String toString() {
        return "Topo{" +
                "nodes=" + nodes +
                ", links=" + links +
                ", vnfs=" + vnfs +
                ", sfcs=" + sfcs +
                '}';
    }
    public void computeIndex(){
        if(vnfs == null || vnfs.size() == 0) {
            index = 1;
        }else {
            index = vnfs.stream().mapToInt(VNF::getVnfId).max().getAsInt() + 1;
        }
    }
    public int getIndex(){
        return index;
    }
}