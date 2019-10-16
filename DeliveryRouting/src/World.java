import jade.core.AID;

import javax.swing.JFrame;
import java.awt.*;
import java.util.*;
import java.util.List;

public class World extends JFrame
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private int width = 1200;
    private int height = 700;
    private NodeGraph graph;
    private List<Truck> trucks;
    private boolean trucksSet = false;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    public World(List<Truck> trucks)
    {
        this.trucks = trucks;
        graph = createGraph();
        Vector2 depotPos = graph.getDepotNode().position;

        for (Truck truck : this.trucks)
        {
            truck.setPosition(depotPos);
        }

        setTitle("Delivery Routing");
        setSize(width, height);
        setLocationRelativeTo(null);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    private NodeGraph createGraph()
    {
        NodeGraph graph = new NodeGraph();
        List<Vector2> points = PoissonDiscSampling.GeneratePoints(50, new Vector2(width, height), 3);
        int count = 0;

        for(Vector2 v : points)
        {
            graph.addNode(count, v);
            count++;
        }

//        graph.addNode(1);
//        graph.createWorldPoints();
        return graph;
    }

    public void paint(Graphics g)
    {
        //Note: each new thing rendered gets rendered on top of everything else that's already been rendered

        //Render Roads
        g.setColor(Color.BLACK);

        for(Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            List<Node> nodeList = entry.getValue();
            graph.addEdgesInRange(node, 95);

            for(Node n: nodeList)
            {
                g.drawLine((int)n.position.x, (int)n.position.y, (int)node.position.x, (int)node.position.y);
            }
        }

        //Render Nodes
        for(Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            g.setColor(node.id == 0 ? Color.RED : Color.BLUE);
            g.fillOval((int)node.position.x - 5, (int)node.position.y- 5, 10, 10);
        }

        //Render Trucks
        g.setColor(Color.BLACK);

        for (Truck truck : trucks)
        {
            g.drawOval((int)truck.getPosition().x - 10, (int)truck.getPosition().y- 10, 20, 20);
        }
    }
}






