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

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Depot extends Agent
{
    private World world;
    private Parcel[] parcels = new Parcel[30];
    private List<AID> trucksAtDepot;
    private Map<AID, Float> truckCapacity = new HashMap<AID, Float>();
    private Map<AID, List<Road>> routes = new HashMap<AID, List<Road>>();
    private Map<AID, List<Parcel>> truckParcels = new HashMap<AID, List<Parcel>>();
    private List<Node> unroutedNodes;
    private List<Node> routedNodes;

    public Depot(World world, List<AID> trucksAtDepot)
    {
        this.world = world;
        this.trucksAtDepot = trucksAtDepot;

    }

    private BoolVar[] getColumn(BoolVar[][] array, int index){
        BoolVar[] column = new BoolVar[array[0].length]; // Here I assume a rectangular 2D array!
        for(int i = 0; i < column.length; i++){
            column[i] = array[i][index];
        }
        return column;
    }

    protected void setup()
    {
        System.out.println("Depot: setup");
        //GetParcels();         //Threw an error
        CreateBehaviourRequestConstraints();
        //AssignParcels();  //Matches up trucks with parcels
        //CreateRoutes();
        //CreateBehaviourAllocateParcels();     //Sends assigned parcels to the truck via messages

        CreateCyclicBehaviourCheckForRouteRequests();
    }

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

    private void AssignParcels(){
        Model m = new Model("Parcel Assignment for Trucks");
        int numParcels = parcels.length;
        int numTrucks = trucksAtDepot.size();
        BoolVar[][] assignment = m.boolVarMatrix(numParcels, numTrucks);

        // Check num of trucks assigned to parcel = 1
        for (int i = 0; i < numParcels; i++)
        {
            m.sum(assignment[i], "=", 1).post();
        }

        // Check total weight of parcels <= truck weight limit
        for (int i = 0; i < numTrucks; i++)
        {
            m.sum(getColumn(assignment, i), "<=", Math.round(truckCapacity.get(trucksAtDepot.get(i)))).post();
        }

        Solver s = m.getSolver();
        s.solve();

        // Print solution
        for (int i = 0; i < numParcels; i++)
        {
            for (int j = 0; j < numTrucks; j++)
            {
                System.out.print(assignment[i][j].getValue());
            }
            System.out.println();
        }
    }

    private void AssignParcelsWithWeights(){
        Model m = new Model("Parcel Assignment for Trucks");
        int numParcels = parcels.length;
        int numTrucks = trucksAtDepot.size();
        BoolVar[][] assignment = m.boolVarMatrix(numParcels, numTrucks);

        // Check num of trucks assigned to parcel = 1
        for (int i = 0; i < numParcels; i++)
        {
            m.sum(assignment[i], "=", 1).post();
        }

        // Check total weight of parcels <= truck weight limit
        for (int i = 0; i < numTrucks; i++)
        {
            IntVar[] weights = getColumn(assignment, 1);
            for (int j = 0; j < numParcels; j++)
            {
                int weight = weights[j].getValue();
                weight *= parcels[j].getWeight();
                weights[j] = m.intVar(weight);
            }

            m.sum(weights, "<=", Math.round(truckCapacity.get(trucksAtDepot.get(i)))).post();
        }

        Solver s = m.getSolver();
        s.solve();

        // Print solution
        for (int i = 0; i < numParcels; i++)
        {
            for (int j = 0; j < numTrucks; j++)
            {
                System.out.print(assignment[i][j].getValue());
            }
            System.out.println();
        }
    }

    public void CreateRoutes()
    {
        throw new UnsupportedOperationException("Not implemented");
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
        CyclicBehaviour cyclicBehaviourCheckForRouteRequests = new CyclicBehaviour(this)
        {
            public void action()
            {
                System.out.println(getLocalName() + ": checking for route requests");
                // Match a request for a route
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("Route_Request"));
                ACLMessage request = receive(mt);

                if (request != null)
                {
                    System.out.println(getLocalName() + ": found route request");
                    ACLMessage reply = request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setConversationId(request.getConversationId());

                    // Send the route according to the AID of the truck
                    try
                    {
                        //reply.setContentObject((Serializable) routes.get(request.getSender()));
                        List<Road> route = new ArrayList<Road>();
                        route.add(world.getRandomRoad());
                        reply.setContentObject((Serializable)route);
                    }
                    catch (IOException e)
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

    //Not used; currently obsolesced by CreateCyclicBehaviourCheckForRouteRequests()
    public void GiveRoute(AID truck){
        throw new UnsupportedOperationException("Not implemented");
    }
}
