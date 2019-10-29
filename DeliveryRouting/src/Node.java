import java.util.ArrayList;
import java.util.List;

public class Node
{
    //Never used and we have Vector2s??
//    static class WorldPoint{
//        int x;
//        int y;
//
//        WorldPoint(int x, int y) {
//            this.x = x;
//            this.y = y;
//        }
//    }

    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private List<Parcel> parcels = new ArrayList<Parcel>();
    int id;
    Vector2 position;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    //Constructor(s)---------------------------------------------------------------------------------------------------------------------------------

    Node(int value)     //If this one is to be acceptable, needs a public property setPosition(), but I don't know why you wouldn't just use the other constructor that takes the position
    {
        this.id = value;
    }

    Node(int value, Vector2 position)
    {
        this.id = value;
        this.position = position;
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

    public void DeliverParcels(List<Parcel> parcels)
    {
        this.parcels.addAll(parcels);
    }
}
