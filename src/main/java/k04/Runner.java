package k04;

import java.io.PrintWriter;
import java.nio.file.*;
import java.util.Locale;
import k04.algorithm.*;
import k04.dataset.GoCJLoader;
import k04.model.*;
import k04.sim.CloudSimRunner;

/** Pakai: java -cp "out:lib/*" k04.Runner E1 [folderData]. E1 | E2 | E3 | E4. */
public class Runner {
    static final String[] NM = {"PSO", "FA", "BA"};

    public static void main(String[] a) throws Exception {
        String exp = a.length > 0 ? a[0] : "E1";
        String dir = a.length > 1 ? a[1] : "data";
        int runs = a.length > 2 ? Integer.parseInt(a[2]) : 30;
        AlgorithmParams ap = new AlgorithmParams();
        switch (exp) {
            case "E1": run(dir, 500, 0.7, false, runs, ap, "E1"); break;
            case "E2": run(dir, 500, 0.7, true, runs, ap, "E2"); break;      // populasi awal berisi solusi Min-Min
            case "E3": for (double w : new double[]{1.0, 0.7, 0.5}) run(dir, 500, w, false, runs, ap, "E3"); break;
            case "E4": for (int n : new int[]{100, 300, 500, 800, 1000}) run(dir, n, 0.7, false, runs, ap, "E4"); break;
            default: throw new IllegalArgumentException("pilih E1..E4");
        }
    }

    static void run(String dir, int fileN, double w, boolean seedMM, int runs,
                    AlgorithmParams ap, String tag) throws Exception {
        Problem p = Problem.design(GoCJLoader.read(dir + "/GoCJ_Dataset_" + fileN + ".txt"));
        int n = p.taskCount(), m = p.vmCount();

        long t0 = System.nanoTime();
        int[] mm = MinMin.schedule(p);
        long mmMs = (System.nanoTime() - t0) / 1_000_000;
        double ref = new FitnessEvaluator(p, 1, 1).metrics(mm).makespan;     // acuan normalisasi = makespan Min-Min
        ScheduleMetrics mmM = new FitnessEvaluator(p, w, ref).metrics(mm);
        double lb = p.lowerBound();

        Scheduler[] algs = {new PsoScheduler(), new FireflyScheduler(), new BatScheduler()};
        double[][] mk = new double[3][runs], di = new double[3][runs], gap = new double[3][runs],
                evs = new double[3][runs], ms = new double[3][runs];
        int[][] firstBest = new int[3][];

        Files.createDirectories(Paths.get("results"));
        String base = "results/" + tag + "_n" + n + "_w" + w;
        try (PrintWriter csv = new PrintWriter(base + ".csv");
             PrintWriter cv = new PrintWriter(base + "_conv.csv")) {
            csv.println("algo,seed,makespan,di,arur,throughput,avgExec,avgWait,gapLB_pct,fitness,evals,runtimeMs");
            cv.println("algo,seed,gen,bestFitness");
            csv.println(row("MinMin", 0, mmM, lb, new FitnessEvaluator(p, w, ref).fitness(mm), 0, mmMs));

            for (int s = 1; s <= runs; s++) {
                int[][] init = PopulationFactory.random(ap.populationSize, n, m, s);
                System.err.println("seed " + s + "/" + runs + " (n=" + n + ")");
                if (seedMM) init[0] = mm.clone();
                for (int k = 0; k < 3; k++) {
                    FitnessEvaluator e = new FitnessEvaluator(p, w, ref);      // evaluator baru: hitungan evaluasi bersih
                    ScheduleResult r = algs[k].schedule(p, ap, e, PopulationFactory.copy(init), s);
                    ScheduleMetrics sm = e.metrics(r.mapping);
                    mk[k][s - 1] = sm.makespan; di[k][s - 1] = sm.degreeOfImbalance;
                    gap[k][s - 1] = (sm.makespan - lb) / lb * 100;
                    evs[k][s - 1] = r.evaluations; ms[k][s - 1] = r.runtimeMs;
                    if (s == 1) firstBest[k] = r.mapping;
                    csv.println(row(NM[k], s, sm, lb, r.fitness, r.evaluations, r.runtimeMs));
                    for (int g = 0; g < r.convergence.length; g++)
                        cv.println(NM[k] + "," + s + "," + g + "," + r.convergence[g]);
                }
            }
        }

        System.out.printf(Locale.US, "%n== %s | n=%d VM=%d w=%.1f seedMinMin=%b | budget=%d eval | LB=%.2f%n",
                tag, n, m, w, seedMM, ap.maxEvaluations(n), lb);
        System.out.printf(Locale.US, "Min-Min : makespan=%.2f DI=%.4f gapLB=%.2f%% (%d ms)%n",
                mmM.makespan, mmM.degreeOfImbalance, (mmM.makespan - lb) / lb * 100, mmMs);
        for (int k = 0; k < 3; k++) {
            double[] x = mk[k];
            double best = Double.MAX_VALUE, worst = 0;
            for (double v : x) { best = Math.min(best, v); worst = Math.max(worst, v); }
            System.out.printf(Locale.US, "%-4s best=%.2f mean=%.2f worst=%.2f sd=%.2f | DI=%.4f | gapLB=%.2f%% | eval=%.0f | %.0f ms%n",
                    NM[k], best, Stats.mean(x), worst, Stats.std(x), Stats.mean(di[k]),
                    Stats.mean(gap[k]), Stats.mean(evs[k]), Stats.mean(ms[k]));
        }
        System.out.printf(Locale.US, "Wilcoxon rank-sum (makespan): PSO-FA p=%.4f | PSO-BA p=%.4f | FA-BA p=%.4f%n",
                Stats.mannWhitneyP(mk[0], mk[1]), Stats.mannWhitneyP(mk[0], mk[2]), Stats.mannWhitneyP(mk[1], mk[2]));

        // validasi model analitik vs CloudSim (seed 1 + Min-Min). Draft: selisih relatif harus < 1%
        FitnessEvaluator ve = new FitnessEvaluator(p, w, ref);
        int[][] chk = {firstBest[0], firstBest[1], firstBest[2], mm};
        String[] lbl = {"PSO", "FA", "BA", "Min-Min"};
        for (int i = 0; i < 4; i++) {
            double cs = CloudSimRunner.makespan(CloudSimRunner.runDesign(p, chk[i]));
            double an = ve.metrics(chk[i]).makespan;
            double rel = Math.abs(cs - an) / an;
            System.out.printf(Locale.US, "CloudSim %-7s %.4f | analitik %.4f | selisih %.4f%% %s%n",
                    lbl[i], cs, an, rel * 100, rel < 0.01 ? "OK" : "<< MELEBIHI 1%, koreksi model");
        }
    }

    private static String row(String algo, int seed, ScheduleMetrics m, double lb, double fit, long evals, long ms) {
        return String.format(Locale.US, "%s,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.4f,%.6f,%d,%d",
                algo, seed, m.makespan, m.degreeOfImbalance, m.arur, m.throughput,
                m.avgExecTime, m.avgWaitTime, (m.makespan - lb) / lb * 100, fit, evals, ms);
    }
}