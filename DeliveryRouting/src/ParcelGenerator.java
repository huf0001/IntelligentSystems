import com.google.gson.Gson;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParcelGenerator {
    public static final int NUM = 30;
    private Parcel[] parcels = new Parcel[NUM];

    public static void main(String[] args) {
        ParcelGenerator generator = new ParcelGenerator();
        generator.CreateParcels(NUM);
        generator.DumpToJson();
    }

    private void CreateParcels(int num) {
        Random rand = new Random();
        for (int i = 0; i < num; i++) {
            Parcel parcel = new Parcel(i, rand.nextInt(9) + 1);
            parcels[i] = parcel;
        }
    }

    private void DumpToJson() {
        if (parcels.length != 0){
            Gson gson = new Gson();
            try{
                FileWriter writer = new FileWriter("Parcels.json");
                writer.write(gson.toJson(parcels));
                writer.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
    }
}
