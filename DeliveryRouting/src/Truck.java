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


    private World world;
    private AID depotAID = null;
    private List<Node> route = new ArrayList<Node>();

    private List<Parcel> parcels = new ArrayList<Parcel>();
    private Node currentDestination;
    private Vector2 position;
    private float weightLimit;
    private float speed = 5f;

    private int step = 0;
    private MessageTemplate routeReplyTemplate = null;
    private MessageTemplate constraintRequestTemplate = null;
    private MessageTemplate parcelAllocationTemplate = null;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Basic Public Properties
    public Vector2 getPosition() { return position; }
    public float getWeightLimit() { return weightLimit; }

    //Complex Public Properties
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

    public void setRoute(List<Integer> value)
    {
        List<Node> route = new ArrayList<Node>();

        for (Integer i : value)
        {
            route.add(world.getGraph().getNodeWithID(i));
        }

        this.route = route;
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
        constraintRequestTemplate = MessageTemplate.MatchConversationId("Constraint_Request");
        parcelAllocationTemplate = MessageTemplate.MatchConversationId("Parcel_Allocation");
        CreateCyclicBehaviourUpdate();
    }

    private void CreateCyclicBehaviourUpdate()
    {
        CyclicBehaviour cyclicBehaviourUpdate = new CyclicBehaviour(this)
        {
            int count = 0;

            public void action()
            {
                count++;

                if (count > 5000) //Slow down agent execution / reduce the number of times they scream "I'm doing something!!!!" every second
                {
                    count -= 5000;
                    System.out.println(getLocalName() + ".Update().CheckForConstraintRequest()");
                    CheckForConstraintRequest();
                    System.out.println(getLocalName() + ".Update().CheckForParcelAllocation()");
                    CheckForParcelAllocation();

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
            }
        };

        addBehaviour(cyclicBehaviourUpdate);
        System.out.println(getLocalName() + ": update behaviour added");
    }

    private void CheckForConstraintRequest()
    {
        //Declare template and message variables
        ACLMessage msg = null;
        msg = receive(constraintRequestTemplate);

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
        String capacity = "" + weightLimit;
        reply.setContent(capacity);

        //Send reply
        send(reply);
        System.out.println(getLocalName() + ": received Constraint Request from " + request.getSender().getLocalName() + ", replied with constraints");
    }

    private void CheckForParcelAllocation()
    {
        //Declare template and message variables
        ACLMessage msg = null;
        boolean received = false;

        //Check for Parcel_Allocation
        msg = receive (parcelAllocationTemplate);

        if (msg != null)
        {
            //String serialization
            List<Parcel> newParcels = new ArrayList<Parcel>();
            String msgContent = msg.getContent();
            String[] parcelIDs = msgContent.split(":");

            for (String id : parcelIDs)
            {
                newParcels.add(world.getDepot().getParcelByID(Integer.parseInt(id)));
            }

            parcels.addAll(newParcels);
            received = true;
            System.out.println(getLocalName() + ": received " + newParcels.size() + " parcels. Now have " + parcels.size() + " parcels");

//            //Wouldn't accept lists of parcels
//            try
//            {
//                List<Parcel> newParcels = (List<Parcel>) msg.getContentObject();
//                received = true;
//
//                if (newParcels == null)
//                {
//                    System.out.println(getLocalName() + ": received list of parcels that was null");
//                }
//                else
//                {
//                    parcels.addAll(newParcels);
//                    System.out.println(getLocalName() + ": received " + newParcels.size() + " parcels. Now have " + parcels.size() + " parcels");
//                }
//            }
//            catch (UnreadableException e)
//            {
//                e.printStackTrace();
//            }

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
                                route = (List<Node>) reply.getContentObject();
                                received = true;

                                //Retrieve first destination from route
                                currentDestination = route.get(0);
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
                step++;
                System.out.println(getLocalName() + ": requesting route; setting requestingRoute to true");
            }
            else
            {
                currentDestination = route.get(0);
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

        System.out.println(getLocalName() + ": " + delivery.size() + " parcels delivered to Node " + currentDestination.id);
    }

    private void SendRouteRequest()
    {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(depotAID);
        request.setContent("Route");
        request.setConversationId("Route_Request");
        request.setReplyWith("request" + System.currentTimeMillis()); // Unique ID

        System.out.println(getLocalName() + ": sending route request to agent " + depotAID.getLocalName());

        // Send request
        send(request);

        // Setup template to receive responses
        routeReplyTemplate = MessageTemplate.MatchConversationId("Route_Request");

        step++;
    }

    private void ReceiveRouteReply()
    {
        // Wait for replies and store their content
        ACLMessage reply = receive(routeReplyTemplate);
        boolean received = false;

        if (reply != null)
        {
            try
            {
                //Serialize as strings
                String replyString = reply.getContent();
                String[] nodeIDs = replyString.split(":");
                Node previousNode = currentDestination;

                for (int i = 0, j = nodeIDs.length; i < j; i++)
                {
                    Node n = world.getNodeByID(Integer.parseInt(nodeIDs[i]));
                    route.add(n);
                    previousNode = n;
                }

//                        List<Road> receivedRoute = (List<Road>)reply.getContentObject();
//
//                        if (receivedRoute == null)
//                        {
//                            System.out.println(getLocalName() + ": WARNING: received null route");
//                        }
//                        else
//                        {
                //route.addAll(receivedRoute);
                System.out.println(getLocalName() + ": received route");
                received = true;

                //Retrieve first destination from route
                currentDestination = route.get(0);
                route.remove(0);
                //}
            }
            catch (Exception e)
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

            step = 0;
        }
//        else
//        {
//            block();
//        }
    }
}
