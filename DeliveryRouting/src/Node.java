import java.util.ArrayList;
import java.util.List;

public class Node
{
    int id, weight;
    Vector2 position;

    static class WorldPoint{
        int x;
        int y;

        WorldPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    Node(int value)
    {
        this.id = value;
    }

    Node(int value, Vector2 position)
    {
        this.id = value;
        this.position = position;
    }

    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private List<Parcel> parcels = new ArrayList<Parcel>();

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    public void DeliverParcels(List<Parcel> parcels)
    {
        this.parcels.addAll(parcels);
    }
}
