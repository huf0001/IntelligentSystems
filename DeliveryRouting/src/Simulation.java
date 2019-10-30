import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Simulation
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private static ContainerController container = null;
    private static BufferedWriter p_stdin;
    private static Process p;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public static ContainerController getContainer()
    {
        return container;
    }

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    //Main-------------------------------------------------------------------------------------------------------------------------------------------

    //OpenCMDShell() and CloseCMDShell() adapted from https://stackoverflow.com/questions/18866381/how-can-i-run-multiple-commands-in-just-one-cmd-windows-in-java
    private static void OpenCMDShell()
    {
        //init shell
        ProcessBuilder builder = new ProcessBuilder( "C:/Windows/System32/cmd.exe");
        Process p = null;
        try
        {
            p = builder.start();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

        //get stdin of shell
         p_stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
    }

    private static void StartJADE()
    {
        try
        {
            //single execution
            p_stdin.write("java -cp lib\\jade.jar jade.Boot -gui");
            p_stdin.newLine();
            p_stdin.flush();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }

    private static ContainerController GetJADEContainer()
    {
        try
        {
            //Delay to give JADE enough time to boot up properly
            TimeUnit.MILLISECONDS.sleep(3000);
        }
            catch(Exception e)
        {
            e.printStackTrace();
        }

        //Get the JADE runtime interface (singleton)
        Runtime runtime = Runtime.instance();

        //Create a Profile, where the launch arguments are stored
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, "TestContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");

        //create a non-main agent container
        return runtime.createAgentContainer(profile);
    }

    private static void CloseCMDShell()
    {
        // finally close the shell by execution exit command
        try {
            p_stdin.write("exit");
            p_stdin.newLine();
            p_stdin.flush();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

        // write stdout of shell (=output of all commands)
        Scanner s = new Scanner( p.getInputStream() );

        while (s.hasNext())
        {
            System.out.println( s.next() );
        }
        s.close();
    }

    public static void main(String[] args)
    {
        //Start JADE
        OpenCMDShell();
        StartJADE();
        container = GetJADEContainer();

        JFrame frame = new JFrame("Delivery Routing");
        World world = new World();
        Calendar cal = Calendar.getInstance();
        boolean finished = false;
        float timeAtStartOfLoop;
        float loopDuration;
        float standardDelay = 1000 / 60;

        frame.add(world);
        frame.setSize(world.getWidth(), world.getHeight());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        while (!finished)
        {
            timeAtStartOfLoop = cal.getTimeInMillis();
            world.updateTrucks();
            world.repaint();

            try
            {
                loopDuration = cal.getTimeInMillis() - timeAtStartOfLoop;
                TimeUnit.MILLISECONDS.sleep(standardDelay > loopDuration ? (long)(standardDelay - loopDuration) : 0);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        CloseCMDShell();
    }
}
