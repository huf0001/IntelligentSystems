import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class NodeGraph {
    // data structure to store graph edges
    static class Edge {
        int src, dest;
        int weight;

        Edge(int src, int dest, int weight)
        {
            this.src = src;
            this.dest = dest;
            this.weight = weight;
        }
    };

    // data structure for adjacency list node

    // A list of lists to represent adjacency list
    List<List<Node>> adj = new ArrayList<>();

    // Constructor to construct graph
    public NodeGraph(List<Edge> edges)
    {
        // allocate memory for adjacency list
        for (int i = 0; i < edges.size(); i++)
            adj.add(i, new ArrayList<>());


        // add edges to the undirected graph
        for (Edge e : edges)
        {
            // allocate new node in adjacency List from src to dest
            adj.get(e.src).add(new Node(e.dest, e.weight));


            // Uncomment line 39 for undirected graph

            // allocate new node in adjacency List from dest to src
//             adj.get(e.dest).add(new Node(e.src, e.weight));
        }
    }

    public boolean checkPoint(List<Node.WorldPoint> points, Node.WorldPoint n){
        boolean result = true;
        int radius = 150;
        for (Node.WorldPoint var : points)
        {
            if(Math.abs(var.x - n.x) < radius && Math.abs(var.y - n.y) < radius){
                result = false;
            }
        }
        return result;
    }

    // print adjacency list representation of graph

}
