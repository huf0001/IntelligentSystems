import jade.core.AID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Simulation
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    //Main-------------------------------------------------------------------------------------------------------------------------------------------

    public static void main(String[] args)
    {
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
