public class Road
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private Node source;
    private Node destination;
    private double weight;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public Node getSource()
    {
        return source;
    }

    public Node getDestination()
    {
        return destination;
    }

    public double getWeight(){
        return weight;
    }

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    public Road (Node source, Node destination)
    {
        this.source = source;
        this.destination = destination;
        weight = Vector2.distance(source.position, destination.position);
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------
}
