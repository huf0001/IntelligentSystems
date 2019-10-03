public class Node {
    int id, weight;
    WorldPoint position;

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
};
