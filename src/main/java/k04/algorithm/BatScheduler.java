package k04.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import k04.model.FitnessEvaluator;
import k04.model.Problem;

/** BA diskret: f in [0, fmax], tiap task salin gen best dengan peluang f. Bila rand > r: salinan best + k task dipindah, k = ceil(mean(A)*kmax). Terima bila F <= F(s_i) dan rand < A. */
public class BatScheduler implements Scheduler {
    public String name() { return "BA"; }

    public ScheduleResult schedule(Problem p, AlgorithmParams ap, FitnessEvaluator ev,
                                   int[][] init, long seed) {
        long t0 = System.nanoTime();
        Random rnd = new Random(seed);
        int n = p.taskCount(), m = p.vmCount(), N = init.length;
        long max = ap.maxEvaluations(n);
        long base = ev.evaluations();
        int kmax = Math.max(1, (int) Math.round(ap.batKmaxFraction * n));

        int[][] pop = PopulationFactory.copy(init);
        double[] fit = new double[N], loud = new double[N], pulse = new double[N];
        int[] best = null;
        double bestFit = Double.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            FitnessEvaluator.validateMapping(p, pop[i]);
            fit[i] = ev.fitness(pop[i]);
            loud[i] = ap.batA0;
            pulse[i] = 0;
            if (fit[i] < bestFit) { bestFit = fit[i]; best = pop[i].clone(); }
        }
        List<Double> conv = new ArrayList<>();
        conv.add(bestFit);
        int gen = 0;

        outer:
        while (ev.evaluations() - base < max) {
            gen++;
            double avgA = 0;
            for (double a : loud) avgA += a;
            avgA /= N;
            for (int i = 0; i < N; i++) {
                if (ev.evaluations() - base >= max) break outer;
                double f = rnd.nextDouble() * ap.batFMax;
                int[] c = pop[i].clone();
                for (int t = 0; t < n; t++) if (rnd.nextDouble() < f) c[t] = best[t];
                if (rnd.nextDouble() > pulse[i]) {
                    c = best.clone();
                    int k = (int) Math.ceil(avgA * kmax);
                    for (int q = 0; q < k; q++) c[rnd.nextInt(n)] = rnd.nextInt(m);
                }
                double fc = ev.fitness(c);
                if (fc <= fit[i] && rnd.nextDouble() < loud[i]) {
                    pop[i] = c; fit[i] = fc;
                    loud[i] *= ap.batAlpha;
                    pulse[i] = ap.batR0 * (1 - Math.exp(-ap.batGamma * gen));
                }
                if (fc < bestFit) { bestFit = fc; best = c.clone(); }
            }
            conv.add(bestFit);
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return new ScheduleResult(best, bestFit, ev.evaluations() - base,
                conv.stream().mapToDouble(Double::doubleValue).toArray(), ms);
    }
}