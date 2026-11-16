package k04.algorithm;

import java.util.Random;

public final class PopulationFactory {
    private PopulationFactory() {}

    /** Populasi acak: tiap task diberi indeks VM acak. Seed sama = populasi sama. */
    public static int[][] random(int populationSize, int taskCount, int vmCount, long seed) {
        Random rnd = new Random(seed);
        int[][] pop = new int[populationSize][taskCount];
        for (int i = 0; i < populationSize; i++) {
            for (int t = 0; t < taskCount; t++) {
                pop[i][t] = rnd.nextInt(vmCount);
            }
        }
        return pop;
    }

    public static int[][] copy(int[][] pop) {
        int[][] c = new int[pop.length][];
        for (int i = 0; i < pop.length; i++) c[i] = pop[i].clone();
        return c;
    }
}