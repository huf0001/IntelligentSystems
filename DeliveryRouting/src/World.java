import javax.swing.JFrame;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;



public class World extends JFrame {


    public void paint(Graphics g){
        NodeGraph graph = createGraph();
        graph.createWorldPoints();



        for(Map.Entry<Node, List<Node>> entry : graph.adjNodes.entrySet()){
            Node node = entry.getKey();
            List<Node> nodeList = entry.getValue();

            g.fillOval(node.position.x - 5, node.position.y- 5, 10, 10);


            graph.addEdgesInRange(node, 60);

            for(Node n: nodeList){
                g.drawLine(n.position.x, n.position.y, node.position.x, node.position.y);
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
        for(int i = 0;  i < 50; i++){
            graph.addNode(i);
        }
        return graph;
    }




    public static void main(String[] args) {
        World w = new World();

    }
}






