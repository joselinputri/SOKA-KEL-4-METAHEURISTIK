package k04.algorithm;

public class ScheduleResult {
    public final int[] mapping;
    public final double fitness;
    public final long evaluations;
    public final double[] convergence;   // fitness terbaik per generasi (indeks 0 = populasi awal)
    public final long runtimeMs;

    public ScheduleResult(int[] mapping, double fitness, long evaluations,
                          double[] convergence, long runtimeMs) {
        this.mapping = mapping;
        this.fitness = fitness;
        this.evaluations = evaluations;
        this.convergence = convergence;
        this.runtimeMs = runtimeMs;
    }
}