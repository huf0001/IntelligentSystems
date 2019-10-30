public class Parcel
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private int id;
    private int weight;
    private Node destination;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public Node getDestination() { return destination; }
    public void setDestination(Node value) { destination = value; }
    public int getWeight() { return weight; }
    public int getID()
    {
        return id;
    }

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    Parcel(int aId, int aWeight)
    {
        id = aId;
        weight = aWeight;
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

}
