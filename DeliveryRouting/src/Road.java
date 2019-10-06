public class Road
{
    //Private Fields---------------------------------------------------------------------------------------------------------------------------------

    private Node source;
    private Node destination;

    //Public Properties------------------------------------------------------------------------------------------------------------------------------

    public Node getSource()
    {
        return source;
    }

    public Node getDestination()
    {
        return destination;
    }

    //Constructor------------------------------------------------------------------------------------------------------------------------------------

    public Road (Node source, Node destination)
    {
        this.source = source;
        this.destination = destination;
    }

    //Methods----------------------------------------------------------------------------------------------------------------------------------------

}
