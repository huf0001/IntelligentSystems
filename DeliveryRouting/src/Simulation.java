import jade.core.AID;

import javax.swing.*;
import java.awt.*;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.util.Calendar;

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
        Calendar cal = Calendar.getInstance();
        boolean finished = false;

        frame.add(world);
        frame.setSize(world.getWidth(), world.getHeight());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);


        while (!finished)
        {
            float timeAtStartOfLoop = cal.getTimeInMillis();

            world.updateTrucks();
            world.repaint();

            try
            {
                float loopDuration = cal.getTimeInMillis() - timeAtStartOfLoop;
                float standardDelay = 1000 / 60;

                TimeUnit.MILLISECONDS.sleep(standardDelay > loopDuration ? (long)(standardDelay - loopDuration) : 0);
            }
            catch(Exception e)
            {

            }
        }
    }
}
