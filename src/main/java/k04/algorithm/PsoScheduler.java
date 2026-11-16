package k04.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import k04.model.FitnessEvaluator;
import k04.model.Problem;

/** PSO: posisi kontinu [0, m), decode dengan floor. Inertia turun linear mengikuti progres evaluasi. */
public class PsoScheduler implements Scheduler {
    public String name() { return "PSO"; }

    public ScheduleResult schedule(Problem p, AlgorithmParams ap, FitnessEvaluator ev,
                                   int[][] init, long seed) {
        long t0 = System.nanoTime();
        Random rnd = new Random(seed);
        int n = p.taskCount(), m = p.vmCount(), N = init.length;
        long max = ap.maxEvaluations(n);
        long base = ev.evaluations();
        double vmax = ap.psoVmaxFraction * m;
        double xmax = m - 1e-9;

        double[][] x = new double[N][n], v = new double[N][n], pb = new double[N][n];
        double[] pbFit = new double[N];
        int[] best = null;
        double[] gb = null;
        double bestFit = Double.MAX_VALUE;
        int[] dec = new int[n];

        for (int i = 0; i < N; i++) {
            FitnessEvaluator.validateMapping(p, init[i]);
            for (int t = 0; t < n; t++) {
                x[i][t] = init[i][t] + 0.5;                       // pusat sel VM -> decode kembali ke init
                v[i][t] = (rnd.nextDouble() * 2 - 1) * vmax;
            }
            pb[i] = x[i].clone();
            pbFit[i] = ev.fitness(init[i]);
            if (pbFit[i] < bestFit) { bestFit = pbFit[i]; best = init[i].clone(); gb = x[i].clone(); }
        }
        List<Double> conv = new ArrayList<>();
        conv.add(bestFit);

        outer:
        while (ev.evaluations() - base < max) {
            double progress = (double) (ev.evaluations() - base) / max;
            double w = ap.psoWStart - (ap.psoWStart - ap.psoWEnd) * progress;
            for (int i = 0; i < N; i++) {
                if (ev.evaluations() - base >= max) break outer;
                for (int t = 0; t < n; t++) {
                    double vel = w * v[i][t]
                            + ap.psoC1 * rnd.nextDouble() * (pb[i][t] - x[i][t])
                            + ap.psoC2 * rnd.nextDouble() * (gb[t] - x[i][t]);
                    vel = Math.max(-vmax, Math.min(vmax, vel));
                    v[i][t] = vel;
                    x[i][t] = Math.max(0, Math.min(xmax, x[i][t] + vel));
                    dec[t] = (int) Math.floor(x[i][t]);
                }
                double f = ev.fitness(dec);
                if (f < pbFit[i]) { pbFit[i] = f; pb[i] = x[i].clone(); }
                if (f < bestFit) { bestFit = f; best = dec.clone(); gb = x[i].clone(); }
            }
            conv.add(bestFit);
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return new ScheduleResult(best, bestFit, ev.evaluations() - base,
                conv.stream().mapToDouble(Double::doubleValue).toArray(), ms);
    }
}