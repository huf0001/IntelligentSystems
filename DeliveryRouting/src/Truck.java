import jade.core.AID;
import jade.core.Agent;
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

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public Vector2 getPosition()
    {
        return position;
    }

    public void setDepotAID(AID value)
    {
        if (value == null)
        {
            System.out.println(getLocalName() + ": received Depot AID; value received is null");
        }
        else
        {
            System.out.println(getLocalName() + ": received Depot AID; value received is valid");
        }

        depotAID = value;

        if (depotAID == null)
        {
            System.out.println(getLocalName() + ": received Depot AID; set AID is null");
        }
        else
        {
            System.out.println(getLocalName() + ": received Depot AID; set AID is valid");
        }
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
        System.out.println(getLocalName() + ": setup");
        CreateCyclicBehaviourListening();
        CreateCyclicBehaviourUpdate();
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

    private void CreateCyclicBehaviourUpdate()
    {
        CyclicBehaviour cyclicBehaviourUpdate = new CyclicBehaviour(this)
        {
            private int step = 0;
            private MessageTemplate mt;

            public void action()
            {
                switch (step)
                {
                    case 0:
                        System.out.println(getLocalName() + ".Update().Move()");
                        Move();
                        break;
                    case 1:
                        System.out.println(getLocalName() + ".Update().SendRouteRequest()");
                        SendRouteRequest();
                        break;
                    case 2:
                        System.out.println(getLocalName() + ".Update().ReceiveRouteReply()");
                        ReceiveRouteReply();
                        break;
                }
            }

            private void Move()
            {
                double distance = Vector2.distance(position, currentDestination.position);
                Vector2 toCurrentNode = currentDestination.position.minus(position);

                toCurrentNode.normalize();
                toCurrentNode = toCurrentNode.multiply(speed < distance ? speed : distance);
                position = position.add(toCurrentNode);

                if (Vector2.distance(position, currentDestination.position) == 0 && depotAID != null)
                {
                    DeliverParcels();

                    if (route.size() == 0)
                    {
                        //route.add(world.getRandomRoad(currentDestination));//For testing

                        //TODO: request new route
                        step++;
                        System.out.println(getLocalName() + ": requesting route; setting requestingRoute to true");
                    }
                    else
                    {
                        currentDestination = route.get(0).getDestination();
                        route.remove(0);

                        System.out.println(getLocalName() + ": New destination: Node " + currentDestination.id);
                    }
                }
                else if (depotAID != null)
                {
                    System.out.println(getLocalName() + ".Move(): waiting for route request to be resolved");
                }
                else
                {
                    System.out.println(getLocalName() + ".Move(): waiting for depotAID");
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

            private void SendRouteRequest()
            {
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

                step++;
            }

            private void ReceiveRouteReply()
            {
                // Wait for replies and store their content
                ACLMessage reply = receive(mt);
                boolean received = false;

                if (reply != null)
                {
                    try
                    {
                        //Store route
//                        Object routeObject = (Object)reply.getContentObject();
//                        Road[] temp = (Road[])routeObject;
//
//                        for (Road r : temp)
//                        {
//                            route.add(r);
//                        }

                        route = (List<Road>)reply.getContentObject();

                        received = true;

                        //Retrieve first destination from route
                        currentDestination = route.get(0).getDestination();
                        route.remove(0);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    System.out.println(getLocalName() + ": received route");
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

                    step = 0;
                }
                else
                {
                    block();
                }
            }
        };

        addBehaviour(cyclicBehaviourUpdate);
        System.out.println(getLocalName() + ": update behaviour added");
    }
}
