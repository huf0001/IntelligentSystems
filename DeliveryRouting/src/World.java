import javax.swing.JFrame;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;



public class World extends JFrame {

    NodeGraph graph = createGraph();


    public void paint(Graphics g){

        for(Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet()){
            Node node = entry.getKey();
            List<Node> nodeList = entry.getValue();

//            g.fillOval(node.position.x - 5, node.position.y- 5, 10, 10);
//            g.fillOval((int)node.position.x - 5, (int)node.position.y - 5, 10, 10);
            g.drawString(Integer.toString(node.id), (int)node.position.x , (int)node.position.y);
//            g.drawOval((int)node.position.x - 15, (int)node.position.y - 15, 30, 30);


            graph.addEdgesInRange(node, 100);

            for(Node n: nodeList){
                g.drawLine((int)n.position.x, (int)n.position.y, (int)node.position.x, (int)node.position.y);
            }
        }
    }




    public World() {
        setTitle("Delivery Routing");
        setSize(800, 800);
        setLocationRelativeTo(null);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

    }

    static NodeGraph createGraph(){
        NodeGraph graph = new NodeGraph();
        int count = 0;
        List<Vector2> points = PoissonDiscSampling.GeneratePoints(50, new Vector2(800, 800), 1);
        for(Vector2 v : points){
            graph.addNode(count, v);

            count++;
        }
//        graph.addNode(1);
//        graph.createWorldPoints();
        return graph;
    }




    public static void main(String[] args) {
        World w = new World();


    }
}






