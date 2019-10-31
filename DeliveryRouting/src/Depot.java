import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Serializable;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;
import org.jgap.*;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Depot extends Agent
{
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

    public Depot(List<AID> trucksAtDepot, World world)
    {
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

    public void StartVRP() throws Exception {
        final Configuration configuration = new DefaultConfiguration();
        configuration.setPreservFittestIndividual(true);
        configuration.setFitnessFunction(new VrpFitnessFunc(world, this));
        configuration.setPopulationSize(POPULATION_SIZE);

        log.info("Loaded vrp configuration:\n" + world.toString());

        final int graphDimension = nodesWithParcelsAssigned.size();
        final Gene[] genes = new Gene[2 * graphDimension];
        for (int i = 0; i < graphDimension; i++) {
            genes[i] = new IntegerGene(configuration, 1, numTrucks);
            genes[i + graphDimension] = new DoubleGene(configuration, 0, graphDimension);//to keep order of nodes
        }

        configuration.setSampleChromosome(new Chromosome(configuration, genes));
        final Genotype population = Genotype.randomInitialGenotype(configuration);

        final Instant start = Instant.now();
        log.info("Generations: " + EVOLUTIONS);
        for (int i = 1; i <= EVOLUTIONS; i++) {
            if (i % 100 == 0) {
                final IChromosome bestSolution = population.getFittestChromosome();
                log.info("Best fitness after " + i + " evolutions: " + bestSolution.getFitnessValue());
                double total = 0;
                final List<Double> demands = new ArrayList<>();
                for (int j = 1; j <= numTrucks; ++j) {
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
        for (int i = 0; i < 2 * graphDimension; i++) {
            log.info((i + 1) + ". " + bestSolution.getGene(i).getAllele());
        }

        double total = 0.0;

        for (int i = 1; i <= numTrucks; ++i) {
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

    public int getTotalDemand()
    {
        int totWeight = 0;

        for (Parcel p : parcels)
        {
            totWeight += p.getWeight();
        }

        return totWeight;
    }

    public List<Node> GetNodesWithParcelsAssigned()
    {
        return nodesWithParcelsAssigned;
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

    private List<Integer> formatRoute(List<Integer> list) {
        final List<Integer> result = new ArrayList<>(Collections.singletonList(0));//source node
        result.addAll(list.stream().collect(Collectors.toList()));

        for (int i = 1; i < result.size(); i++)
        {
            result.set(i, nodesWithParcelsAssigned.get(result.get(i)).id);
            //System.out.println(result.get(i));
        }

        return result;
    }

    protected void setup(List<AID> trucks){
        trucksAtDepot = trucks;

        //GetParcels();
        //CreateBehaviourRequestConstraints
        //AssignParcels();  //Matches up trucks with parcels
        //CreateRoutes();
        //CreateBehaviourAllocateParcels();     //Sends assigned parcels to the truck via messages
        //CreateBehaviourRequestConstraints();

        CreateCyclicBehaviourCheckForRouteRequests();
    }

    public int getNumTrucks() { return numTrucks; }

    public void GetParcels(){
        try {
            String data = new String(Files.readAllBytes(Paths.get("Parcels.json")));
            Gson gson = new Gson();
            parcels = gson.fromJson(data, Parcel[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void CreateBehaviourRequestConstraints() {
        Behaviour behaviourRequestConstraints = new Behaviour(this) {
            private int truckAmount = 0;
            private int truckResponsesReceived = 0;
            private int step = 0;
            private MessageTemplate mt;

            public void action() {
                switch (step) {
                    case 0:
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

                        // Ask all trucks at depot
                        for (AID truck : trucksAtDepot) {
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
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Constraint_Request"),
                                MessageTemplate.MatchInReplyTo(request.getReplyWith()));

                        step = 1;
                        break;
                    case 1:
                        ACLMessage reply = receive(mt);
                        // Wait for replies and store their content
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                if (truckCapacity.containsKey(reply.getSender())) {
                                    truckCapacity.replace(reply.getSender(), Float.parseFloat(reply.getContent()));
                                } else {
                                    truckCapacity.put(reply.getSender(), Float.parseFloat(reply.getContent()));
                                }
                            }
                            truckResponsesReceived++;
                        } else {
                            block();
                        }

                        if (truckResponsesReceived >= truckAmount) {
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

    public void CreateBehaviourAllocateParcels() {
        Behaviour behaviourAllocateParcels = new Behaviour(this) {
            private int truckAmount = 0;
            private int truckResponsesReceived = 0;
            private int step = 0;
            private Map<AID, Boolean> responses = new HashMap<AID, Boolean>();
            private MessageTemplate mt;

            public void action() {
                switch (step) {
                    case 0:
                        // Give to all trucks at depot
                        for (AID truck : trucksAtDepot)
                        {
                            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);

                            // Setup inform values
                            try
                            {
                                inform.setContentObject((java.io.Serializable) truckParcels.get(truck));
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }

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

                        ACLMessage reply = receive(mt);
                        // Wait for replies and store their content
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                String answer = reply.getContent();
                                responses.put(reply.getSender(), answer.equals("Yes"));
//                                if (answer.equals("Yes")){
//                                    responses.put(reply.getSender(), true);
//                                } else{
//                                    responses.put(reply.getSender(), false);
//                                }
                            }
                            truckResponsesReceived++;
                        } else {
                            block();
                        }

                        if (truckResponsesReceived >= truckAmount) {
                            step = 2;
                        }
                        break;
                }
            }

            public boolean done() {
                return step == 2;
            }
        };

        addBehaviour(behaviourAllocateParcels);
    }

    private void CreateCyclicBehaviourCheckForRouteRequests()
    {
        CyclicBehaviour cyclicBehaviourCheckForRouteRequests = new CyclicBehaviour(this) {
            public void action() {
                // Match a request for a route
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("Route_Request"));
                ACLMessage request = receive(mt);

                if (request != null) {
                    ACLMessage reply = request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setConversationId(request.getConversationId());

                    // Send the route according to the AID of the truck
                    try
                    {
                        reply.setContentObject((Serializable) routes.get(request.getSender()));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    send(reply);
                } else {
                    block();
                }
            }
        };
        addBehaviour(cyclicBehaviourCheckForRouteRequests);
    }

    //Not used; currently obsolesced by CreateCyclicBehaviourCheckForRouteRequests()
    public void GiveRoute(AID truck){
        throw new UnsupportedOperationException("Not implemented");
    }
}
