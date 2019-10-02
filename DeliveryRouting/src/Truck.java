import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Truck extends Agent
{
    //Private Fields --------------------------------------------------------------------------------------------------------------------------------

    private AID truckDepot;
    private float weightLimit;
    private List<Parcel> parcels;
    private Node currentNode;
    private List<Road> route;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    public Truck(float weightLimit)
    {
        this.weightLimit = weightLimit;
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    protected void setup()
    {
        CyclicBehaviour listeningBehaviour = new CyclicBehaviour(this)
        {
            public void action()
            {
                CheckForConstraintRequest();
                CheckForParcelAllocation();
            }
        };

        addBehaviour(listeningBehaviour);
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

        //Check for Parcel_Allocation
        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("Parcel_Allocation"));
        msg = receive (mt);

        if (msg != null)
        {
            try
            {
                parcels = (List<Parcel>) msg.getContentObject();
            }
            catch (UnreadableException e)
            {
                e.printStackTrace();
            }

            msg = null;
        }
    }

    private void GetRoute ()
    {
        Behaviour getRoute = new Behaviour(this)
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

                        if (reply != null)
                        {
//                            String response = reply.getContent();

                            try
                            {
                                route = (List<Road>) reply.getContentObject();
                            }
                            catch (UnreadableException e)
                            {
                                e.printStackTrace();
                            }

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

        addBehaviour(getRoute);
    }

    private void GoToNextNode ()
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void DropOffParcel ()
    {
        List<Parcel> delivery = new ArrayList<Parcel>();

        for (Parcel p : parcels)
        {
            if (p.getDestination() == currentNode)
            {
                delivery.add(p);
            }
        }

        parcels.removeAll(delivery);
        currentNode.DeliverParcels(delivery);

//        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
//
//        // Setup request values
//        message.addReceiver(currentNode);
//
//        // Send the route according to the AID of the truck
//        try
//        {
//            message.setContentObject((Serializable)droppingOff);
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//
//        message.setConversationId("Parcel_Delivery");
//        message.setReplyWith("request" + System.currentTimeMillis()); // Unique ID
//
//        // Send request
//        send(message);
    }
}
