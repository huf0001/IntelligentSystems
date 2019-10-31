import jade.core.AID;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

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

    //Basic Public Properties

    public Depot getDepot() { return depot; }
    public NodeGraph getGraph() { return graph; }
    public int getHeight() { return height; }
    public List<Truck> getTrucks() { return trucks; }
    public int getWidth() { return width; }

    //Complex Public Properties

    public Node getRandomNode()
    {
        ArrayList<Node> nodes = new ArrayList<Node>(graph.adjNodes.keySet());
        return nodes.get((int)RandomFloatBetween(0, nodes.size() - 1));
    }

    public Road getRandomRoad()
    {
        ArrayList<Node> nodes = new ArrayList<Node>(graph.adjNodes.keySet());
        return new Road(nodes.get(0), nodes.get((int)RandomFloatBetween(0, nodes.size() - 1)));
    }

    //Setup Methods----------------------------------------------------------------------------------------------------------------------------------

    public World() throws ControllerException
    {
        List<AID> truckAIDs = new ArrayList<>();
        float minWeightLimit = 15;
        float maxWeightLimit = 30;
        int numTrucks = 10;
        List<AgentController> agentControllers = new ArrayList<>();

        graph = createGraph();
        Node depotNode = graph.getNodeWithID(0);

        for (Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            graph.addEdgesInRange(entry.getKey(), 95);
        }

        for (int i = 0; i < numTrucks; i++)
        {
            Truck truck = new Truck(this, depotNode, RandomFloatBetween(minWeightLimit, maxWeightLimit));

            //Start the trucks on the container
            try
            {
                agentControllers.add(Simulation.getContainerController().acceptNewAgent("Truck" + i, truck));
                Object var = Simulation.getContainerController().getAgent("Truck"+i);

                if (var != null)
                {
                    System.out.println(truck.getLocalName() + ": successfully added to container");
                }
            }
            catch (StaleProxyException e)
            {
                e.printStackTrace();
            }

            trucks.add(truck);
            truckAIDs.add(trucks.get(i).getAID());
        }

        depot = new Depot(this, truckAIDs);

        try
        {
            depot.StartVRP();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        AgentController agentController = Simulation.getContainerController().acceptNewAgent("Depot", depot);
        Object var = Simulation.getContainerController().getAgent("Depot");

        if (var != null)
        {
            System.out.println(depot.getLocalName() + ": successfully added to container");
            agentController.start();

            for(Truck t : trucks)
            {
                t.setDepotAID(depot.getAID());
            }

            for (AgentController ac : agentControllers)
            {
                ac.start();
            }
        }
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

        return graph;
    }

    //Recurring Methods------------------------------------------------------------------------------------------------------------------------------

    public void paint(Graphics g)
    {
        //Note: each new thing rendered gets rendered on top of everything else that's already been rendered
        super.paint(g);
        g.setColor(Color.BLACK);

        //Render Roads
        for (Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            List<Node> nodeList = entry.getValue();

            for (Node n : nodeList) {
                g.drawLine((int) n.position.x, (int) n.position.y, (int) node.position.x, (int) node.position.y);
            }
        }

        //Render Nodes
        for (Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            g.setColor(node.id == 0 ? Color.RED : (!depot.getNodesWithParcelsAssigned().contains(node) ? Color.BLUE : Color.GREEN));
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

    public Node getNodeByID(int id) { return graph.getNodeWithID(id); }

    private float RandomFloatBetween(float min, float max)
    {
        return random.nextFloat() * (max - min) + min;
    }

    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();
        int numNodes = graph.adjNodes.size();
        int numTrucks = depot.getNumTrucks();
        int totDemand = depot.getTotalDemand();
        stringBuilder.append("total number of nodes: ").append(numNodes).append("\n");
        stringBuilder.append("capacity of vehicles: Randomised between 10-20").append("\n");
        stringBuilder.append("number of vehicles: ").append(numTrucks).append("\n");
//        for (int i = 0; i < graph.adjNodes.size(); i++)
//        {
//            stringBuilder.append("Node ").append(i + 1).append(" ").append(graph.adjNodes.keySet().).append("\n");
//        }
        stringBuilder.append("total parcel weight: ").append(totDemand).append("\n");
        return stringBuilder.toString();
    }
}






