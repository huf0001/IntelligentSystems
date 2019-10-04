import jade.core.AID;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Simulation
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private Random random = new Random();
    private float minWeightLimit = 10;
    private float maxWeightLimit = 20;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    void main()
    {
        World world = new World();
        Depot depot;
        List<Truck> trucks = new ArrayList<>();
        List<AID> truckAIDs = new ArrayList<>();
        int numTrucks = 10;


        for (int i = 0; i < numTrucks; i++)
        {
            trucks.add(new Truck(RandomFloatBetween(minWeightLimit, maxWeightLimit)));
            truckAIDs.add(trucks.get(i).getAID());
        }

        depot = new Depot(truckAIDs);
    }

    private float RandomFloatBetween(float min, float max)
    {
        return random.nextFloat() * (max - min) + min;
    }
}
