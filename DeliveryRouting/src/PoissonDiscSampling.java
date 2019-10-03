import java.util.*;
import java.lang.Math;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PoissonDiscSampling {

public static List<Vector2> GeneratePoints(float radius, Vector2 sampleRegionSize, int numSamplesBeforeRejections){
    Random rand = new Random();

    float cellSize = (float) (radius/Math.sqrt(2));
    int[][] grid = new int[(int) Math.ceil(sampleRegionSize.x/cellSize)][(int) Math.ceil(sampleRegionSize.y/cellSize)];
    List<Vector2> points = new ArrayList<>();
    List<Vector2> spawnPoints = new ArrayList<>();

    spawnPoints.add(sampleRegionSize.divide(2));

        while(spawnPoints.size() > 0) {
//        for(int i = 0; i < 100; i++) {
            int spawnIndex = rand.nextInt(spawnPoints.size());
            Vector2 spawnCentre = spawnPoints.get(spawnIndex);
            boolean candidateAccepted = false;

            for(int j = 0; j < numSamplesBeforeRejections; j++){
                float angle = (float) (rand.nextFloat() * Math.PI * 2);
                Vector2 dir = new Vector2(Math.sin(angle), Math.cos(angle));
                Vector2 candidate = spawnCentre.add(dir.multiply(ThreadLocalRandom.current().nextDouble(radius, 2*radius)));
                if(IsValid(candidate, sampleRegionSize, cellSize, radius, points, grid)){
                    points.add(candidate);
                    spawnPoints.add(candidate);
                    grid[(int)(candidate.x/cellSize)][(int)(candidate.y/cellSize)] = points.size();
                    candidateAccepted = true;
                    break;
                }
            }
            if(!candidateAccepted){
                spawnPoints.remove(spawnIndex);
            }
        }
        return points;
    }

    static boolean IsValid(Vector2 candidate, Vector2 sampleRegionSize, float cellSize, float radius, List<Vector2> points, int[][] grid){
        if(candidate.x >= 0 && candidate.x < sampleRegionSize.x && candidate.y >= 0 && candidate.y < sampleRegionSize.y){
            int cellX = (int)(candidate.x/cellSize);
            int cellY = (int)(candidate.y/cellSize);
            int searchStartX = Math.max(0, cellX - 2);
            int searchEndX = Math.min(cellX + 2, grid[0].length - 1);
            int searchStartY = Math.max(0, cellY - 2);
            int searchEndY = Math.min(cellY + 2, grid[1].length - 1);

            for(int x = searchStartX; x <= searchEndX; x++){
                for(int y = searchStartY; y <= searchEndY; y++){
                    int pointIndex = grid[x][y] - 1;
                    if (pointIndex != -1 ){
                        double sqrDst = (candidate.minus((points.get(pointIndex)))).sqrMagnitude();
                        if (sqrDst < radius * radius){
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return true;
    }
}
