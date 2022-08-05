package placement;

import com.sun.source.tree.NewArrayTree;
import components.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 先不考虑过载的情况，只考虑正常的部署
 */
public class PlacementMaker {
    public List<VNF> makePlacement(Topo topo, List<SFC> requests, int algorithmNum) {
        switch (algorithmNum){
            case 1 : {
                AlgorithmGA algorithmGA = new AlgorithmGA(topo);
                return algorithmGA.placementByGA(requests, 30, 50);
            }
            case 2 : return placementByGreedy(topo, requests);
            default : return null;
//            default: return placementByGA(topo, requests, 20, 30);
        }

    }

    public List<VNF> placementByGreedy(Topo topo, List<SFC> requests){
        NetworkInspector networkInspector = new NetworkInspector(topo);
        int index = 1;
        List<VNF> vnfList = new ArrayList<>();
        List<Node> nodes = topo.getNodes();
        VNF vnf;
        List<VNF> tempVNFs;
        VirtualLink virtualLink;
        for (SFC sfc : requests) {
            for (int i = 0; i < sfc.getRequiredVNF().size(); i++) {
                String vnfType = sfc.getRequiredVNF().get(i);
                tempVNFs = networkInspector.getVNFsByType(vnfType);
                // 如果网络中没有这种类型的VNF，则需要新建
                if (tempVNFs == null || tempVNFs.size() == 0) {
                    vnf = new VNF(index++, vnfType);
                    for (Node node : nodes) {
                        if (networkInspector.addASFCtoANode(node, vnf, sfc)) {
                            vnf.addSFCWithoutUpdate(sfc);
                            node.addVNF(vnf);
                            vnf.setNode(node);
                            sfc.setVNF(vnf, i);
                            break;
                        }
                    }
                } else {     //如果网络中存在这种类型的VNF
                    boolean flag = false;
                    for (VNF vnf1 : tempVNFs) {
                        if (networkInspector.addASFCtoANode(vnf1.getNode(), vnf1, sfc)) {
                            vnf1.addSFC(sfc);
                            sfc.setVNF(vnf1, i);
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        vnf = new VNF(index++, vnfType);
                        for (Node node : nodes) {
                            if (networkInspector.addASFCtoANode(node, vnf, sfc)) {
                                vnf.addSFCWithoutUpdate(sfc);
                                node.addVNF(vnf);
                                vnf.setNode(node);
                                sfc.setVNF(vnf, i);
                                break;
                            }
                        }
                    }
                }
                // 这个时候sfc需要的vnf已经确定完了，接下来就是把这些vnf连接需要的虚拟链路确定
                List<VNF> vnfs = sfc.getVnfList();
                for (i = 0; i < vnfs.size() - 1; i++) {
                    virtualLink = VirtualLink.getVirtualLink(vnfs.get(i), vnfs.get(i + 1), sfc.getFlowRate());
                    sfc.setLink(virtualLink, i);
                }
            }
        }
        return vnfList;
    }

    /**
     * 使用遗传算法计算placement的解,需要设置遗传算法的代数,
     * computeDelay需要优化，计算过的链路需要存下来，这样可以节约计算延时的时间，但是这个部分等项目先做完以后再考虑
     */
    public List<VNF> placementByGA(Topo topo,List<SFC> requests, int population, int generation){

        List<VNF> solution = new ArrayList<>();
        for(SFC sfc : requests){
            solution.addAll(onceGA(topo, sfc, population, generation));
        }
        return solution;
    }

    // 单次放置GA
    public List<VNF> onceGA(Topo topo, SFC sfc,int population, int generation){
        NetworkInspector networkInspector = new NetworkInspector(topo);
        List<Chromosome> tempGeneration = new LinkedList<>();
        List<Node> nodeList;
        List<Chromosome> nextGeneration = new LinkedList<>();
        Chromosome elite1,elite2;
        for (int j = 0; j < population; j++) {
            nodeList = generateAResult(topo, sfc);
            tempGeneration.add(new Chromosome(nodeList, computeDelay(topo, nodeList, sfc)));
        }
        for (int i = 0; i < generation; i++) {
            tempGeneration.sort(Comparator.comparingInt(Chromosome::getDelay));
            elite1 = tempGeneration.get(0);
            elite2 = tempGeneration.get(1);
            nextGeneration.add(elite1);
            nextGeneration.add(elite2);
            tempGeneration = crossover(networkInspector, tempGeneration, sfc, 10, 0.5);
            nextGeneration.addAll(tempGeneration);
            nextGeneration = mutate(networkInspector, nextGeneration, sfc, 10, 0.1);
            tempGeneration.clear();
            tempGeneration.addAll(nextGeneration);
            nextGeneration.clear();
        }
        tempGeneration.sort(Comparator.comparingInt(Chromosome::getDelay));
        Chromosome result = null;
        for (int i = 0; i < tempGeneration.size(); i++) {
            result = tempGeneration.get(i);
            if(solutionValid(networkInspector, result, sfc))break;
        }
//        System.out.println(result);
        return networkInspector.deploySFC(result.getNodes(), sfc);
    }

    private List<Chromosome> mutate(NetworkInspector networkInspector, List<Chromosome> tempGeneration,SFC sfc, int times, double muRate){
        Random random = new Random();
        int geneNum = tempGeneration.get(0).getNodes().size();
        int size = 0;
        List<Node> nodes = networkInspector.getTopo().getNodes();
        Chromosome cloneChromosome;
        List<Chromosome> result = new LinkedList<>();
        for(Chromosome chromosome : tempGeneration){
            cloneChromosome = Chromosome.clone(chromosome);
            size = result.size();
            for (int i = 0; i < times; i++) {
                for (int j = 0; j < geneNum; j++) {
                    if(random.nextDouble(0,1) < muRate){
                        chromosome.getNodes().set(j, nodes.get(random.nextInt(0, nodes.size())));
                    }
                }
                if(solutionValid(networkInspector, chromosome, sfc)){
                    chromosome.setDelay(computeDelay(networkInspector.getTopo() ,chromosome.getNodes(),sfc));
                    result.add(chromosome);
                    break;
                }else {
                    chromosome = Chromosome.clone(cloneChromosome);
                }
            }
            if(size == result.size()){
                result.add(cloneChromosome);
            }
        }
        return result;
    }
    private List<Chromosome> crossover(NetworkInspector networkInspector, List<Chromosome> tempGeneration,SFC sfc, int times, double crRate) {
        int time = tempGeneration.size()/2 - 1, all = tempGeneration.stream().mapToInt(Chromosome::getDelay).sum();
        Random random = new Random();
        List<Chromosome> result = new LinkedList<>();
        Chromosome c1, c2, c1Clone, c2Clone;
        int  index, geneNum, size;
        double isCr, rate;
        double[] probability = tempGeneration.stream().mapToDouble(c -> (1.0 - 1.0 * c.getDelay() / all) / (tempGeneration.size() - 1)).toArray();
        while (time-- > 0){
            rate = random.nextDouble(0, 1);
            for (index = 0; index < tempGeneration.size() - 1; index++) {
                rate -= probability[index];
                if(rate < 0) break;
            }
            c1 = tempGeneration.get(index);
            c1Clone = Chromosome.clone(c1);
            rate = random.nextDouble(0, 1);
            for (index = 0; index < tempGeneration.size() - 1; index++) {
                rate -= probability[index];
                if(rate < 0) break;
            }
            c2 = tempGeneration.get(index);
            c2Clone = Chromosome.clone(c2);
            geneNum = c1.getNodes().size();
            size = result.size();
            for (int i = 0; i < times; i++) {
                for (int j = 0; j < geneNum; j++) {
                    isCr = random.nextDouble(0, 1);
                    if(isCr < crRate){
                        Node tempNode = c1.getNodes().get(j);
                        c1.getNodes().set(j, c2.getNodes().get(j));
                        c2.getNodes().set(j, tempNode);
                    }
                }
                if(solutionValid(networkInspector, c1, sfc) && solutionValid(networkInspector, c2, sfc)){
                    c1.setDelay(computeDelay(networkInspector.getTopo(), c1.getNodes(), sfc));
                    c2.setDelay(computeDelay(networkInspector.getTopo(), c2.getNodes(), sfc));
                    result.add(c1);
                    result.add(c2);
                    break;
                }else {
                    c1 = Chromosome.clone(c1Clone);
                    c2 = Chromosome.clone(c2Clone);
                }
            }
            if(result.size() == size){
                result.add(c1Clone);
                result.add(c2Clone);
            }
        }
        return result;
    }
    private boolean solutionValid(NetworkInspector networkInspector, Chromosome chromosome, SFC sfc){
        HashMap<Node, Integer> nodeCpuUsed = new HashMap<>();
        HashMap<Node, Integer> nodeMemUsed = new HashMap<>();
        List<Node> nodes = networkInspector.getTopo().getNodes();
        Node node;
        for (int i = 0; i < nodes.size(); i++) {
            node = nodes.get(i);
            nodeCpuUsed.put(node, 0);
            nodeMemUsed.put(node, 0);
        }
        int size = sfc.getRequiredVNF().size();
        VNF vnf;
        List<Node> nodeList = chromosome.getNodes();
        List<String> requiredType = sfc.getRequiredVNF();
        for (int i = 0; i < size; i++) {
            node = nodeList.get(i);
            vnf = node.getVNFByType(requiredType.get(i));
            if(vnf != null){
                if(node.getRestCpuResource() >= vnf.getRequiredCpuResource(sfc) + nodeCpuUsed.get(node) &&
                        node.getRestMemResource() >= vnf.getRequiredMemResource(sfc) + nodeMemUsed.get(node) ){
                    nodeCpuUsed.put(node, nodeCpuUsed.get(node) + vnf.getRequiredCpuResource(sfc));
                    nodeMemUsed.put(node, nodeMemUsed.get(node) + vnf.getRequiredMemResource(sfc));
                }else {
                    return false;
                }
            }else {
                vnf = new VNF(1, requiredType.get(i));
                if(node.getRestCpuResource() >= vnf.getRequiredCpuResource(sfc) + vnf.getDefaultCpuConsumption() + nodeCpuUsed.get(node) &&
                        node.getRestMemResource() >= vnf.getRequiredMemResource(sfc) + vnf.getDefaultMemConsumption() + nodeMemUsed.get(node) ) {
                    nodeCpuUsed.put(node, nodeCpuUsed.get(node) + vnf.getRequiredCpuResource(sfc) + vnf.getDefaultCpuConsumption());
                    nodeMemUsed.put(node, nodeMemUsed.get(node) + vnf.getRequiredMemResource(sfc) + vnf.getDefaultMemConsumption());
                } else {
                    return false;
                }
            }
        }
        return true;
    }
    private int computeDelay(Topo topo, List<Node> nodes, SFC sfc){
        int delay = 0;
        List<Link> list;
        for (int i = 0; i < nodes.size()-1; i++) {
            delay += VirtualLink.getLinksBetweenNode(nodes.get(i), nodes.get(i+1), sfc.getFlowRate()).stream().mapToInt(Link::getDelay).sum();
        }
        if(nodes.get(0).getNodeId() != sfc.getAccessNodeId()){
            list = VirtualLink.getLinksBetweenNode(topo.getNodes().get(sfc.getAccessNodeId() - 1), nodes.get(0), sfc.getFlowRate());
            delay += list.stream().mapToInt(Link::getDelay).sum();
        }
        return delay;
    }
    private List<Node> generateAResult(Topo topo, SFC sfc){
        List<String> requiredVNF = sfc.getRequiredVNF();
        List<Node> nodes = topo.getNodes();
        List<Node> result = new ArrayList<>();
        HashMap<Node, Integer> nodeCpuUsed = new HashMap<>();
        HashMap<Node, Integer> nodeMemUsed = new HashMap<>();
        HashSet<Node> tempSet = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            nodeCpuUsed.put(nodes.get(i), 0);
            nodeMemUsed.put(nodes.get(i), 0);
        }
        Random random = new Random();
        Node node;
        VNF vnf;
        for(String type : requiredVNF){
            node = nodes.get(random.nextInt(0, nodes.size()));
            while (true){
                vnf = node.getVNFByType(type);
                if(vnf != null){
                    if(node.getRestCpuResource() >= vnf.getRequiredCpuResource(sfc) + nodeCpuUsed.get(node) &&
                            node.getRestMemResource() >= vnf.getRequiredMemResource(sfc) + nodeMemUsed.get(node) ){
                        result.add(node);
                        nodeCpuUsed.put(node, nodeCpuUsed.get(node) + vnf.getRequiredCpuResource(sfc));
                        nodeMemUsed.put(node, nodeMemUsed.get(node) + vnf.getRequiredMemResource(sfc));
                        tempSet.add(node);
                        break;
                    }
                } else {
                    vnf = new VNF(1, type);
                    if(node.getRestCpuResource() >= vnf.getRequiredCpuResource(sfc) + vnf.getDefaultCpuConsumption() + nodeCpuUsed.get(node) &&
                            node.getRestMemResource() >= vnf.getRequiredMemResource(sfc) + vnf.getDefaultMemConsumption() + nodeMemUsed.get(node) ) {
                        result.add(node);
                        nodeCpuUsed.put(node, nodeCpuUsed.get(node) + vnf.getRequiredCpuResource(sfc) + vnf.getDefaultCpuConsumption());
                        nodeMemUsed.put(node, nodeMemUsed.get(node) + vnf.getRequiredMemResource(sfc) + vnf.getDefaultMemConsumption());
                        tempSet.add(node);
                        break;
                    }
                }
                node = nodes.get(random.nextInt(0, nodes.size()));
                while (tempSet.contains(node)){
                    node = nodes.get(random.nextInt(0, nodes.size()));
                }
            }
        }
        return result;
    }
}
