import com.google.gson.Gson;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.sniffer.Message;
import jade.util.leap.Serializable;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.*;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;
import org.jgap.*;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Depot extends Agent
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private World world;
    private Parcel[] parcels = new Parcel[30];
    private List<AID> trucksAtDepot;
    private Map<AID, Float> truckCapacity = new HashMap<AID, Float>();
    private Map<AID, List<Road>> routes = new HashMap<AID, List<Road>>();
    private Map<AID, List<Parcel>> truckParcels = new HashMap<AID, List<Parcel>>();
    private List<Node> unroutedNodes;
    private List<Node> nodesWithParcelsAssigned = new ArrayList<>();
    private int numTrucks;
    private static Logger log = LoggerFactory.getLogger(Depot.class);
    private static final int EVOLUTIONS = 2000;
    private static final int POPULATION_SIZE = 350;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public List<Node> GetNodesWithParcelsAssigned() { return nodesWithParcelsAssigned; }

    public int getNumTrucks() { return numTrucks; }

    public int getTotalDemand()
    {
        int totWeight = 0;

        for (Parcel p : parcels)
        {
            totWeight += p.getWeight();
        }

        return totWeight;
    }

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    public Depot(World world, List<AID> trucksAtDepot)
    {
        this.world = world;
        this.trucksAtDepot = trucksAtDepot;
        numTrucks = trucksAtDepot.size();
        this.world = world;
        GetParcels();

        for (Parcel p : parcels)
        {
            Node randNode = world.getRandomNode();
            p.setDestination(randNode);
            nodesWithParcelsAssigned.add(randNode);
            System.out.println(randNode.id);
        }
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    public void GetParcels()
    {
        try
        {
            String data = new String(Files.readAllBytes(Paths.get("Parcels.json")));
            Gson gson = new Gson();
            parcels = gson.fromJson(data, Parcel[].class);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void StartVRP() throws Exception
    {
        final Configuration configuration = new DefaultConfiguration();
        configuration.setPreservFittestIndividual(true);
        configuration.setFitnessFunction(new VrpFitnessFunc(world, this));
        configuration.setPopulationSize(POPULATION_SIZE);

        log.info("Loaded vrp configuration:\n" + world.toString());

        final int graphDimension = nodesWithParcelsAssigned.size();
        final Gene[] genes = new Gene[2 * graphDimension];

        for (int i = 0; i < graphDimension; i++)
        {
            genes[i] = new IntegerGene(configuration, 1, numTrucks);
            genes[i + graphDimension] = new DoubleGene(configuration, 0, graphDimension);//to keep order of nodes
        }

        configuration.setSampleChromosome(new Chromosome(configuration, genes));
        final Genotype population = Genotype.randomInitialGenotype(configuration);
        final Instant start = Instant.now();
        log.info("Generations: " + EVOLUTIONS);

        for (int i = 1; i <= EVOLUTIONS; i++)
        {
            if (i % 100 == 0)
            {
                final IChromosome bestSolution = population.getFittestChromosome();
                log.info("Best fitness after " + i + " evolutions: " + bestSolution.getFitnessValue());
                double total = 0;
                final List<Double> demands = new ArrayList<>();

                for (int j = 1; j <= numTrucks; ++j)
                {
                    final double distanceRoute = VrpFitnessFunc.computeTotalDistance(j - 1, bestSolution, world.getGraph());
                    final double demand = VrpFitnessFunc.computeTotalCoveredDemand(j, bestSolution, world.getGraph());
                    total += distanceRoute;
                    demands.add(demand);
                }

                log.info("Total distance: " + total);
                log.info("Covered demands: " + demands);
            }

            population.evolve();
        }

        log.info("Execution time: " + Duration.between(start, Instant.now()));

        final IChromosome bestSolution = population.getFittestChromosome();

        log.info("Best fitness: " + bestSolution.getFitnessValue());
        log.info("Result: ");

        for (int i = 0; i < 2 * graphDimension; i++)
        {
            log.info((i + 1) + ". " + bestSolution.getGene(i).getAllele());
        }

        double total = 0.0;

        for (int i = 1; i <= numTrucks; ++i)
        {
            List<Integer> route = VrpFitnessFunc.getPositions(i, bestSolution, world.getGraph(), true);
            route = formatRoute(route);
            final double distanceRoute = VrpFitnessFunc.computeTotalDistance(i - 1, bestSolution, world.getGraph());
            final double demand = VrpFitnessFunc.computeTotalCoveredDemand(i, bestSolution, world.getGraph());
            log.info("Vehicle #" + i + " :" + route);
            log.info("Distance: " + distanceRoute);
            log.info("Demand: " + demand);
            total += distanceRoute;

            world.getTrucks().get(i - 1).setRoute(route);
        }

        log.info("Total distance: " + total);
    }

    public int getNodeDemand(Node node)
    {
        int totalWeight = 0;

        for (Parcel p: parcels)
        {
            if (p.getDestination().id == node.id)
            {
                totalWeight += p.getWeight();
            }
        }

        return totalWeight;
    }

    public Parcel getParcelByID(int id)
    {
        for (Parcel p : parcels)
        {
            if (p.getID() == id)
            {
                return p;
            }
        }

        return null;
    }

    private BoolVar[] getColumn(BoolVar[][] array, int index)
    {
        BoolVar[] column = new BoolVar[array[0].length]; // Here I assume a rectangular 2D array!

        for(int i = 0; i < column.length; i++)
        {
            column[i] = array[i][index];
        }

        return column;
    }

    private List<Integer> formatRoute(List<Integer> list)
    {
        final List<Integer> result = new ArrayList<>(Collections.singletonList(0));//source node
        result.addAll(list.stream().collect(Collectors.toList()));

        for (int i = 1; i < result.size(); i++)
        {
            result.set(i, nodesWithParcelsAssigned.get(result.get(i)).id);
            //System.out.println(result.get(i));
        }

        return result;
    }

    protected void setup()
    {
        System.out.println("Depot: setup");
        //GetParcels();         //Threw an error
        CreateBehaviourRequestConstraints();
        //AssignParcels();  //Matches up trucks with parcels
        //CreateRoutes();
        CreateBehaviourAllocateParcels();     //Sends assigned parcels to the truck via messages
        CreateCyclicBehaviourCheckForRouteRequests();
    }

    public void CreateBehaviourRequestConstraints()
    {
        Behaviour behaviourRequestConstraints = new Behaviour(this)
        {
            private int truckAmount = 0;
            private int truckResponsesReceived = 0;
            private int step = 0;
            private MessageTemplate mt;

            public void action()
            {
                switch (step)
                {
                    case 0:
                        System.out.println(getLocalName() + ": Requesting Constraints");
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

                        // Ask all trucks at depot
                        for (AID truck : trucksAtDepot)
                        {
                            request.addReceiver(truck);
                            truckAmount++;
                        }

                        // Setup request values
                        request.setContent("Capacity");
                        request.setConversationId("Constraint_Request");
                        request.setReplyWith("request" + System.currentTimeMillis()); // Unique ID

                        // Send request
                        send(request);

                        // Setup template to receive responses
                        mt = MessageTemplate.MatchConversationId("Constraint_Request");

                        step = 1;
                        break;
                    case 1:
                        System.out.println(getLocalName() + ": Checking for Constraint Replies");
                        ACLMessage reply = receive(mt);

                        // Wait for replies and store their content
                        if (reply != null)
                        {
                            if (reply.getPerformative() == ACLMessage.INFORM)
                            {
                                if (truckCapacity.containsKey(reply.getSender()))
                                {
                                    truckCapacity.replace(reply.getSender(), Float.parseFloat(reply.getContent()));
                                }
                                else
                                {
                                    truckCapacity.put(reply.getSender(), Float.parseFloat(reply.getContent()));
                                }
                            }

                            System.out.println(getLocalName() + ": received Constraint Reply from " + reply.getSender().getLocalName());
                            truckResponsesReceived++;
                        }
                        else
                        {
                            System.out.println(getLocalName() + ": no Constraint Replies");
                            block();
                        }

                        if (truckResponsesReceived >= truckAmount)
                        {
                            System.out.println(getLocalName() + ": received all Constraint Replies");
                            step = 2;
                        }

                        break;
                }
            }

            public boolean done() {
                return step == 2;
            }
        };

        addBehaviour(behaviourRequestConstraints);
    }


//    private void AssignParcels(){
//        Model m = new Model("Parcel Assignment for Trucks");
//        int numParcels = parcels.length;
//        int numTrucks = trucksAtDepot.size();
//        BoolVar[][] assignment = m.boolVarMatrix(numParcels, numTrucks);
//
//        // Check num of trucks assigned to parcel = 1
//        for (int i = 0; i < numParcels; i++)
//        {
//            m.sum(assignment[i], "=", 1).post();
//        }
//
//        // Check total weight of parcels <= truck weight limit
//        for (int i = 0; i < numTrucks; i++)
//        {
//            m.sum(getColumn(assignment, i), "<=", Math.round(truckCapacity.get(trucksAtDepot.get(i)))).post();
//        }
//
//        Solver s = m.getSolver();
//        s.solve();
//
//        // Print solution
//        for (int i = 0; i < numParcels; i++)
//        {
//            for (int j = 0; j < numTrucks; j++)
//            {
//                System.out.print(assignment[i][j].getValue());
//            }
//            System.out.println();
//        }
//    }
//
//    private void AssignParcelsWithWeights(){
//        Model m = new Model("Parcel Assignment for Trucks");
//        int numParcels = parcels.length;
//        int numTrucks = trucksAtDepot.size();
//        BoolVar[][] assignment = m.boolVarMatrix(numParcels, numTrucks);
//
//        // Check num of trucks assigned to parcel = 1
//        for (int i = 0; i < numParcels; i++)
//        {
//            m.sum(assignment[i], "=", 1).post();
//        }
//
//        // Check total weight of parcels <= truck weight limit
//        for (int i = 0; i < numTrucks; i++)
//        {
//            IntVar[] weights = getColumn(assignment, 1);
//            for (int j = 0; j < numParcels; j++)
//            {
//                int weight = weights[j].getValue();
//                weight *= parcels[j].getWeight();
//                weights[j] = m.intVar(weight);
//            }
//
//            m.sum(weights, "<=", Math.round(truckCapacity.get(trucksAtDepot.get(i)))).post();
//        }
//
//        Solver s = m.getSolver();
//        s.solve();
//
//        // Print solution
//        for (int i = 0; i < numParcels; i++)
//        {
//            for (int j = 0; j < numTrucks; j++)
//            {
//                System.out.print(assignment[i][j].getValue());
//            }
//            System.out.println();
//        }
//    }
//
//    public void CreateRoutes()
//    {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    public void CreateBehaviourAllocateParcels()
    {
        Behaviour behaviourAllocateParcels = new Behaviour(this)
        {
            private int truckAmount = 0;
            private int truckResponsesReceived = 0;
            private int step = 0;
            private Map<AID, Boolean> responses = new HashMap<AID, Boolean>();
            private MessageTemplate mt;

            public void action()
            {
                switch (step)
                {
                    case 0:
                        System.out.println("Parcel allocation, step 1");

                        // Give to all trucks at depot
                        for (AID truck : trucksAtDepot)
                        {
                            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);

                            // Setup inform values
//                            try
//                            {
                                //Serialize as strings
                                String parcelIDs = "";
                                parcels[0] = new Parcel(0, 0);

                                for (int i = 0; i < parcels.length; i++)
                                {
                                    if (parcels[i] != null)
                                    {
                                        if (i > 0)
                                        {
                                            parcelIDs += ":";
                                        }

                                        parcelIDs += parcels[i].getID();
                                    }
                                }

                                inform.setContent(parcelIDs);

//                                //Won't accept parcels
//                                if (truckParcels.get(truck) == null || truckParcels.get(truck).size() == 0)
//                                {
//                                    List<Parcel> parcels = new ArrayList<Parcel>();
//                                    parcels.add(new Parcel(0, 10));
//                                    truckParcels.put(truck, parcels);
//                                }
//
//                                inform.setContentObject((java.io.Serializable) truckParcels.get(truck));
//                            }
//                            catch (IOException e)
//                            {
//                                e.printStackTrace();
//                            }

                            inform.setConversationId("Parcel_Allocation");
                            inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique ID

                            inform.addReceiver(truck);
                            truckAmount++;

                            // Send inform
                            send(inform);
                        }

                        // Setup template to receive responses
                        mt = MessageTemplate.MatchConversationId("Parcel_Allocation");

                        step = 1;
                        break;
                    case 1:
                        System.out.println("Parcel allocation, step 2");
                        ACLMessage reply = receive(mt);

                        // Wait for replies and store their content
                        if (reply != null)
                        {
                            if (reply.getPerformative() == ACLMessage.INFORM)
                            {
                                String answer = reply.getContent();
                                responses.put(reply.getSender(), answer.equals("Yes"));
                            }

                            truckResponsesReceived++;
                        }
                        else
                        {
                            block();
                        }

                        if (truckResponsesReceived >= truckAmount)
                        {
                            step = 2;
                        }

                        break;
                }
            }

            public boolean done() { return step == 2; }
        };

        addBehaviour(behaviourAllocateParcels);
    }

    private void CreateCyclicBehaviourCheckForRouteRequests()
    {
        CyclicBehaviour cyclicBehaviourCheckForRouteRequests = new CyclicBehaviour(this)
        {
            public void action()
            {
                System.out.println(getLocalName() + ": checking for route requests");
                // Match a request for a route
                MessageTemplate mt = MessageTemplate.MatchConversationId("Route_Request");
                ACLMessage request = receive(mt);

                if (request != null)
                {
                    //TODO: if no route, store request and return to when a route has been established for the requesting truck
                    System.out.println(getLocalName() + ": found route request");
                    ACLMessage reply = request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setConversationId(request.getConversationId());

                    // Send the route according to the AID of the truck
                    try
                    {
                        //Serialize as string
                        String nodeIDs = "";
                        List<Road> route = new ArrayList<>();
                        route.add(world.getRandomRoad());

                        for (int i = 0; i < route.size(); i++)
                        {
                            if (i > 0)
                            {
                                nodeIDs += ":";
                            }

                            nodeIDs += route.get(i).getDestination().getId();
                        }

                        reply.setContent(nodeIDs);

//                        //Won't accept roads
//                        if (routes.get(request.getSender()) == null || routes.get(request.getSender()).size() == 0)
//                        {
//                            List<Road> route = new ArrayList<Road>();
//                            route.add(world.getRandomRoad());
//                            routes.put(request.getSender(), route);
//                        }
//
//                        reply.setContentObject((java.io.Serializable)routes.get(request.getSender()));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    send(reply);
                }
                else
                {
                    System.out.println(getLocalName() + ": no route requests");
                    block();
                }
            }
        };

        addBehaviour(cyclicBehaviourCheckForRouteRequests);
        System.out.println("Depot: created check for route requests behaviour");
    }
}
