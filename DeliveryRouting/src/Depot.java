import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Serializable;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public Depot(List<AID> trucksAtDepot)
    {
        this.trucksAtDepot = trucksAtDepot;
    }

    protected void setup(List<AID> trucks){
        trucksAtDepot = trucks;

        CyclicBehaviour listenForRouteQueries = new CyclicBehaviour(this) {
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
        addBehaviour(listenForRouteQueries);
    }

    public void GetConstraints() {
        Behaviour getConstraint = new Behaviour(this) {
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

        addBehaviour(getConstraint);
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

    public void CreateRoutes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void GiveParcels() {
        Behaviour giveParcels = new Behaviour(this) {
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

                            inform.setConversationId("Parcels");
                            inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique ID

                            inform.addReceiver(truck);
                            truckAmount++;

                            // Send inform
                            send(inform);
                        }

                        // Setup template to receive responses
                        mt = MessageTemplate.MatchConversationId("Parcels");

                        step = 1;
                        break;
                    case 1:

                        ACLMessage reply = receive(mt);
                        // Wait for replies and store their content
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                String answer = reply.getContent();
                                if (answer.equals("Yes")){
                                    responses.put(reply.getSender(), true);
                                } else{
                                    responses.put(reply.getSender(), false);
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

        addBehaviour(giveParcels);
    }

    public void GiveRoute(AID truck){
        throw new UnsupportedOperationException("Not implemented");
    }
}
