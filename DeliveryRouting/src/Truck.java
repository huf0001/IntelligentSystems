import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.tools.sniffer.Message;

import java.util.List;

public class Truck extends Agent
{
    private AID truckDepot;
    private float weightLimit;
    private List<Parcel> parcels;
    private Node currentNode;
    private List<Road> route;

    public Truck(float weightLimit)
    {
        this.weightLimit = weightLimit;
    }

    protected void setup()
    {
        CyclicBehaviour listeningBehaviour = new CyclicBehaviour(this)
        {
            public void action()
            {
                //Create message template
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("Constraint_Request"));

                //Look for message
                ACLMessage msg = receive(mt);

                //If found a message
                if (msg != null)
                {
                    /*//Get fields that tell me what the message is for
                    int performative = msg.getPerformative();
                    String conversationId = msg.getConversationId();
                    String content = msg.getContent();

                    //If it's a constraint request asking for the weight limit
                    if (performative == ACLMessage.REQUEST && conversationId == "Constraint_Request" && content == "Capacity")
                    {*/
                        //Reply with weight limit
                        GiveConstraints(msg);
                    //}

                    //Reset after handling message
                    msg = null;
                }
            }
        };

        addBehaviour(listeningBehaviour);
    }

    private void DropOffParcel ()
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void GoToNextNode ()
    {
        throw new UnsupportedOperationException("Not implemented");
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
}
