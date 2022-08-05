package components;

import lombok.Data;

import java.util.*;
import java.util.stream.Stream;

@Data
public class VirtualLink {
    private int virtualLinkId;
    private VNF srcVNF;
    private VNF dstVNF;
    private List<Link> usedLinks;
    private int requiredBandwidth;

    @Override
    public String toString() {
        return "VirtualLink{" +
                "virtualLinkId=" + virtualLinkId +
                ", srcVNF=" + srcVNF +
                ", dstVNF=" + dstVNF +
                ", requiredBandwidth=" + requiredBandwidth +
                '}';
    }

    public VirtualLink(VNF srcVNF, VNF dstVNF, List<Link> usedLinks, int requiredBandwidth) {
        this.srcVNF = srcVNF;
        this.dstVNF = dstVNF;
        this.usedLinks = usedLinks;
        this.requiredBandwidth = requiredBandwidth;
    }


    public int getDelay(){
        int delay = 0;
        for (Link link: usedLinks) {
            delay += link == null ? 0 : link.getDelay();
        }
        return delay;
    }
    // 通过广度优先搜索查找路径
    public static VirtualLink getVirtualLink(VNF vnf1, VNF vnf2, int flowRate){
        if(vnf1.getNode() == vnf2.getNode()) return null;
        Node srcNode = vnf1.getNode();
        Node dstNode = vnf2.getNode();
        Node tempNode = srcNode;
        int index = 0;
        List<Node> nodeQueue = new LinkedList<>();
//        List<Link> linkQueue = new LinkedList<>();
        List<Node> nodes;
        HashMap<Node, Link> result = new HashMap<>();
        while(tempNode != dstNode){
            nodes = tempNode.getNeighbors();
            for (int i = 0; i < nodes.size(); i++) {
                if(result.get(nodes.get(i)) == null && tempNode.getPorts().get(i).holdFlow(flowRate)){
                    nodeQueue.add(nodes.get(i));
//                    linkQueue.add(tempNode.getPorts().get(i));
                    result.put(nodes.get(i), tempNode.getPorts().get(i));
                }
            }
            tempNode = nodeQueue.get(index++);
        }
        List<Link> resultRoad = new ArrayList<>();
        Link link;
        while(tempNode != srcNode){
            link = result.get(tempNode);
            resultRoad.add(link);
            tempNode = link.getAnotherNode(tempNode);
        }
        Collections.reverse(resultRoad);
        return new VirtualLink(vnf1, vnf2, resultRoad, flowRate);
    }
    public static List<Link> getLinksBetweenNode(Node srcNode, Node dstNode, int flowRate){
        if(srcNode == null || dstNode == null)return null;
        Node tempNode = srcNode;
        int index = 0;
        List<Node> nodeQueue = new LinkedList<>();
//        List<Link> linkQueue = new LinkedList<>();
        List<Node> nodes;
        HashMap<Node, Link> result = new HashMap<>();
        while(tempNode != dstNode){
            nodes = tempNode.getNeighbors();
            for (int i = 0; i < nodes.size(); i++) {
                if(result.get(nodes.get(i)) == null && tempNode.getPorts().get(i).holdFlow(flowRate)){
                    nodeQueue.add(nodes.get(i));
//                    linkQueue.add(tempNode.getPorts().get(i));
                    result.put(nodes.get(i), tempNode.getPorts().get(i));
                }
            }
            tempNode = nodeQueue.get(index++);
        }
        List<Link> resultRoad = new ArrayList<>();
        Link link;
        while(tempNode != srcNode){
            link = result.get(tempNode);
            resultRoad.add(link);
            tempNode = link.getAnotherNode(tempNode);
        }
        Collections.reverse(resultRoad);
        return resultRoad;
    }
}
