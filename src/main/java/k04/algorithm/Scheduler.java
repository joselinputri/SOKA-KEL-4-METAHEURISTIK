package k04.algorithm;

import k04.model.FitnessEvaluator;
import k04.model.Problem;

/** Interface bersama semua algoritma. Populasi awal diberikan dari luar agar identik antar algoritma. */
public interface Scheduler {
    String name();

    ScheduleResult schedule(Problem problem, AlgorithmParams params, FitnessEvaluator evaluator,
                            int[][] initialPopulation, long seed);
}