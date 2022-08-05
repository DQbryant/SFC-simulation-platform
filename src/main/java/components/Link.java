package components;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class Link {
    private int linkId;
    private int bandwidth;
    private Node node1;
    private Node node2;
    private int delay;
    private int usedbandwidth;
    private List<VirtualLink> virtualLinks;
    public boolean addVirtualLink(VirtualLink virtualLink){
        if(!virtualLinks.contains(virtualLink)){
            virtualLinks.add(virtualLink);
            usedbandwidth += virtualLink.getRequiredBandwidth();
            return true;
        }
        return false;
    }
    public boolean removeVirtualLink(VirtualLink virtualLink){
        if(!virtualLinks.contains(virtualLink)){
            virtualLinks.remove(virtualLink);
            usedbandwidth -= virtualLink.getRequiredBandwidth();
            return true;
        }
        return false;
    }
    public boolean connects(Node node1, Node node2){
        return (this.node1 == node1 && this.node2 == node2) || (this.node1 == node2 && this.node2 == node1);
    }
    public Link(int bandwidth, int delay) {
        this.bandwidth = bandwidth;
        this.delay = delay;
    }

    public Link(int linkId, int bandwidth, Node node1, Node node2, int delay) {
        this.linkId = linkId;
        this.bandwidth = bandwidth;
        this.node1 = node1;
        this.node2 = node2;
        this.delay = delay;
        usedbandwidth = 0;
        virtualLinks = new ArrayList<>();
    }

    public Node getAnotherNode(Node node) {
        if(node == node1) return node2;
        if(node == node2) return node1;
        return null;
    }
    public boolean holdFlow(int flowRate){
        return bandwidth - usedbandwidth >= flowRate;
    }

    @Override
    public String toString() {
        return "Link{" +
                "linkId=" + linkId +
                ", bandwidth=" + bandwidth +
                ", delay=" + delay +
                ", usedbandwidth=" + usedbandwidth +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return linkId == link.linkId && bandwidth == link.bandwidth && delay == link.delay && usedbandwidth == link.usedbandwidth && node1.equals(link.node1) && node2.equals(link.node2) && Objects.equals(virtualLinks, link.virtualLinks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkId, bandwidth, node1, node2, delay, usedbandwidth);
    }
}
