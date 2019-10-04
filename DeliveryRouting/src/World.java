import javax.swing.JFrame;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class World extends JFrame
{
    //Problem: if width != height, some bunching up of nodes on the right-hand side of the window. ==> Gonna use 700 x 700
    private static int width = 700;    //for height = 700, width <= 800 works, >= 900 doesn't.
    private static int height = 700;    //height > 700 too tall for screen; for width = 1000, height <= 900 works, >= 800 doesn't work.

    NodeGraph graph;

    public World()
    {
        graph = createGraph();
        setTitle("Delivery Routing");
        setSize(width, height);
        setLocationRelativeTo(null);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    static NodeGraph createGraph()
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
        for(Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet())
        {
            Node node = entry.getKey();
            List<Node> nodeList = entry.getValue();
            graph.addEdgesInRange(node, 95);

            for(Node n: nodeList)
            {
                g.drawLine((int)n.position.x, (int)n.position.y, (int)node.position.x, (int)node.position.y);
            }

            if(node.id == 0)
            {
                g.setColor(Color.RED);
                g.fillOval((int)node.position.x - 5, (int)node.position.y- 5, 10, 10);
                g.setColor(Color.BLACK);

            }
            else
            {
                g.setColor(Color.BLUE);
                g.fillOval((int)node.position.x - 5, (int)node.position.y- 5, 10, 10);
                g.setColor(Color.BLACK);
            }

//            g.fillOval((int)node.position.x - 5, (int)node.position.y - 5, 10, 10);
//            g.drawString(Integer.toString(node.id), (int)node.position.x , (int)node.position.y);
//            g.drawOval((int)node.position.x - 15, (int)node.position.y - 15, 30, 30);
        }
    }

    public static void main(String[] args)
    {
        World w = new World();
    }
}






