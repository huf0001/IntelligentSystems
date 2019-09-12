import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

public class Truck extends Agent
{
    private float weightLimit;
    private List<Parcel> parcels;
    private Node currentNode;
    private List<Road> route;

    protected void setup()
    {
        addBehaviour(new ListeningBehaviour());
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
        throw new UnsupportedOperationException("Not implemented");
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

    private class ListeningBehaviour extends CyclicBehaviour
    {
        public void action()
        {
            //Look for message
            ACLMessage msg = receive();

            //If found a message
            if (msg != null)
            {
                //Get fields that tell me what the message is for
                String conversationId = msg.getConversationId();
                String content = msg.getContent();

                //If it's a constraint request asking for the weight limit
                if (conversationId == "Constraint_Request" && content == "Capacity")
                {
                    //Reply with weight limit
                    GiveConstraints(msg);
                }

                //Reset after handling message
                msg = null;
            }
        }
    }
}
