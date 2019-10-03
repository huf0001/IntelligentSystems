public class Node {
    int id, weight;
    Vector2 position;



    Node(int value)
    {
        this.id = value;
    }

    Node(int value, Vector2 position)
    {
        this.id = value;
        this.position = position;
    }
};
