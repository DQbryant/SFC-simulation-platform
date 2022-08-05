package components;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VNF {
    public static int defaultDelay = 20;
    public static int flowRateThreshold = 20;
    private int vnfId;
    private String vnfType;
    private List<SFC> sfcList;
    private Node node;
    private int defaultCpuConsumption;
    private int defaultMemConsumption;
    private int cpuConsumed;
    private int memConsumed;
    private int providedCpuResource;
    private int providedMemResource;

    public VNF(int vnfId, String type) {
        this.vnfId = vnfId;
        sfcList = new ArrayList<>();
        vnfType = type;
        defaultCpuConsumption = Constant.defaultCpuConsumption.get(type);
        cpuConsumed = defaultCpuConsumption;
        defaultMemConsumption = Constant.defaultMemConsumption.get(type);
        memConsumed = defaultMemConsumption;
    }
    public boolean removeSFC(SFC sfc){
        boolean flag = sfcList.remove(sfc);
        node.removeVNF(this);
        if(flag){
            cpuConsumed -= sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
            memConsumed -= sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
        }
        node.addVNF(this);
        return flag;
    }
    public boolean removeSFCWithoutRemove(SFC sfc){
        boolean flag = sfcList.remove(sfc);
        node.setUsedCpuResource(node.getUsedCpuResource() - getProvidedCpuResource());
        node.setUsedMemResource(node.getUsedMemResource() - getProvidedMemResource());
        if(flag){
            cpuConsumed -= sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
            memConsumed -= sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
        }
        setProvidedCpuResource(Math.min(getCpuConsumed(), node.getRestCpuResource()));
        setProvidedMemResource(Math.min(getMemConsumed(), node.getRestMemResource()));
        node.setUsedMemResource(node.getUsedCpuResource() + getProvidedCpuResource());
        node.setUsedMemResource(node.getUsedMemResource() + getProvidedMemResource());
        return flag;
    }
    public void update(SFC sfc,int newFlowRate){
        if(sfcList.contains(sfc)){
            cpuConsumed -= sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
            memConsumed -= sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
            cpuConsumed += newFlowRate*Constant.consumeCpuRatio.get(vnfType);
            memConsumed += newFlowRate*Constant.consumeCpuRatio.get(vnfType);
            node.update(this);
        }
    }
    public boolean addSFC(SFC sfc){
        if (sfcList.contains(sfc)) return false;
        sfcList.add(sfc);
        node.removeVNF(this);
        cpuConsumed += sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
        memConsumed += sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
        node.addVNF(this);
        return true;
    }
    public boolean addSFCWithoutUpdate(SFC sfc){
        if (sfcList.contains(sfc)) return false;
        sfcList.add(sfc);
        cpuConsumed += sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
        memConsumed += sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
        return true;
    }
    public boolean removeSFCWithoutUpdate(SFC sfc){
        if (!sfcList.contains(sfc)) return false;
        sfcList.remove(sfc);
        cpuConsumed -= sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
        memConsumed -= sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
        return true;
    }


    public int getRequiredCpuResource(SFC sfc){
        return sfc.getFlowRate()*Constant.consumeCpuRatio.get(vnfType);
    }
    public int getRequiredMemResource(SFC sfc){
        return sfc.getFlowRate()*Constant.consumeMemRatio.get(vnfType);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public int getDelay(SFC sfc, boolean enough) {
        if(enough){
            if(node.isP4Node()) return defaultDelay / 10;
            if(sfc.getFlowRate() <= flowRateThreshold){
                return defaultDelay;
            } else return Constant.delayRatio.get(vnfType) * sfc.getFlowRate();
        }
        if(providedCpuResource == cpuConsumed && providedMemResource == memConsumed){
            if(node.isP4Node()) return defaultDelay / 10;
            if(sfc.getFlowRate() <= flowRateThreshold){
                return defaultDelay;
            } else return Constant.delayRatio.get(vnfType) * sfc.getFlowRate();
        }else {
            int restCpuResource = providedCpuResource - defaultCpuConsumption;
            int restMemResource = providedMemResource - defaultMemConsumption;
            for(SFC sfc1 : sfcList){
                if(sfc1 == sfc){
                    if(restCpuResource >= getRequiredCpuResource(sfc) && restMemResource >= getRequiredMemResource(sfc)){
                        if(node.isP4Node()) return defaultDelay / 10;
                        if(sfc.getFlowRate() <= flowRateThreshold){
                            return defaultDelay;
                        } else return Constant.delayRatio.get(vnfType) * sfc.getFlowRate();
                    }else return 999;
                }else {
                    restCpuResource -= getRequiredCpuResource(sfc1);
                    restMemResource -= getRequiredMemResource(sfc1);
                }
            }
            return 0;
        }
    }

    public static boolean merge(VNF dstVNF, VNF srcVNF, Topo topo){
        Node node = dstVNF.getNode();
        int restCpuResource = node.getCpuResource() - node.getUsedCpuResource();
        int restMemResource = node.getMemResource() - node.getUsedMemResource();
        if(restCpuResource >= srcVNF.getCpuConsumed() - srcVNF.getDefaultCpuConsumption() &&
                restMemResource >= srcVNF.getMemConsumed() - srcVNF.getDefaultMemConsumption()){        // 因为需要的资源已经足够了，所以根本不需要考虑迁移后会不会出现sfc资源不够的问题
            srcVNF.getSfcList().forEach(dstVNF::addSFC);
            srcVNF.getSfcList().forEach(sfc -> sfc.changeVNF(srcVNF ,dstVNF, topo));       // change函数还没有具体实现，这里假设已经实现了,效果是把vnf1换成vnf2
            return true;
        }
        return false;
    }
    @Override
    public String toString() {
        return "VNF{" +
                "vnfId=" + vnfId +
                ", vnfType='" + vnfType + '\'' +
                ", sfcList=" + sfcList +
                ", node=" + node +
                ", defaultCpuConsumption=" + defaultCpuConsumption +
                ", defaultMemConsumption=" + defaultMemConsumption +
                ", cpuConsumed=" + cpuConsumed +
                ", memConsumed=" + memConsumed +
                ", providedCpuResource=" + providedCpuResource +
                ", providedMemResource=" + providedMemResource +
                '}';
    }



    public double getMigrateCost(){
        return sfcList.stream().mapToDouble(sfc -> Constant.migrateCostRatio.get(vnfType) * sfc.getFlowRate()).sum();
    }
    public double getMigrateCost(SFC sfc){
        return sfc.getFlowRate() * Constant.migrateCostRatio.get(vnfType);
    }

    public boolean isEmpty() {
        return defaultCpuConsumption == cpuConsumed && defaultMemConsumption == memConsumed;
    }
}
