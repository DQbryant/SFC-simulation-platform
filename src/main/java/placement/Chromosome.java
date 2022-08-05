package placement;

import components.Link;
import components.Node;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class Chromosome implements Comparable<Chromosome >{
    private List<Node> nodes;
    private int delay;

    public Chromosome(List<Node> nodes, int delay) {
        this.nodes = nodes;
        this.delay = delay;
    }
    public static Chromosome clone(Chromosome chromosome){
        List<Node> nodes = new LinkedList<>(chromosome.getNodes());
        return new Chromosome(nodes, chromosome.getDelay());
    }
    @Override
    public int compareTo(Chromosome o) {
        return delay - o.getDelay();
    }
}
