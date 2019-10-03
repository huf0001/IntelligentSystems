import javax.swing.JFrame;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Math;
import java.util.Random;



public class World extends JFrame {
    public void paint(Graphics g){

        Random rand = new Random();

        List<NodeGraph.Edge> edges = Arrays.asList(new NodeGraph.Edge(0, 1, 1), new NodeGraph.Edge(1, 2,1),
                new NodeGraph.Edge(2, 0, 1), new NodeGraph.Edge(2, 1, 1),new NodeGraph.Edge(3, 2, 1),
                new NodeGraph.Edge(4, 5, 1), new NodeGraph.Edge(5, 4, 1));

        // construct graph from given list of edges
        NodeGraph graph = new NodeGraph(edges);

        for (List<Node> n : graph.adj){
//            graph.adj.get()
        }

        int src = 0;
        int n = graph.adj.size();

        while (src < n)
        {
            // print current vertex and all its neighboring vertices
            for (Node edge : graph.adj.get(src)) {
//                edge.position = new Node.WorldPoint(rand.nextInt(800), rand.nextInt(800));
//                g.fillOval(edge.position.x, edge.position.y, 10,10);
                System.out.print(src + " --> " + edge.id +
                        " (" + edge.weight + ") " + "\t");
            }

            System.out.println();
            src++;
        }

    }

    public static void printGraph(NodeGraph graph)
    {

    }



    public World() {


        setTitle("Delivery Routing");
        setSize(800, 600);
//        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

    }

    // Weighted Directed Graph Implementation in Java
    public static void main(String[] args) {
        World w = new World();

        // Input: List of edges in a digraph (as per above diagram)


        // print adjacency list representation of the graph
    }
}






