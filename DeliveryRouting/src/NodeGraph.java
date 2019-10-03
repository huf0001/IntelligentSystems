import java.util.*;
import java.lang.Math;
import java.util.Random;



public class NodeGraph {
    public Map<Node, List<Node>> adjNodes;
    Random rand = new Random();


    public NodeGraph(){
        adjNodes = new HashMap<Node, List<Node>>();
    }

    void addNode(int id){
        Node n = new Node(id);
        adjNodes.putIfAbsent(n, new ArrayList<>());
    }
    void removeNode(int id){
        Node n = getNodeWithID(id);
        adjNodes.values().stream().forEach(e -> e.remove(n));
        adjNodes.remove(n);
    }
    void addEdge(int src, int dest){
        Node n1 = getNodeWithID(src);
        Node n2 = getNodeWithID(dest);
        adjNodes.get(n1).add(n2);
        adjNodes.get(n2).add(n1);
    }

    void addEdge(Node src, Node dest){
        Node n1 = getNode(src);
        Node n2 = getNode(dest);
        adjNodes.get(n1).add(n2);
        adjNodes.get(n2).add(n1);
    }
    void removeEdge(int src, int dest){
        Node n1 = getNodeWithID(src);
        Node n2 = getNodeWithID(dest);
        List<Node> eN1 = adjNodes.get(src);
        List<Node> eN2 = adjNodes.get(dest);
        if (eN1 != null)
            eN1.remove(n2);
        if (eN2 != null)
            eN2.remove(n1);
    }
    private List<Node> getAdjNodes(int id){
        return adjNodes.get(getNodeWithID(id));
    }

    void printNodes(int id){
        System.out.print(id + "-->");
        for (Node n : getAdjNodes(id)){
            System.out.print(n.id + " ");
        }
    }
    List<Node.WorldPoint> GetPoints(){
        List<Node.WorldPoint> points = new ArrayList<>();
        for (Node n : adjNodes.keySet()){
            points.add(n.position);
        }
        return points;
    }



    private Node getNodeWithID(int id){
        for (Node n : adjNodes.keySet()){
            if(n.id == id){
                return n;
            }
        }
        return null;
    }

    private Node getNode(Node node){
        for (Node n : adjNodes.keySet()){
            if(n == node){
                return n;
            }
        }
        return null;
    }

    void addEdgesInRange(Node node, int radius){
        int x_dist, y_dist;
        for (Node n : adjNodes.keySet()){
            x_dist = Math.abs(node.position.x - n.position.x);
            y_dist = Math.abs(node.position.y - n.position.y);
            if ((x_dist < radius) && (y_dist < radius)){
                addEdge(n, node);
            }
        }
    }

    void createWorldPoints(){
        for (Node n: adjNodes.keySet()){
            Node.WorldPoint temp = new Node.WorldPoint(rand.nextInt(600) + 100, rand.nextInt(600) + 100);
            n.position = temp;
        }
        for (Node n: adjNodes.keySet()){
            for(int i = 0; i < 1000; i++){
                Node.WorldPoint temp = new Node.WorldPoint(rand.nextInt(800), rand.nextInt(800));

                if(!checkPoint(temp, 100)){
                    n.position = temp;
                }
            }
        }
    }

    boolean checkPoint(Node.WorldPoint w, int radius){
        boolean result = true;
        for(Node n: adjNodes.keySet()){
            if (Math.abs(n.position.x - w.x) < radius && Math.abs(n.position.y - w.y) < radius){
                result = false;
            }
        }
        return result;
    }
}

