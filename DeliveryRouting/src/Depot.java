import jade.core.Agent;
import java.util.List;

public class Depot extends Agent{
    private World world;
    private List<Parcel> parcels;
    private List<Truck> trucksAtDepot;
    private List<List<Road>> routes;
    private List<Node> unroutedNodes;
    private List<Node> routedNodes;

    public void GetConstraint(Truck truck) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<Parcel> GetParcels(){
        throw new UnsupportedOperationException("Not implemented");
    }

    public void CreateRoutes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void GiveRoute(Truck){
        throw new UnsupportedOperationException("Not implemented");
    }
}
