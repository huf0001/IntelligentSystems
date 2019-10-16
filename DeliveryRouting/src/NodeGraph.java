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
        adjNodes.putIfAbsent(new Node(id), new ArrayList<>());
    }

    void addNode(int id, Vector2 v){
        adjNodes.putIfAbsent(new Node(id, v), new ArrayList<>());
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
    void removeEdge(int src, int dest) {
        Node n1 = getNodeWithID(src);
        Node n2 = getNodeWithID(dest);
        List<Node> eN1 = adjNodes.get(src);
        List<Node> eN2 = adjNodes.get(dest);
        if (eN1 != null)
            eN1.remove(n2);
        if (eN2 != null)
            eN2.remove(n1);
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

    public Node getDepotNode()
    {
        return getNodeWithID(0);
    }

    void addEdgesInRange(Node node, int radius){
        double dist;
        for (Node n : adjNodes.keySet()){
            dist = Vector2.distance(node.position, n.position);
            if ((dist < radius)){
                addEdge(n, node);
            }
        }
    }


    boolean checkPoint(Vector2 w, int radius){
        boolean result = true;
        for(Node n: adjNodes.keySet()){
            if (Vector2.distance(n.position, w) < radius){
                result = false;
            }
        }
        return result;
    }
}

