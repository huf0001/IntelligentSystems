import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VrpFitnessFunc extends FitnessFunction {

    private static final int PENALIZE_INCOMPLETE_DELIVERY = 125;
    private static final int PENALIZE_INCOMPLETE_TRUCK = 5;
    private static final int PENALIZE_DISTANCE = 25;
    private static final int PENALIZE_EMPTY_TRUCK = 100;
    private final World world;
    private static Depot depot = null;

    public VrpFitnessFunc(final World conf, final Depot depot) {
        this.world = conf;
        this.depot = depot;
    }

    /**
     * Computes the fitness of the chromosome
     * - total_distance = distance covered by a vehicle
     * - total_demand = demand covered by solution:
     * - 25 * (covered_demand  - vehicle_capacity) - the truck can't deliver everything
     * - 2 * (vehicle_capacity - covered_demand)   - the truck has unused capacity
     * <p>
     * fitness = 10 * total_distance + total_demand
     * <p>
     * Constraints
     * 1<=fitness<=1000000
     *
     * @param chromosome given chromosome
     * @return fitness value of the chromosome
     */
    @Override
    public double evaluate(final IChromosome chromosome) {
        final int numberOfVehicles = depot.getNumTrucks();
        double fitness = 0;

        for (int i = 0; i < numberOfVehicles; i++) {
            fitness += computeTotalDistance(i, chromosome, world.getGraph()) * PENALIZE_DISTANCE;
            fitness += computeTotalDemand(i, chromosome, world);
        }

        if (fitness < 0) {
            return 0;
        }

        return Math.max(1, 1000000 - fitness);
    }

    public static double computeTotalCoveredDemand(int vehicleNumber, IChromosome chromosome, NodeGraph graph) {
        final List<Integer> positions = getPositions(vehicleNumber, chromosome, graph, false);

        double totalCoveredBySolution = 0.0;
        for (int pos : positions) {
            final Node node = graph.getNodeWithID(pos);
            totalCoveredBySolution += depot.getNodeDemand(node);
        }
        //if (totalCoveredBySolution == 0) System.out.println("truck has no parcels");
        return totalCoveredBySolution;
    }

    private static double computeTotalDemand(int vehicleNumber, IChromosome chromosome, World world) {
        final double totalCoveredBySolution = computeTotalCoveredDemand(vehicleNumber + 1, chromosome, world.getGraph());
        final double vehicleCapacity = world.getTrucks().get(vehicleNumber).getWeightLimit();

        if (totalCoveredBySolution == 0)
        {
            return (vehicleCapacity - totalCoveredBySolution) * PENALIZE_EMPTY_TRUCK;
        }
        if (totalCoveredBySolution > vehicleCapacity) {//can't complete delivery
            return (totalCoveredBySolution - vehicleCapacity) * PENALIZE_INCOMPLETE_DELIVERY;
        }
        return 0;
        //return (vehicleCapacity - totalCoveredBySolution) * PENALIZE_INCOMPLETE_TRUCK;//unused capacity
    }

    public static double computeTotalDistance(int vehicleNumber, IChromosome chromosome, NodeGraph graph) {
        double totalDistance = 0.0;
        final List<Integer> positions = getPositions(vehicleNumber + 1, chromosome, graph, true);

        final Node store = graph.getNodeWithID(0);//first node represents the starting point

        Node lastVisited = store;

        for (int pos : positions) {
            final Node node = graph.getNodeWithID(pos);
            totalDistance += Vector2.distance(lastVisited.position, node.position);//lastVisited.distanceTo(node);
            lastVisited = node;
        }

        totalDistance += Vector2.distance(lastVisited.position, store.position);//lastVisited.distanceTo(store);//distance back to the store

        return totalDistance;
    }


    /**
     * Reads data from the given chromosome in order
     * to generate the solution representing the
     * order of nodes for a vehicle
     *
     * @param vehicleNumber given vehicle
     * @param chromosome    to be decoded
     * @param graph existing configuration
     * @param order         if nodes need to be sorted
     * @return sequence of nodes representing the track of the given vehicle
     */
    public static List<Integer> getPositions(final int vehicleNumber, final IChromosome chromosome, final NodeGraph graph, final boolean order) {
        final List<Integer> route = new ArrayList<>();
        final List<Double> positions = new ArrayList<>();
        final int graphDimension = depot.GetNodesWithParcelsAssigned().size();
        for (int i = 1; i < graphDimension; ++i) {
            int chromosomeValue = (Integer) chromosome.getGene(i).getAllele();
            if (chromosomeValue == vehicleNumber) {
                route.add(i);
                positions.add((Double) chromosome.getGene(i + graphDimension).getAllele());
            }
        }

        if (order) {
            order(positions, route);
        }

        return route;

    }


    private static void order(List<Double> positions, List<Integer> route) {
        for (int i = 0; i < positions.size(); ++i) {//todo improve sorting
            for (int j = i + 1; j < positions.size(); ++j) {
                if (positions.get(i).compareTo(positions.get(j)) < 0) {
                    Collections.swap(positions, i, j);
                    Collections.swap(route, i, j);
                }
            }
        }
    }


}