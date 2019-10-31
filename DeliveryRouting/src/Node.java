import java.util.ArrayList;
import java.util.List;

public class Node
{
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

    public double GetDistance(Node node) {
        return Vector2.distance(position, node.position);
    }
}
