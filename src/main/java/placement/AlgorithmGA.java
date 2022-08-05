package placement;

import components.*;
import migration.DataCollector;

import java.util.*;

public class AlgorithmGA {
    public static double crRate = 0.5;
    public static double muRate = 0.1;
    public static int muTimes = 10;
    public static int crossTimes = 10;
    private Topo topo;
    private int index;
    public AlgorithmGA(Topo topo) {
        this.topo = topo;
        topo.computeIndex();
        index = topo.getIndex();
    }

    public AlgorithmGA() {
    }

    /**
     * 使用遗传算法计算placement的解,需要设置遗传算法的代数,
     * computeDelay需要优化，计算过的链路需要存下来，这样可以节约计算延时的时间，但是这个部分等项目先做完以后再考虑
     */
    public List<VNF> placementByGA(List<SFC> requests, int population, int generation){
        List<VNF> solution = new ArrayList<>();
        for(SFC sfc : requests){
            solution.addAll(onceGA(sfc, population, generation));
        }
        return solution;
    }

    // 单次放置GA
    public List<VNF> onceGA(SFC sfc,int population, int generation){
        NetworkInspector networkInspector = new NetworkInspector(topo);
        List<Chromosome> tempGeneration = new LinkedList<>();
        List<Node> nodeList;
        List<Chromosome> nextGeneration = new LinkedList<>();
        Chromosome elite1,elite2;
        List<Chromosome> resultSet = new ArrayList<>();
        Chromosome result = null;
        int times = 0;
        for (int j = 0; j < population; j++) {
            nodeList = generateAResult(sfc);
            tempGeneration.add(new Chromosome(nodeList, computeDelay(nodeList, sfc)));
        }
        tempGeneration.sort(Comparator.comparingInt(Chromosome::getDelay));
        for (int i = 0; i < generation; i++) {
            elite1 = tempGeneration.get(0);
            elite2 = tempGeneration.get(1);
            nextGeneration.add(elite1);
            nextGeneration.add(elite2);
            tempGeneration = crossover(networkInspector, tempGeneration, sfc);
            nextGeneration.addAll(tempGeneration);
            nextGeneration = mutate(networkInspector, nextGeneration, sfc);
            tempGeneration.clear();
            tempGeneration.addAll(nextGeneration);
            nextGeneration.clear();
            tempGeneration.sort(Comparator.comparingInt(Chromosome::getDelay));
            resultSet.add(tempGeneration.get(0));
        }
        resultSet.sort(Comparator.comparingInt(Chromosome::getDelay));
        for (int i = 0; i < resultSet.size(); i++) {
            result = resultSet.get(i);
            if(solutionValid(networkInspector, result, sfc))break;
        }
//        System.out.println(result);
        return networkInspector.deploySFC(result.getNodes(), sfc);
    }

    private List<Chromosome> mutate(NetworkInspector networkInspector, List<Chromosome> tempGeneration,SFC sfc){
        Random random = new Random();
        int geneNum = tempGeneration.get(0).getNodes().size();
        int size = 0;
        List<Node> nodes = networkInspector.getTopo().getNodes();
        Chromosome cloneChromosome;
        List<Chromosome> result = new LinkedList<>();
        for(Chromosome chromosome : tempGeneration){
            cloneChromosome = Chromosome.clone(chromosome);
            size = result.size();
            for (int i = 0; i < muTimes; i++) {
                for (int j = 0; j < geneNum; j++) {
                    if(random.nextDouble(0,1) < muRate){
                        chromosome.getNodes().set(j, nodes.get(random.nextInt(0, nodes.size())));
                    }
                }
                if(solutionValid(networkInspector, chromosome, sfc)){
                    chromosome.setDelay(computeDelay(chromosome.getNodes(),sfc));
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
    private List<Chromosome> crossover(NetworkInspector networkInspector, List<Chromosome> tempGeneration,SFC sfc) {
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
            for (int i = 0; i < crossTimes; i++) {
                for (int j = 0; j < geneNum; j++) {
                    isCr = random.nextDouble(0, 1);
                    if(isCr < crRate){
                        Node tempNode = c1.getNodes().get(j);
                        c1.getNodes().set(j, c2.getNodes().get(j));
                        c2.getNodes().set(j, tempNode);
                    }
                }
                if(solutionValid(networkInspector, c1, sfc) && solutionValid(networkInspector, c2, sfc)){
                    c1.setDelay(computeDelay(c1.getNodes(), sfc));
                    c2.setDelay(computeDelay(c2.getNodes(), sfc));
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
        Set<Node> P4Nodes = new HashSet<>();
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
            if(P4Nodes.contains(node))return false;
            if(node.isP4Node()) P4Nodes.add(node);
            vnf = node.getVNFByType(requiredType.get(i));
            if(vnf != null){
                if(node.getRestCpuResource() >= vnf.getRequiredCpuResource(sfc) + nodeCpuUsed.get(node) &&
                        node.getRestMemResource() >= vnf.getRequiredMemResource(sfc) + nodeMemUsed.get(node) ){
                    nodeCpuUsed.put(node, nodeCpuUsed.get(node) + vnf.getRequiredCpuResource(sfc));
                    nodeMemUsed.put(node, nodeMemUsed.get(node) + vnf.getRequiredMemResource(sfc));
                }else {
                    return false;
                }
            }else if(!node.isP4Node() || node.getVnfList().size() == 0){
                vnf = new VNF(index, requiredType.get(i));
                if(node.getRestCpuResource() >= vnf.getRequiredCpuResource(sfc) + vnf.getDefaultCpuConsumption() + nodeCpuUsed.get(node) &&
                        node.getRestMemResource() >= vnf.getRequiredMemResource(sfc) + vnf.getDefaultMemConsumption() + nodeMemUsed.get(node) ) {
                    nodeCpuUsed.put(node, nodeCpuUsed.get(node) + vnf.getRequiredCpuResource(sfc) + vnf.getDefaultCpuConsumption());
                    nodeMemUsed.put(node, nodeMemUsed.get(node) + vnf.getRequiredMemResource(sfc) + vnf.getDefaultMemConsumption());
                } else {
                    return false;
                }
            }else return false;
        }
        return true;
    }
    private int computeDelay(List<Node> nodes, SFC sfc){
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
    private List<Node> generateAResult(SFC sfc){
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
                } else if(!node.isP4Node() || node.getVnfList().size() == 0){
                    vnf = new VNF(index, type);
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
