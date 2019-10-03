public class Vector2 {
    public double x;
    public double y;

    // Constructors
    public Vector2() {
        this.x = 0.0f;
        this.y = 0.0f;
    }

//    public Vector2(float x, float y) {
//        this.x = x;
//        this.y = y;
//    }

    public Vector2(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    // Compare two vectors
    public boolean equals(Vector2 other) {
        return (this.x == other.x && this.y == other.y);
    }

    public static double distance(Vector2 a, Vector2 b) {
        double v0 = b.x - a.x;
        double v1 = b.y - a.y;
        return Math.sqrt(v0*v0 + v1*v1);
    }

    public static double distance(Vector2 a, double b) {
        double v0 = b - a.x;
        double v1 = b - a.y;
        return Math.sqrt(v0*v0 + v1*v1);
    }

    public void normalize() {
        // sets length to 1
        //
        double length = Math.sqrt(x*x + y*y);

        if (length != 0.0) {
            float s = 1.0f / (float)length;
            x = x*s;
            y = y*s;
        }
    }

    public Vector2 divide(double factor) {
        Vector2 v = new Vector2();
        if (factor != 0){
            v.x = x / factor;
            v.y = y / factor;
            return v;
        }
        return null;
    }

    public Vector2 divide(Vector2 factor) {
        Vector2 v = new Vector2();
        if (factor.x != 0 && factor.y != 0){
            v.x = x / factor.x;
            v.y = y / factor.y;
            return v;
        }
        return null;
    }

    public Vector2 multiply(double factor) {
        Vector2 v = new Vector2();
        v.x = x * factor;
        v.y = y * factor;
        return v;
    }

    public Vector2 multiply(Vector2 factor) {
        Vector2 v = new Vector2();
        v.x = x * factor.x;
        v.y = y * factor.y;
        return v;
    }

    public Vector2 add(double factor) {
        Vector2 v = new Vector2();
        v.x = x + factor;
        v.y = y + factor;
        return v;
    }

    public Vector2 add(Vector2 factor) {
        Vector2 v = new Vector2();
        v.x = x + factor.x;
        v.y = y + factor.y;
        return v;
    }

    public Double sqrMagnitude(){
        Double d;
        d = x * x;
        d = d + y * y;
        return d;
    }

    public Vector2 minus(Vector2 factor){
        Vector2 v = new Vector2();
        v.x = x - factor.x;
        v.y = y - factor.y;
        return v;
    }

    public Vector2 minus(double factor){
        Vector2 v = new Vector2();
        v.x = x - factor;
        v.y = y - factor;
        return v;
    }
}
