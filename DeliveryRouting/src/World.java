import jade.core.AID;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class World extends JPanel
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private Random random = new Random();

    private JFrame frame;
    private NodeGraph graph;
    private List<Truck> trucks = new ArrayList<Truck>();
    private Depot depot;

    private int width = 1200;
    private int height = 700;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public int getHeight() { return height; }

    //For testing--------------------------------------------------------------
    public Node getRandomNode()
    {
        ArrayList<Node> nodes = new ArrayList<Node>(graph.adjNodes.keySet());
        return nodes.get((int)RandomFloatBetween(0, nodes.size() - 1));
    }

    public int getWidth() { return width; }
    public NodeGraph getGraph() { return graph; }

    //Setup Methods----------------------------------------------------------------------------------------------------------------------------------

    public World()
    {
        List<AID> truckAIDs = new ArrayList<>();
        float minWeightLimit = 10;
        float maxWeightLimit = 20;
        int numTrucks = 10;

        graph = createGraph();
        Node depotNode = graph.getNodeWithID(0);

        for (int i = 0; i < numTrucks; i++)
        {
            trucks.add(new Truck(this, depotNode, RandomFloatBetween(minWeightLimit, maxWeightLimit)));
            truckAIDs.add(trucks.get(i).getAID());
        }

        depot = new Depot(truckAIDs);
    }

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

    //Recurring Methods------------------------------------------------------------------------------------------------------------------------------

    public void updateTrucks()
    {
        for (Truck truck : trucks)
        {
            //truck.GoToNextNode();   //For testing
        }
    }

    public void paint(Graphics g)
    {
        super.paint(g);
        g.setColor(Color.BLACK);

        //Note: each new thing rendered gets rendered on top of everything else that's already been rendered

        //Render Roads
        for (Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            List<Node> nodeList = entry.getValue();
            graph.addEdgesInRange(node, 95);

            for (Node n : nodeList) {
                g.drawLine((int) n.position.x, (int) n.position.y, (int) node.position.x, (int) node.position.y);
            }
        }

        //Render Nodes
        for (Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            g.setColor(node.id == 0 ? Color.RED : Color.BLUE);
            g.fillOval((int) node.position.x - 5, (int) node.position.y - 5, 10, 10);
        }

        //Render Trucks
        g.setColor(Color.BLACK);

        for (Truck truck : trucks)
        {
            g.drawOval((int)truck.getPosition().x - 10, (int)truck.getPosition().y- 10, 20, 20);
        }
    }

    //Utility Methods--------------------------------------------------------------------------------------------------------------------------------

    private float RandomFloatBetween(float min, float max)
    {
        return random.nextFloat() * (max - min) + min;
    }
}






