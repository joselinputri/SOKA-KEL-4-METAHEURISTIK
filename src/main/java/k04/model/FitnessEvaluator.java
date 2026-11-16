package k04.model;

import java.util.Arrays;

/**
 * Evaluator analitik. Tiap VM = antrean FIFO (urutan id task), tiap task ambil PE yang paling cepat kosong.
 * Untuk VM 1 PE: T(v) = jumlah MI/MIPS task di VM v.
 * Fitness (diminimalkan): F = w * makespan / makespanRef + (1 - w) * DI.
 */
public class FitnessEvaluator {
    private final Problem problem;
    private final double weight;
    private final double makespanRef;
    private final double[][] free;
    private final double[] finish;
    private long evaluations;

    public FitnessEvaluator(Problem problem, double weight, double makespanRef) {
        if (weight < 0 || weight > 1) throw new IllegalArgumentException("bobot w harus 0..1");
        if (!(makespanRef > 0)) throw new IllegalArgumentException("makespanRef harus > 0");
        this.problem = problem;
        this.weight = weight;
        this.makespanRef = makespanRef;
        this.free = new double[problem.vmCount()][];
        for (int v = 0; v < free.length; v++) free[v] = new double[problem.pes(v)];
        this.finish = new double[problem.vmCount()];
    }

    public static void validateMapping(Problem p, int[] mapping) {
        if (mapping == null || mapping.length != p.taskCount()) {
            throw new IllegalArgumentException("panjang mapping harus " + p.taskCount());
        }
        for (int vm : mapping) {
            if (vm < 0 || vm >= p.vmCount()) throw new IllegalArgumentException("indeks VM di luar rentang: " + vm);
        }
    }

    /** acc[0] = total waktu eksekusi, acc[1] = total waktu tunggu (boleh null). */
    private void simulate(int[] mapping, double[] acc) {
        for (double[] f : free) Arrays.fill(f, 0.0);
        for (int t = 0; t < mapping.length; t++) {
            int v = mapping[t];
            double[] f = free[v];
            int k = 0;
            for (int i = 1; i < f.length; i++) if (f[i] < f[k]) k = i;
            double e = problem.execTime(t, v);
            if (acc != null) { acc[0] += e; acc[1] += f[k]; }
            f[k] += e;
        }
        for (int v = 0; v < free.length; v++) {
            double mx = 0;
            for (double x : free[v]) mx = Math.max(mx, x);
            finish[v] = mx;
        }
    }

    /** Fitness satu solusi; menambah penghitung evaluasi. */
    public double fitness(int[] mapping) {
        evaluations++;
        simulate(mapping, null);
        double max = 0, min = Double.MAX_VALUE, sum = 0;
        for (double l : finish) { max = Math.max(max, l); min = Math.min(min, l); sum += l; }
        double avg = sum / finish.length;
        double di = avg > 0 ? (max - min) / avg : 0;
        return weight * (max / makespanRef) + (1 - weight) * di;
    }

    /** Metrik lengkap; tidak menambah penghitung evaluasi. */
    public ScheduleMetrics metrics(int[] mapping) {
        validateMapping(problem, mapping);
        double[] acc = new double[2];
        simulate(mapping, acc);
        int n = mapping.length;
        return new ScheduleMetrics(finish.clone(), n, acc[0] / n, acc[1] / n);
    }

    public long evaluations() { return evaluations; }
}