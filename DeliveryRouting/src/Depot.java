import com.google.gson.Gson;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
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

import org.chocosolver.solver.variables.BoolVar;
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
    private Map<AID, List<Parcel>> truckParcels = new HashMap<AID, List<Parcel>>();
    private List<Parcel> allParcels = new ArrayList<Parcel>();
    private List<Parcel> unallocatedParcels = new ArrayList<Parcel>();

    private int numTrucks;
    private List<AID> trucksAtDepot;
    private Map<AID, Float> truckCapacity = new HashMap<AID, Float>();

    private Map<AID, List<Integer>> routes = new HashMap<AID, List<Integer>>();
    private List<ACLMessage> pendingRouteRequests = new ArrayList<>();
    private List<Node> nodesWithParcelsAssigned = new ArrayList<>();

    private static Logger log = LoggerFactory.getLogger(Depot.class);
    private static final int EVOLUTIONS = 2000;
    private static final int POPULATION_SIZE = 350;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Basic Public Properties
    public List<Node> getNodesWithParcelsAssigned() { return nodesWithParcelsAssigned; }
    public int getNumTrucks() { return numTrucks; }

    //Complex Public Properties
    public int getTotalDemand()
    {
        int totWeight = 0;

        for (Parcel p : allParcels)
        {
            totWeight += p.getWeight();
        }

        return totWeight;
    }

    //Pseudo Public Properties

    public int getNodeDemand(Node node)
    {
        int totalWeight = 0;

        for (Parcel p: allParcels)
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
        for (Parcel p : allParcels)
        {
            if (p.getID() == id)
            {
                return p;
            }
        }

        return null;
    }

    //Setup Methods----------------------------------------------------------------------------------------------------------------------------------

    public Depot(World world, List<AID> trucksAtDepot)
    {
        this.world = world;
        this.trucksAtDepot = trucksAtDepot;
        numTrucks = trucksAtDepot.size();
        this.world = world;
        GetParcels();

        List<Integer> assignedIDs = new ArrayList<>();
        for (Parcel p : allParcels)
        {
            Node randNode = world.getRandomNode();
            while (assignedIDs.contains(randNode.id)){
                randNode = world.getRandomNode();
            }

            p.setDestination(randNode);
            nodesWithParcelsAssigned.add(randNode);
            assignedIDs.add(randNode.id);
            System.out.println(randNode.id);
        }

        for (AID truckAID : trucksAtDepot)
        {
            truckParcels.put(truckAID, new ArrayList<Parcel>());
        }
    }

    private void GetParcels()
    {
        try
        {
            String data = new String(Files.readAllBytes(Paths.get("Parcels.json")));
            Gson gson = new Gson();
            Parcel[] parcelArray = gson.fromJson(data, Parcel[].class);

            for (Parcel parcel : parcelArray)
            {
                allParcels.add(parcel);
                unallocatedParcels.add(parcel);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    protected void setup()
    {
        System.out.println("Depot: setup");
        CreateBehaviourRequestConstraints();
        //AssignParcels();  //Matches up trucks with parcels
        CreateBehaviourAllocateParcels();     //Sends assigned parcels to the truck via messages
        CreateCyclicBehaviourHandleRouteRequests();
    }

    //Vehicle Routing Methods------------------------------------------------------------------------------------------------------------------------

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

            AID truckAID = world.getTrucks().get(i - 1).getAID();
            routes.put(truckAID, route);
            List<Parcel> allocatedParcels = new ArrayList<Parcel>();


            for (Parcel parcel : unallocatedParcels)
            {
                for (Integer nodeID : route)
                {
                    if (parcel.getDestination().getId() == nodeID)
                    {
                        allocatedParcels.add(parcel);
                        break;
                    }
                }
            }

            unallocatedParcels.removeAll(allocatedParcels);
            truckParcels.get(truckAID).addAll(allocatedParcels);
            log.info("Parcels allocated: " + allocatedParcels.size());
        }

        log.info("Total distance: " + total);
        log.info("All parcels: " + allParcels.size());
        log.info("Allocated parcels: " + (allParcels.size() - unallocatedParcels.size()));
        log.info("Unallocated parcels: " + unallocatedParcels.size());
    }

    public void SimpleWaypoints() {
        List<Node> nodes = new ArrayList<>();
        //nodes.addAll(world.getGraph().adjNodes.keySet());
        nodes.addAll(nodesWithParcelsAssigned);
        nodes.add(0, world.getNodeByID(0));
        SimpleWaypointGen gen = new SimpleWaypointGen(nodes, numTrucks);
        List<List<Node>> genRoutes = gen.GenerateRoutes();

        int count = 0;
        for (List<Node> nodeList : genRoutes){
            TravelingSalesman salesman = new TravelingSalesman();
            salesman.SetRoute(nodeList);
            List<Integer> route = salesman.SimulateAnnealing(1000, 10000, 0.003);

            route.add(0);

            AID truckAID = world.getTrucks().get(count).getAID();
            routes.put(truckAID, route);
            List<Parcel> allocatedParcels = new ArrayList<Parcel>();

            count++;

            for (Parcel parcel : unallocatedParcels)
            {
                for (Integer nodeID : route)
                {
                    if (parcel.getDestination().getId() == nodeID)
                    {
                        allocatedParcels.add(parcel);
                        break;
                    }
                }
            }
            unallocatedParcels.removeAll(allocatedParcels);
            truckParcels.get(truckAID).addAll(allocatedParcels);
        }
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

        result.add(0);

        return result;
    }

    //Constraint Collection Handling-----------------------------------------------------------------------------------------------------------------

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
                        //System.out.println(getLocalName() + ": Requesting Constraints");
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
                        //System.out.println(getLocalName() + ": Checking for Constraint Replies");
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

                            //System.out.println(getLocalName() + ": received Constraint Reply from " + reply.getSender().getLocalName());
                            truckResponsesReceived++;
                        }
                        else
                        {
                            //System.out.println(getLocalName() + ": no Constraint Replies");
                            block();
                        }

                        if (truckResponsesReceived >= truckAmount)
                        {
                            //System.out.println(getLocalName() + ": received all Constraint Replies");
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

    //Parcel Distribution Handling-------------------------------------------------------------------------------------------------------------------

    public void CreateBehaviourAllocateParcels()
    {
        Behaviour behaviourAllocateParcels = new Behaviour(this)
        {
            private int truckAmount = 0;
            private int truckResponsesReceived = 0;
            private int step = 0;
            private Map<AID, Boolean> responses = new HashMap<AID, Boolean>();
            private MessageTemplate parcelAllocationTemplate;

            public void action()
            {
                switch (step)
                {
                    case 0:
                        //System.out.println("Parcel allocation, step 1");

                        // Give to all trucks at depot
                        for (AID truck : trucksAtDepot)
                        {
                            //Create message
                            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                            inform.setConversationId("Parcel_Allocation");
                            inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique ID
                            inform.addReceiver(truck);
                            truckAmount++;

                            //Serialize parcels as string
                            String parcelIDs = "";
                            List<Parcel> allocatedParcels = truckParcels.get(truck);

                            for (int i = 0; i < allocatedParcels.size(); i++)
                            {
                                if (allocatedParcels.get(i) != null)
                                {
                                    if (i > 0)
                                    {
                                        parcelIDs += ":";
                                    }

                                    parcelIDs += allocatedParcels.get(i).getID();
                                }
                            }

                            inform.setContent(parcelIDs);

                            //Send message
                            send(inform);
                        }

                        // Setup template to receive responses
                        parcelAllocationTemplate = MessageTemplate.MatchConversationId("Parcel_Allocation");

                        step = 1;
                        break;
                    case 1:
                        //System.out.println("Parcel allocation, step 2");
                        ACLMessage reply = receive(parcelAllocationTemplate);

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

    //Route Request Handling-------------------------------------------------------------------------------------------------------------------------

    private void CreateCyclicBehaviourHandleRouteRequests()
    {

        CyclicBehaviour cyclicBehaviourHandleRouteRequests = new CyclicBehaviour(this)
        {
            private int count = 0;

            public void action()
            {
                count++;

                if (count > 5000) //Slow down agent execution / reduce the number of times they scream "I'm doing something!!!!" every second
                {
                    count -= 5000;
                    HandleQueuedRouteRequests();
                    HandleNewRouteRequests();
                }
            }
        };

        addBehaviour(cyclicBehaviourHandleRouteRequests);
        //System.out.println("Depot: created cyclicBehaviourHandleRouteRequests");
    }

    private void HandleQueuedRouteRequests()
    {
        //System.out.println(getLocalName() + ": checking queued route requests");
        List<ACLMessage> completedRequests = new ArrayList<>();
        List<List<Integer>> dispatchedRoutes = new ArrayList<>();

        for (ACLMessage request : pendingRouteRequests)
        {
            List<Integer> route = routes.get(request.getSender());

            if (route != null && route.size() > 0)
            {
                //System.out.println(getLocalName() + ": found route for route request; replying to route request");
                ReplyToRouteRequest(request, route);
                completedRequests.add(request);
                dispatchedRoutes.add(route);
            }
        }

        pendingRouteRequests.removeAll(completedRequests);

        for (List<Integer> route : dispatchedRoutes)
        {
            route.clear();
        }
    }

    private void HandleNewRouteRequests()
    {
        //System.out.println(getLocalName() + ": checking for new route requests");

        // Match a request for a route
        MessageTemplate routeRequestTemplate = MessageTemplate.MatchConversationId("Route_Request");
        ACLMessage request;

        do
        {
            request = receive(routeRequestTemplate);

            if (request != null)
            {
                //System.out.println(getLocalName() + ": received new route request");
                List<Integer> route = routes.get(request.getSender());

                if (route == null || route.size() == 0)
                {
                    //System.out.println(getLocalName() + ": no route to reply with; queuing route request");
                    pendingRouteRequests.add(request);
                }
                else
                {
                    ReplyToRouteRequest(request, route);
                }
            }
            else
            {
                //System.out.println(getLocalName() + ": received no further route requests");
                //block();      //If need block, move RouteRequest handling methods back into cyclicBehaviourHandleRouteRequests
            }
        } while (request != null);
    }

    private void ReplyToRouteRequest(ACLMessage request, List<Integer> route)
    {
        ACLMessage reply = request.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setConversationId(request.getConversationId());

        // Send the route according to the AID of the truck; serialize as string
        String nodeIDs = "";

        for (int i = 0; i < route.size(); i++)
        {
            if (i > 0)
            {
                nodeIDs += ":";
            }

            nodeIDs += route.get(i);
        }

        reply.setContent(nodeIDs);

        send(reply);
    }
}
