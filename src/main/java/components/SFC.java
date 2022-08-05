package components;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class SFC {
    private int sfcId;
    private int maxDelay;
    private int flowRate;
    private int accessNodeId;
    private List<Link> accessToVNF0;
    private List<String> requiredVNF;
    private List<VNF> vnfList;
    private List<VirtualLink> linkList;

    public SFC(int sfcId, int maxDelay, int flowRate, int accessNodeId, List<String> requiredVNF) {
        this.sfcId = sfcId;
        this.maxDelay = maxDelay;
        this.flowRate = flowRate;
        this.accessNodeId = accessNodeId;
        this.requiredVNF = requiredVNF;
    }

    public SFC(int sfcId, int maxDelay, int flowRate, List<String> requiredVNF) {
        this.sfcId = sfcId;
        this.maxDelay = maxDelay;
        this.flowRate = flowRate;
        this.requiredVNF = requiredVNF;
    }

    public SFC(int sfcId, int maxDelay, int flowRate) {
        this.sfcId = sfcId;
        this.maxDelay = maxDelay;
        this.flowRate = flowRate;
    }

    public SFC(int sfcId) {
        this.sfcId = sfcId;
        requiredVNF = new ArrayList<>();
        vnfList = new ArrayList<>();
        linkList = new ArrayList<>();
    }

    public SFC() {

    }

    public void setVNF(VNF vnf,int index){
        if(vnfList == null){
            vnfList = new ArrayList<>();
        }
        if(index >= vnfList.size()){
            vnfList.add(vnf);
            return;
        }
        VNF vnf1 = vnfList.get(index);
        if(vnf1 != null) {
            vnf1.removeSFC(this);
        }
        vnfList.set(index,vnf);
    }
    public int getDelay(boolean enough){
        int delay = 0;
        for (VNF vnf : vnfList) {
            delay += vnf.getDelay(this, enough);
        }
        for (VirtualLink vl: linkList) {
            delay += vl==null ? 0 : vl.getDelay();
        }
        if(accessToVNF0 != null && accessToVNF0.size() != 0){
            delay += accessToVNF0.stream().mapToInt(Link::getDelay).sum();
        }
        return delay;
    }
    public void setLink(VirtualLink link,int index){
        if(linkList == null){
            linkList = new ArrayList<>();
        }
        if(index >= linkList.size()){
            linkList.add(link);
            return;
        }
        linkList.set(index,link);
    }

    public void changeVNF(VNF srcVNF, VNF dstVNF, Topo topo) {
        for (int i = 0; i < vnfList.size(); i++) {
            if(vnfList.get(i) == srcVNF){
                vnfList.set(i, dstVNF);
                if(i == 0){
                    accessToVNF0 = VirtualLink.getLinksBetweenNode(topo.getNodes().get(accessNodeId - 1), dstVNF.getNode(), flowRate);
                }
                if(i > 0){
                    linkList.set(i-1 , VirtualLink.getVirtualLink(vnfList.get(i - 1), dstVNF, flowRate));
                }
                if(i < vnfList.size() - 1){
                    linkList.set(i, VirtualLink.getVirtualLink(dstVNF, vnfList.get(i + 1), flowRate));
                }
                break;
            }
        }

    }
    public boolean changeAccessPoint(){
        if(vnfList.get(0).getNode().getNodeId() == accessNodeId){
            accessToVNF0 = new ArrayList<>();
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "SFC{" +
                "sfcId=" + sfcId +
                ", maxDelay=" + maxDelay +
                ", flowRate=" + flowRate +
                ", accessNodeId=" + accessNodeId +
                ", requiredVNF=" + requiredVNF +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SFC sfc = (SFC) o;
        return sfcId == sfc.sfcId && maxDelay == sfc.maxDelay && flowRate == sfc.flowRate && requiredVNF.equals(sfc.requiredVNF);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sfcId, maxDelay, flowRate, requiredVNF);
    }

    public void update(int flowRate) {
        vnfList.forEach(vnf -> vnf.update(this, flowRate));
        this.flowRate = flowRate;
    }
    public void updateLinks(Topo topo){
        if(vnfList.get(0).getNode().getNodeId() == accessNodeId){
            setAccessToVNF0(null);
        }else {
            setAccessToVNF0(VirtualLink.getLinksBetweenNode(topo.getNodes().get(accessNodeId - 1), vnfList.get(0).getNode(), flowRate));
        }
        VirtualLink virtualLink;
        for (int i = 0; i < vnfList.size() - 1; i++) {
            virtualLink = VirtualLink.getVirtualLink(vnfList.get(i), vnfList.get(i + 1), flowRate);
            this.setLink(virtualLink, i);
        }
    }
}
