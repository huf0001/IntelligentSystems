import jade.core.AID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Simulation
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    //Public Properties------------------------------------------------------------------------------------------------------------------------------
    public static ContainerController container;
    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    //Main-------------------------------------------------------------------------------------------------------------------------------------------

    public static void main(String[] args)
    {
        //Get the JADE runtime interface (singleton)
        jade.core.Runtime runtime = jade.core.Runtime.instance();
        //Create a Profile, where the launch arguments are stored
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, "TestContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        //create a non-main agent container
        container = runtime.createAgentContainer(profile);

        JFrame frame = new JFrame("Delivery Routing");
        World world = new World();
        boolean finished = false;

        frame.add(world);
        frame.setSize(world.getWidth(), world.getHeight());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);


        while (!finished)
        {
            world.updateTrucks();
            world.repaint();

            try
            {
                TimeUnit.MILLISECONDS.sleep(1000/60);
            }
            catch(Exception e)
            {

            }
        }
    }
}
