import javax.swing.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleWaypointGen {

    private Node _start;
    private List<Node> _nodes = new ArrayList<>();
    private List<Node> _unRouted = new ArrayList<>();
    private List<Node> _routed = new ArrayList<>();
    private int _numRoutes;
    private int _nodesPerRoute;
    private int _extraNodes;

    public static void main(String[] args) {
        List<Node> nodes = new ArrayList<>();

        List<Vector2> points = PoissonDiscSampling.GeneratePoints(100, new Vector2(1200, 700), 3);
        int count = 0;

        for(Vector2 v : points)
        {
            nodes.add(new Node(count, v));
            count++;
        }

        long start = System.nanoTime();
        SimpleWaypointGen gen = new SimpleWaypointGen(nodes, 6);
        long finish = System.nanoTime();
        List<List<Node>> routes = gen.GenerateRoutes();

        int count2 = 0;
        for (List<Node> route : routes){
            count2++;
            System.out.println("Route " + count2 + " --------------------------------------------- \n");
            for (Node node : route) {
                System.out.println("Node " + node.id);
                System.out.println("Position " + (int)node.position.x + "," + (int)node.position.y + "\n");
            }
        }

        System.out.println(finish - start);
    }

    public SimpleWaypointGen(List<Node> nodes, int numRoutes) {
        _nodes = nodes;
        _unRouted = nodes;
        _numRoutes = numRoutes;
    }

    public List<List<Node>> GenerateRoutes(){
        List<List<Node>> routes = new ArrayList<>();

        //Setup start point
        _start = _unRouted.get(0);
        _unRouted.remove(0);

        //Setup how many nodes per route
        _nodesPerRoute = _unRouted.size() / _numRoutes;
        _extraNodes = _unRouted.size() % _numRoutes;

        //Generate the routes
        for (int i = 0; i < _numRoutes; i++) {
            routes.add(GenSingleRoute());
        }

        return routes;
    }

    private List<Node> GenSingleRoute() {
        List<Node> route = new ArrayList<>();
        Node currNode = _start;

        //Create a route by getting the next closest node
        for (int i = 0; i < _nodesPerRoute; i++) {
            Node finalCurrNode = currNode;
            _unRouted.sort(Comparator.comparing((n)-> finalCurrNode.GetDistance(n)));
            route.add(_unRouted.get(0));
            currNode = _unRouted.get(0);
            _unRouted.remove(0);
        }

        //If there is an uneven amount of nodes per route, add an extra node.
        if (_extraNodes > 0) {
            Node finalCurrNode1 = currNode;
            _unRouted.sort(Comparator.comparing((n)-> finalCurrNode1.GetDistance(n)));
            route.add(_unRouted.get(0));
            _unRouted.remove(0);
            _extraNodes--;
        }

        route.add(0, _start);

        return route;
    }
}
