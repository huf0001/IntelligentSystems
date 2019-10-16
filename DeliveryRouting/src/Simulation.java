import jade.core.AID;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Simulation
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private static Random random = new Random();

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    private static float RandomFloatBetween(float min, float max)
    {
        return random.nextFloat() * (max - min) + min;
    }

    //Main-------------------------------------------------------------------------------------------------------------------------------------------

    public static void main(String[] args)
    {
        float minWeightLimit = 10;
        float maxWeightLimit = 20;
        int numTrucks = 10;

        Depot depot;
        List<Truck> trucks = new ArrayList<>();
        List<AID> truckAIDs = new ArrayList<>();
        //Vector2 depotPos = world.getGraph().getDepotNode().position;

        for (int i = 0; i < numTrucks; i++)
        {
            trucks.add(new Truck(new Vector2(), RandomFloatBetween(minWeightLimit, maxWeightLimit)));
            truckAIDs.add(trucks.get(i).getAID());
        }

        depot = new Depot(truckAIDs);
        World world = new World(trucks);
    }
}
