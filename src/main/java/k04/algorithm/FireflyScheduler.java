package k04.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import k04.model.FitnessEvaluator;
import k04.model.Problem;

/** FA diskret: jarak Hamming, beta = beta0*exp(-gamma*d^2), salin gen dari firefly lebih terang, mutasi alpha(t) = alpha0*delta^t. 1 evaluasi per firefly per generasi. */
public class FireflyScheduler implements Scheduler {
    public String name() { return "FA"; }

    public ScheduleResult schedule(Problem p, AlgorithmParams ap, FitnessEvaluator ev,
                                   int[][] init, long seed) {
        long t0 = System.nanoTime();
        Random rnd = new Random(seed);
        int n = p.taskCount(), m = p.vmCount(), N = init.length;
        long max = ap.maxEvaluations(n);
        long base = ev.evaluations();

        int[][] pop = PopulationFactory.copy(init);
        double[] fit = new double[N];
        int[] best = null;
        double bestFit = Double.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            FitnessEvaluator.validateMapping(p, pop[i]);
            fit[i] = ev.fitness(pop[i]);
            if (fit[i] < bestFit) { bestFit = fit[i]; best = pop[i].clone(); }
        }
        List<Double> conv = new ArrayList<>();
        conv.add(bestFit);

        int gens = (int) Math.max(1, (max - N) / N);
        double delta = Math.pow(ap.faAlphaEndRatio, 1.0 / gens);   // alpha turun 10x di akhir run
        double alpha = ap.faAlpha0;

        while (ev.evaluations() - base < max) {
            int[][] next = new int[N][];
            for (int i = 0; i < N; i++) {
                int[] s = pop[i].clone();
                for (int j = 0; j < N; j++) {
                    if (j == i || !(fit[j] < fit[i])) continue;     // hanya tertarik ke yang lebih terang
                    double d = hamming(s, pop[j]) / (double) n;
                    double beta = ap.faBeta0 * Math.exp(-ap.faGamma * d * d);
                    for (int t = 0; t < n; t++) if (rnd.nextDouble() < beta) s[t] = pop[j][t];
                }
                for (int t = 0; t < n; t++) if (rnd.nextDouble() < alpha) s[t] = rnd.nextInt(m);
                next[i] = s;                                        // firefly terbaik: hanya mutasi (random walk)
            }
            double[] nf = fit.clone();
            for (int i = 0; i < N; i++) {
                if (ev.evaluations() - base >= max) { next[i] = pop[i]; continue; }   // budget habis: pertahankan lama
                nf[i] = ev.fitness(next[i]);
                if (nf[i] < bestFit) { bestFit = nf[i]; best = next[i].clone(); }
            }
            pop = next; fit = nf;
            if (Arrays.stream(fit).min().getAsDouble() > bestFit) {   // elitisme: best global tidak hilang
                int w = 0;
                for (int i = 1; i < N; i++) if (fit[i] > fit[w]) w = i;
                pop[w] = best.clone(); fit[w] = bestFit;
            }
            alpha *= delta;
            conv.add(bestFit);
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return new ScheduleResult(best, bestFit, ev.evaluations() - base,
                conv.stream().mapToDouble(Double::doubleValue).toArray(), ms);
    }

    private static int hamming(int[] a, int[] b) {
        int d = 0;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) d++;
        return d;
    }
}