import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Simulation
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private static ContainerController containerController = null;
    private static BufferedWriter bufferedWriter;
    private static Process process;
    private static Calendar cal = null;
    private static final float TARGET_FRAME_DURATION = 1000 / 60;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public static ContainerController getContainerController()
    {
        return containerController;
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
         bufferedWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
    }

    private static void StartJADE()
    {
        try
        {
            //single execution
            bufferedWriter.write("java -cp lib\\jade.jar jade.Boot -gui");
            bufferedWriter.newLine();
            bufferedWriter.flush();
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
            bufferedWriter.write("exit");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

        // write stdout of shell (=output of all commands)
        Scanner s = new Scanner( process.getInputStream() );

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
        containerController = GetJADEContainer();

        JFrame frame = new JFrame("Delivery Routing");
        World world = null;
        cal = Calendar.getInstance();
        boolean finished = false;
        float timeAtStartOfLoop;
        float frameDuration;

        try
        {
            world = new World();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        frame.add(world);
        frame.setSize(world.getWidth(), world.getHeight());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        while (!finished)
        {
            timeAtStartOfLoop = cal.getTimeInMillis();
            world.repaint();

            try
            {
                frameDuration = cal.getTimeInMillis() - timeAtStartOfLoop;
                TimeUnit.MILLISECONDS.sleep(TARGET_FRAME_DURATION > frameDuration ? (long)(TARGET_FRAME_DURATION - frameDuration) : 0);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        CloseCMDShell();
    }
}
