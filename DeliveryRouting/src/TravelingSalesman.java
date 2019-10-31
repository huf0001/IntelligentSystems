//Code taken and adapted from https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman

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

    public List<Node> SimulateAnnealing(double startingTemperature, int numberOfIterations, double coolingRate) {
        double t = startingTemperature;
        double bestDistance = GetDistance();

        for (int i = 0; i < numberOfIterations; i++) {
            if (t > 0.1) {
                SwapNodes();
                double currentDistance = GetDistance();
                if (currentDistance < bestDistance) {
                    bestDistance = currentDistance;
                    _bestRoute = _route;
                } else if (Math.exp((bestDistance - currentDistance) / t) < Math.random()) {
                    RevertSwap();
                }
                t *= coolingRate;
            } else {
                continue;
            }
        }

        return _bestRoute;
    }

    private void SwapNodes() {
        Random r = new Random();
        r.nextInt(_route.size() + 1);
        int a = r.nextInt(_route.size() + 1);
        int b = r.nextInt(_route.size() + 1);
        _previousRoute = _route;
        Node x = _route.get(a);
        Node y = _route.get(b);
        _route.set(a, y);
        _route.set(b, x);
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
