//Code taken and adapted from https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TravelingSalesman {
    //To use this class to generate the shortest route possible:
    //Set route with SetRoute();
    //Call SimulateAnnealing(double startingTemperature, int numberOfIterations, double coolingRate)
    //Notes on these variables from https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman:
        //For small solution spaces it's better to lower the starting temperature and increase the cooling rate,
        //as it will reduce the simulation time, without lose of quality.
        //For bigger solution spaces please choose the higher starting temperature and small cooling rate,
        //as there will be more local minima.
        //Always provide enough time to simulate from the high to low temperature of the system

    private List<Node> _previousRoute;
    private List<Node> _route;
    private List<Node> _bestRoute;

    public void SetRoute(List<Node> route){
        _route = route;
    }
    public void SetRoute(List<Integer> routeInts, World world){
        List<Node> route = new ArrayList<>();
        for (Integer id: routeInts)  {
            route.add(world.getNodeByID(id));
        }
        _route = route;
    }

    public List<Integer> SimulateAnnealing(double startingTemperature, int numberOfIterations, double coolingRate) {
        double t = startingTemperature;
        double bestDistance = GetDistance();
        _bestRoute = _route;
        int i = 0;

        for (i = 0; i < numberOfIterations; i++) {
            if (t > 0.1) {
                SwapNodes();
                double currentDistance = GetDistance();
                if (currentDistance < bestDistance) {
                    bestDistance = currentDistance;
                    _bestRoute = _route;
                    System.out.println("Better route found");
                } else if (Math.exp((bestDistance - currentDistance) / t) < Math.random()) {
                    RevertSwap();
                }
                t *= 1 - coolingRate;
            } else {
                break;
            }
        }

        System.out.println("Iterations " + i);
        System.out.println("Cooling " + coolingRate);
        System.out.println("Temp " + t);

        List<Integer> output = new ArrayList<>();
        for (Node node : _bestRoute){
            output.add(node.id);
        }
        System.out.println("Route " + output + "\n");
        return output;
    }

    private void SwapNodes() {
        Random r = new Random();
        int a = GetIndex();
        int b = GetIndex();
        _previousRoute = _route;
        Node x = _route.get(a);
        Node y = _route.get(b);
        _route.set(a, y);
        _route.set(b, x);
    }

    private int GetIndex() {
        Random r = new Random();
        int result = r.nextInt(_route.size() - 1) + 1;
        return result;
    }

    private void RevertSwap() {
        _route = _previousRoute;
    }

    private int GetDistance() {
        int distance = 0;
        for (int i = 0; i < _route.size(); i++) {
            Node starting = _route.get(i);
            Node destination;
            if (i + 1 < _route.size()) {
                destination = _route.get(i + 1);
            } else {
                destination = _route.get(0);
            }
            distance += starting.GetDistance(destination);
        }
        return distance;
    }
}
