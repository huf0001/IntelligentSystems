import jade.core.Agent;

public class AgentWithArguments extends Agent {
    protected void setup() {
        Object[] args = getArguments();
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
        }
    }
}
