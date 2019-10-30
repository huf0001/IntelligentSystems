import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.List;

public class Truck extends Agent
{
    //Private Fields --------------------------------------------------------------------------------------------------------------------------------

    private World world;    //For testing
    private AID depotAID = null;
    private List<Road> route = new ArrayList<Road>();
    private List<Parcel> parcels = new ArrayList<Parcel>();
    private Node currentDestination;
    private Vector2 position;
    private float weightLimit;
    private float speed = 1f;
    private boolean requestingRoute;
    private int routeRequestStep = 0;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public Vector2 getPosition()
    {
        return position;
    }

    public void setDepotAID(AID value)
    {
//        if (value == null)
//        {
//            System.out.println(getLocalName() + ": received Depot AID; value received is null");
//        }
//        else
//        {
//            System.out.println(getLocalName() + ": received Depot AID; value received is valid");
//        }

        depotAID = value;

//        if (depotAID == null)
//        {
//            System.out.println(getLocalName() + ": received Depot AID; set AID is null");
//        }
//        else
//        {
//            System.out.println(getLocalName() + ": received Depot AID; set AID is valid");
//        }
    }

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    public Truck(World world, Node startNode, float weightLimit)
    {
        this.world = world;
        this.currentDestination = startNode;
        this.position = startNode.position;
        this.weightLimit = weightLimit;
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    protected void setup()
    {
        CreateCyclicBehaviourListening();
        CreateCyclicBehaviourRequestRoute();
    }

    private void CreateCyclicBehaviourListening()
    {
        CyclicBehaviour cyclicBehaviourListening = new CyclicBehaviour(this)
        {
            public void action()
            {
                System.out.println(getLocalName() + ": listening");
                CheckForConstraintRequest();
                CheckForParcelAllocation();
            }
        };

        addBehaviour(cyclicBehaviourListening);
    }

    private void CheckForConstraintRequest()
    {
        //Declare template and message variables
        MessageTemplate mt;
        ACLMessage msg = null;

        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId("Constraint_Request"));
        msg = receive(mt);

        if (msg != null)
        {
            //Reply with weight limit, then reset msg
            GiveConstraints(msg);
            msg = null;
        }
    }

    private void GiveConstraints (ACLMessage request)
    {
        //Create reply
        ACLMessage reply = request.createReply();

        //Set reply "metadata"
        reply.setPerformative(ACLMessage.INFORM);
        reply.setInReplyTo(request.getInReplyTo());
        reply.setConversationId(request.getConversationId());

        //Set reply content
        String capacity = "%d", weightLimit;
        reply.setContent(capacity);

        //Send reply
        send(reply);
    }

    private void CheckForParcelAllocation()
    {
        //Declare template and message variables
        MessageTemplate mt;
        ACLMessage msg = null;
        boolean received = false;

        //Check for Parcel_Allocation
        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("Parcel_Allocation"));
        msg = receive (mt);

        if (msg != null)
        {
            try
            {
                parcels = (List<Parcel>) msg.getContentObject();
                received = true;
            }
            catch (UnreadableException e)
            {
                e.printStackTrace();
            }

            //Create reply
            ACLMessage reply = msg.createReply();

            //Set reply "metadata"
            reply.setPerformative(ACLMessage.INFORM);
            reply.setInReplyTo(msg.getInReplyTo());
            reply.setConversationId(msg.getConversationId());

            //Set reply content
            String outcome = received ? "Yes" : "No";
            reply.setContent(outcome);

            //Send reply
            send(reply);

            msg = null;
        }
    }

    private void CreateCyclicBehaviourRequestRoute()
    {
        CyclicBehaviour cyclicBehaviourRequestRoute = new CyclicBehaviour(this)
        {
            //private int step;
            private MessageTemplate mt;

            public void action()
            {
                System.out.println(getLocalName() + ".BehaviourRequestRoute");
                switch (routeRequestStep) {
                    case 0:
                        block();
                        break;
                    case 1:
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

                        System.out.println(getLocalName() + ": sending route request to agent " + depotAID.getLocalName());

                        // Setup request values
                        request.addReceiver(depotAID);
                        request.setContent("Route");
                        request.setConversationId("Route_Request");
                        request.setReplyWith("request" + System.currentTimeMillis()); // Unique ID

                        // Send request
                        send(request);

                        // Setup template to receive responses
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Route_Request"),
                                MessageTemplate.MatchInReplyTo(request.getReplyWith()));

                        routeRequestStep++;
                        break;
                    case 2:
                        // Wait for replies and store their content
                        ACLMessage reply = receive(mt);
                        boolean received = false;

                        if (reply != null)
                        {
                            try
                            {
                                //Store route
                                route = (List<Road>) reply.getContentObject();
                                received = true;

                                //Retrieve first destination from route
                                currentDestination = route.get(0).getDestination();
                                route.remove(0);
                            }
                            catch (UnreadableException e)
                            {
                                e.printStackTrace();
                            }

                            //Create confirmation message
                            ACLMessage confirmation = reply.createReply();

                            //Set confirmation "metadata"
                            confirmation.setPerformative(ACLMessage.INFORM);
                            confirmation.setInReplyTo(reply.getInReplyTo());
                            confirmation.setConversationId(reply.getConversationId());

                            //Set confirmation content
                            String outcome = received ? "Yes" : "No";
                            confirmation.setContent(outcome);

                            //Send confirmation
                            send(confirmation);

                            routeRequestStep = 0;
                            requestingRoute = false;
                        }
                        else
                        {
                            block();
                        }

                        break;
                }
            }

//            public boolean done()
//            {
//                return routeRequestStep == 3;
//            }
        };

        addBehaviour(cyclicBehaviourRequestRoute);
        System.out.println(getLocalName() + ": route request behaviour added");
    }

    public void GoToNextNode ()
    {
        double distance = Vector2.distance(position, currentDestination.position);
        Vector2 toCurrentNode = currentDestination.position.minus(position);

        toCurrentNode.normalize();
        toCurrentNode = toCurrentNode.multiply(speed < distance ? speed : distance);
        position = position.add(toCurrentNode);

        if (depotAID == null)
        {
            System.out.println(getLocalName() + ": depotAID is null");
        }

        if (Vector2.distance(position, currentDestination.position) == 0 && !requestingRoute && depotAID != null)
        {
            DeliverParcels();

            if (route.size() == 0)
            {
                //route.add(world.getRandomRoad(currentDestination));//For testing

                //TODO: request new route
                requestingRoute = true;
                routeRequestStep = 1;
                //CreateCyclicBehaviourRequestRoute();
                System.out.println(getLocalName() + ": requesting route");
            }
            else
            {
                currentDestination = route.get(0).getDestination();
                route.remove(0);

                System.out.println(getLocalName() + ": New destination: Node " + currentDestination.id);
            }
        }
    }

    private void DeliverParcels()
    {
        List<Parcel> delivery = new ArrayList<Parcel>();

        for (Parcel p : parcels)
        {
            if (p.getDestination() == currentDestination)
            {
                delivery.add(p);
            }
        }

        parcels.removeAll(delivery);
        currentDestination.DeliverParcels(delivery);
        System.out.println(getLocalName() + ": " + parcels.size() + " parcels delivered to Node " + currentDestination.id);
    }
}
