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
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Depot extends Agent{
    private World world;
    private List<Parcel> parcels;
    private List<AID> trucksAtDepot;
    private Map<AID, Float> truckCapacity = new HashMap<AID, Float>();
    private Map<AID, List<Road>> routes = new HashMap<AID, List<Road>>();
    private List<Node> unroutedNodes;
    private List<Node> routedNodes;

    public Depot(List<AID> trucksAtDepot)
    {
        this.trucksAtDepot = trucksAtDepot;
    }

    private void AssignParcels(){
        int numTrucks = trucksAtDepot.size();
        int numParcels = parcels.size();

        int[][] weights = {
                {1, 10},
                {1, 10},
                {1}
        }

        Model m = new Model("Parcel Assignment for Truck_" + j);
        IntVar[] vars = new IntVar[parcels.size()];
        for (int i = 0; i < parcels.size(); i++)
        {
            vars[i] = m.intVar("Parcel_" + i, 0, parcels.get(i).weight);
        }

        for (int i = 0; i < vars.length; i++)
        {
            m.arithm(vars[i], "<", 1);
            m.arithm(vars[i], ">", parcels.get(i).weight - 1);
        }

        m.sum(vars, "<=", 10).post();
        m.sum(vars, ">", 0).post();
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

    public List<Parcel> GetParcels(){
        throw new UnsupportedOperationException("Not implemented");
    }

    public void CreateRoutes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void GiveRoute(AID truck){
        throw new UnsupportedOperationException("Not implemented");
    }
}
