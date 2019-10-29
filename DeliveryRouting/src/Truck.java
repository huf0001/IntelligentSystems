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
    private AID truckDepot;
    private List<Road> route = new ArrayList<Road>();
    private List<Parcel> parcels = new ArrayList<Parcel>();
    private Node currentDestination;
    private Vector2 position;
    private float weightLimit;
    private float speed = 1f;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public Vector2 getPosition()
    {
        return position;
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
        CreateBehaviourRequestRoute();
    }

    private void CreateCyclicBehaviourListening()
    {
        CyclicBehaviour cyclicBehaviourListening = new CyclicBehaviour(this)
        {
            public void action()
            {
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

    private void CreateBehaviourRequestRoute()
    {
        Behaviour behaviourRequestRoute = new Behaviour(this)
        {
            private int step;
            private MessageTemplate mt;

            public void action()
            {
                switch (step) {
                    case 0:
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

                        // Setup request values
                        request.addReceiver(truckDepot);
                        request.setContent("Route");
                        request.setConversationId("Route_Request");
                        request.setReplyWith("request" + System.currentTimeMillis()); // Unique ID

                        // Send request
                        send(request);

                        // Setup template to receive responses
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Route_Request"),
                                MessageTemplate.MatchInReplyTo(request.getReplyWith()));

                        step++;
                        break;
                    case 1:
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

                            step++;
                        }
                        else
                        {
                            block();
                        }

                        break;
                }
            }

            public boolean done()
            {
                return step == 2;
            }
        };

        addBehaviour(behaviourRequestRoute);
    }

    public void GoToNextNode ()
    {
        double distance = Vector2.distance(position, currentDestination.position);
        Vector2 toCurrentNode = currentDestination.position.minus(position);

        toCurrentNode.normalize();
        toCurrentNode = toCurrentNode.multiply(speed < distance ? speed : distance);
        position = position.add(toCurrentNode);

        if (Vector2.distance(position, currentDestination.position) == 0)
        {
            DeliverParcels();

            if (route.size() == 0)
            {
                currentDestination = world.getRandomNode();//For testing
                System.out.println(getLocalName() + ": New destination: Node " + currentDestination.id);

                //TODO: If haven't asked for new route, ask for new route
            }
            else
            {
                currentDestination = route.get(0).getDestination();
                route.remove(0);
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
