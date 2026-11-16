package k04;

import java.util.Arrays;
import k04.algorithm.*;
import k04.model.*;
import k04.sim.CloudSimRunner;

public class Tests {
    static int pass = 0;

    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("GAGAL: " + name);
        pass++;
        System.out.println("OK  " + name);
    }

    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] a) throws Exception {
        // 1. model: kasus tangan. MI={100,200,300}, MIPS={100,50}, map {0,1,0}: VM0 = 1+3 = 4, VM1 = 4
        Problem hand = new Problem(new double[]{100, 200, 300}, new double[]{100, 50});
        ScheduleMetrics sm = new FitnessEvaluator(hand, 0.7, 4.0).metrics(new int[]{0, 1, 0});
        check(Math.abs(sm.makespan - 4.0) < 1e-9 && sm.degreeOfImbalance < 1e-9, "model: makespan & DI hitung tangan");

        Problem two = new Problem(new double[]{1000, 1000, 1000}, new double[]{1000}, new int[]{2});
        check(Math.abs(new FitnessEvaluator(two, 1, 1).metrics(new int[]{0, 0, 0}).makespan - 2.0) < 1e-9,
                "model: VM 2 PE (3 task 1s -> 2s)");

        // 2. kasus batas
        Problem one = new Problem(new double[]{500}, new double[]{250});
        check(Math.abs(new FitnessEvaluator(one, 0.7, 2.0).metrics(new int[]{0}).makespan - 2.0) < 1e-9, "batas: 1 task 1 VM");
        check(throwsIAE(() -> FitnessEvaluator.validateMapping(hand, new int[]{0, 1})), "batas: panjang mapping salah");
        check(throwsIAE(() -> FitnessEvaluator.validateMapping(hand, new int[]{0, 1, 2})), "batas: indeks VM di luar rentang");
        check(throwsIAE(() -> new Problem(new double[]{0}, new double[]{1})), "batas: MI nol ditolak");
        check(throwsIAE(() -> new FitnessEvaluator(hand, 1.5, 1)), "batas: bobot > 1 ditolak");

        // 3. PSO, FA, BA: valid, reprodusibel, tidak memburuk, budget
        Problem p = Problem.generate(60, 6, 7);
        AlgorithmParams ap = new AlgorithmParams();
        ap.evalsPerTask = 40;
        double ref = new FitnessEvaluator(p, 1, 1).metrics(MinMin.schedule(p)).makespan;
        int[][] init = PopulationFactory.random(ap.populationSize, 60, 6, 5);
        for (Scheduler s : new Scheduler[]{new PsoScheduler(), new FireflyScheduler(), new BatScheduler()}) {
            ScheduleResult r1 = s.schedule(p, ap, new FitnessEvaluator(p, 0.7, ref), PopulationFactory.copy(init), 11);
            ScheduleResult r2 = s.schedule(p, ap, new FitnessEvaluator(p, 0.7, ref), PopulationFactory.copy(init), 11);
            FitnessEvaluator.validateMapping(p, r1.mapping);
            check(true, s.name() + ": jadwal valid");
            check(Arrays.equals(r1.mapping, r2.mapping) && r1.fitness == r2.fitness, s.name() + ": seed sama = hasil sama");
            boolean mono = true;
            for (int i = 1; i < r1.convergence.length; i++) mono &= r1.convergence[i] <= r1.convergence[i - 1];
            check(mono, s.name() + ": konvergensi tidak memburuk");
            FitnessEvaluator pe = new FitnessEvaluator(p, 0.7, ref);
            double initBest = Double.MAX_VALUE;
            for (int[] x : init) initBest = Math.min(initBest, pe.fitness(x));
            check(r1.fitness <= initBest + 1e-12, s.name() + ": hasil <= populasi awal terbaik");
            check(r1.evaluations <= ap.maxEvaluations(60), s.name() + ": budget evaluasi tidak dilampaui");
        }

        // 4. statistik
        check(Math.abs(Stats.mean(new double[]{1, 2, 3, 4}) - 2.5) < 1e-12, "stats: mean");
        check(Math.abs(Stats.std(new double[]{2, 4, 4, 4, 5, 5, 7, 9}) - 2.13809) < 1e-4, "stats: std sampel");
        check(Stats.mannWhitneyP(new double[]{1, 2, 3, 4, 5}, new double[]{1, 2, 3, 4, 5}) > 0.99, "stats: data sama -> p ~ 1");
        check(Stats.mannWhitneyP(new double[]{1, 2, 3, 4, 5, 6, 7, 8}, new double[]{20, 21, 22, 23, 24, 25, 26, 27}) < 0.01,
                "stats: beda jelas -> p kecil");

        // 5. CloudSim vs analitik
        double[] cs = CloudSimRunner.run(hand, new int[]{0, 1, 0});
        check(Math.abs(CloudSimRunner.makespan(cs) - 4.0) < 0.2, "CloudSim: kasus tangan makespan ~ 4");
        int[] mm = MinMin.schedule(p);
        double an = new FitnessEvaluator(p, 0.7, ref).metrics(mm).makespan;
        double sim = CloudSimRunner.makespan(CloudSimRunner.run(p, mm));
        check(Math.abs(an - sim) / an < 1e-2, "CloudSim: makespan Min-Min cocok analitik (<1%)");

        System.out.println("SEMUA TES LULUS: " + pass);
    }
}