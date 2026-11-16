package k04;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import k04.algorithm.*;
import k04.dataset.GoCJLoader;
import k04.model.*;

/**
 * MetricExtractor:
 * Menjalankan evaluasi komprehensif pada algoritma utama (BA dan FA)
 * serta pembanding (Min-Min dan PSO) untuk:
 *  - Mode E1: Inisialisasi Acak Murni
 *  - Mode E2: Inisialisasi Hibrida (Seed Min-Min)
 *  - Perbandingan Head-to-Head, Skalabilitas (E4), dan Ekspor CSV/Grafik.
 */
public class MetricExtractor {

    private static final String DATA_DIR = "data";
    private static final String RESULTS_DIR = "results";

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(RESULTS_DIR));

        // Dataset utama sesuai draft desain: GoCJ_Dataset_500.txt (500 task)
        String datasetFile = args.length > 0 ? args[0] : DATA_DIR + "/GoCJ_Dataset_500.txt";

        System.out.println("================================================================================");
        System.out.println("      SIMULASI KOMPARASI LENGKAP PENJADWALAN CLOUD: E1 (ACAK) vs E2 (HIBRIDA)   ");
        System.out.println("================================================================================");

        // 1. Karakterisasi Beban Dataset
        double[] rawMi = GoCJLoader.read(datasetFile);
        int n = rawMi.length;
        Problem problem = Problem.design(rawMi);
        int m = problem.vmCount();

        int small = 0, medium = 0, large = 0, xl = 0, huge = 0;
        for (double mi : rawMi) {
            if (mi <= 55_000) small++;
            else if (mi <= 99_000) medium++;
            else if (mi <= 135_000) large++;
            else if (mi <= 525_000) xl++;
            else huge++;
        }

        System.out.printf("Sumber: %s (%d task, dipakai apa adanya)%n", datasetFile, n);
        System.out.printf("  Small          %4d task (15.000 - 55.000 MI)%n", small);
        System.out.printf("  Medium         %4d task (59.000 - 99.000 MI)%n", medium);
        System.out.printf("  Large          %4d task (101.000 - 135.000 MI)%n", large);
        System.out.printf("  Extra Large    %4d task (150.000 - 525.000 MI)%n", xl);
        System.out.printf("  Huge           %4d task (> 525.000 MI)%n", huge);
        System.out.println("  Distribusi mengikuti kategori dataset seed GoCJ (Hussain & Aleem, 2018).");
        System.out.println();

        double lowerBound = problem.lowerBound();
        AlgorithmParams ap = new AlgorithmParams();

        // Baseline Min-Min
        int[] minMinMap = MinMin.schedule(problem);
        FitnessEvaluator evalRef = new FitnessEvaluator(problem, 1.0, 1.0);
        double makespanRef = evalRef.metrics(minMinMap).makespan;

        FitnessEvaluator evaluator = new FitnessEvaluator(problem, ap.fitnessWeight, makespanRef);
        ScheduleMetrics metMinMin = evaluator.metrics(minMinMap);

        Scheduler bat = new BatScheduler();
        Scheduler firefly = new FireflyScheduler();
        Scheduler pso = new PsoScheduler();

        // =====================================================================
        // EKSPERIMEN E1: INISIALISASI ACAK MURNI
        // =====================================================================
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(">>> [EKSPERIMEN E1] Inisialisasi Acak Murni (Seed = 42, w = 0.7)");
        System.out.println("--------------------------------------------------------------------------------");

        int[][] popE1 = PopulationFactory.random(ap.populationSize, n, m, 42L);
        ScheduleResult resBatE1 = bat.schedule(problem, ap, evaluator, PopulationFactory.copy(popE1), 42L);
        ScheduleResult resFaE1 = firefly.schedule(problem, ap, evaluator, PopulationFactory.copy(popE1), 42L);
        ScheduleResult resPsoE1 = pso.schedule(problem, ap, evaluator, PopulationFactory.copy(popE1), 42L);

        ScheduleMetrics metBatE1 = evaluator.metrics(resBatE1.mapping);
        ScheduleMetrics metFaE1 = evaluator.metrics(resFaE1.mapping);
        ScheduleMetrics metPsoE1 = evaluator.metrics(resPsoE1.mapping);

        Map<String, ScheduleMetrics> metricsE1 = new LinkedHashMap<>();
        metricsE1.put("BA", metBatE1);
        metricsE1.put("FA", metFaE1);
        metricsE1.put("PSO", metPsoE1);
        metricsE1.put("MINMIN", metMinMin);

        Map<String, int[]> mappingsE1 = new LinkedHashMap<>();
        mappingsE1.put("BA", resBatE1.mapping);
        mappingsE1.put("FA", resFaE1.mapping);
        mappingsE1.put("PSO", resPsoE1.mapping);
        mappingsE1.put("MINMIN", minMinMap);

        for (Map.Entry<String, ScheduleMetrics> entry : metricsE1.entrySet()) {
            printAlgorithmMetrics(entry.getKey(), entry.getValue(), lowerBound, problem, mappingsE1.get(entry.getKey()));
        }

        printComparisonTable("BA", metBatE1, metricsE1);
        printComparisonTable("FA", metFaE1, metricsE1);

        // =====================================================================
        // EKSPERIMEN E2: HIBRIDA (SEED MIN-MIN DI POPULASI AWAL)
        // =====================================================================
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(">>> [EKSPERIMEN E2] Hibrida: Populasi Awal Memuat Solusi Min-Min (Seed = 42, w = 0.7)");
        System.out.println("--------------------------------------------------------------------------------");

        int[][] popE2 = PopulationFactory.random(ap.populationSize, n, m, 42L);
        popE2[0] = minMinMap.clone(); // Sisipkan solusi Min-Min pada individu pertama

        ScheduleResult resBatE2 = bat.schedule(problem, ap, evaluator, PopulationFactory.copy(popE2), 42L);
        ScheduleResult resFaE2 = firefly.schedule(problem, ap, evaluator, PopulationFactory.copy(popE2), 42L);
        ScheduleResult resPsoE2 = pso.schedule(problem, ap, evaluator, PopulationFactory.copy(popE2), 42L);

        ScheduleMetrics metBatE2 = evaluator.metrics(resBatE2.mapping);
        ScheduleMetrics metFaE2 = evaluator.metrics(resFaE2.mapping);
        ScheduleMetrics metPsoE2 = evaluator.metrics(resPsoE2.mapping);

        Map<String, ScheduleMetrics> metricsE2 = new LinkedHashMap<>();
        metricsE2.put("BA-Hibrida", metBatE2);
        metricsE2.put("FA-Hibrida", metFaE2);
        metricsE2.put("PSO-Hibrida", metPsoE2);
        metricsE2.put("MINMIN", metMinMin);

        Map<String, int[]> mappingsE2 = new LinkedHashMap<>();
        mappingsE2.put("BA-Hibrida", resBatE2.mapping);
        mappingsE2.put("FA-Hibrida", resFaE2.mapping);
        mappingsE2.put("PSO-Hibrida", resPsoE2.mapping);
        mappingsE2.put("MINMIN", minMinMap);

        for (Map.Entry<String, ScheduleMetrics> entry : metricsE2.entrySet()) {
            printAlgorithmMetrics(entry.getKey(), entry.getValue(), lowerBound, problem, mappingsE2.get(entry.getKey()));
        }

        printComparisonTable("BA-Hibrida", metBatE2, metricsE2);
        printComparisonTable("FA-Hibrida", metFaE2, metricsE2);

        // =====================================================================
        // RINGKASAN KOMPARASI E1 VS E2
        // =====================================================================
        System.out.println("================================================================================");
        System.out.println("       PERBANDINGAN EFEKTIVITAS: E1 (ACAK MURNI) VS E2 (SEED MIN-MIN)");
        System.out.println("================================================================================");
        System.out.printf("%-14s %12s %12s %10s %10s %10s%n",
                "Algoritma", "Makespan E1", "Makespan E2", "Delta(%)", "DI E1", "DI E2");
        printE1vsE2Row("BA (Utama)", metBatE1, metBatE2);
        printE1vsE2Row("FA (Utama)", metFaE1, metFaE2);
        printE1vsE2Row("PSO", metPsoE1, metPsoE2);
        System.out.printf("%-14s %12.2f %12.2f %9s %10.4f %10.4f%n",
                "MINMIN", metMinMin.makespan, metMinMin.makespan, "0.00%", metMinMin.degreeOfImbalance, metMinMin.degreeOfImbalance);
        System.out.println();

        // 4. Skalabilitas Multi-Task E4 (100..1000)
        System.out.println("[Skala E4] Pengaruh jumlah task (dataset GoCJ 100 - 1000, seed 42):");
        System.out.printf("%-6s %-20s %-20s %-20s %-20s%n",
                "task", "BA makespan (gap)", "FA makespan (gap)", "PSO makespan (gap)", "MINMIN makespan (gap)");

        int[] taskSizes = {100, 300, 500, 600, 800, 1000};
        List<String[]> scaleCsvRows = new ArrayList<>();
        scaleCsvRows.add(new String[]{"Tasks", "LowerBound", "BA_Makespan", "BA_Gap", "FA_Makespan", "FA_Gap", "PSO_Makespan", "PSO_Gap", "MINMIN_Makespan", "MINMIN_Gap"});

        for (int tSize : taskSizes) {
            String path = DATA_DIR + "/GoCJ_Dataset_" + tSize + ".txt";
            if (!Files.exists(Paths.get(path))) continue;

            double[] miArr = GoCJLoader.read(path);
            Problem pScale = Problem.design(miArr);
            double lb = pScale.lowerBound();

            int[] mmMap = MinMin.schedule(pScale);
            double refMs = new FitnessEvaluator(pScale, 1.0, 1.0).metrics(mmMap).makespan;
            FitnessEvaluator ev = new FitnessEvaluator(pScale, ap.fitnessWeight, refMs);

            int[][] initPop = PopulationFactory.random(ap.populationSize, pScale.taskCount(), pScale.vmCount(), 42L);
            ScheduleMetrics mBA = ev.metrics(bat.schedule(pScale, ap, ev, PopulationFactory.copy(initPop), 42L).mapping);
            ScheduleMetrics mFA = ev.metrics(firefly.schedule(pScale, ap, ev, PopulationFactory.copy(initPop), 42L).mapping);
            ScheduleMetrics mPSO = ev.metrics(pso.schedule(pScale, ap, ev, PopulationFactory.copy(initPop), 42L).mapping);
            ScheduleMetrics mMM = ev.metrics(mmMap);

            double gapBA = (mBA.makespan - lb) / lb * 100.0;
            double gapFA = (mFA.makespan - lb) / lb * 100.0;
            double gapPSO = (mPSO.makespan - lb) / lb * 100.0;
            double gapMM = (mMM.makespan - lb) / lb * 100.0;

            System.out.printf("%-6d %8.1f (%5.2f%%)   %8.1f (%5.2f%%)   %8.1f (%5.2f%%)   %8.1f (%5.2f%%)%n",
                    tSize, mBA.makespan, gapBA, mFA.makespan, gapFA, mPSO.makespan, gapPSO, mMM.makespan, gapMM);

            scaleCsvRows.add(new String[]{
                    String.valueOf(tSize), String.format(Locale.US, "%.2f", lb),
                    String.format(Locale.US, "%.2f", mBA.makespan), String.format(Locale.US, "%.2f", gapBA),
                    String.format(Locale.US, "%.2f", mFA.makespan), String.format(Locale.US, "%.2f", gapFA),
                    String.format(Locale.US, "%.2f", mPSO.makespan), String.format(Locale.US, "%.2f", gapPSO),
                    String.format(Locale.US, "%.2f", mMM.makespan), String.format(Locale.US, "%.2f", gapMM)
            });
        }
        System.out.println();

        // 5. Export Hasil ke CSV
        Map<String, ScheduleMetrics> allMetricsExport = new LinkedHashMap<>();
        allMetricsExport.put("BA (E1 Acak)", metBatE1);
        allMetricsExport.put("FA (E1 Acak)", metFaE1);
        allMetricsExport.put("PSO (E1 Acak)", metPsoE1);
        allMetricsExport.put("BA (E2 Hibrida)", metBatE2);
        allMetricsExport.put("FA (E2 Hibrida)", metFaE2);
        allMetricsExport.put("PSO (E2 Hibrida)", metPsoE2);
        allMetricsExport.put("MINMIN", metMinMin);

        exportMetricsToCsv(RESULTS_DIR + "/summary_metrics.csv", allMetricsExport, lowerBound);
        exportScaleCsv(RESULTS_DIR + "/scale_metrics.csv", scaleCsvRows);
        exportE1vsE2Csv(RESULTS_DIR + "/e1_vs_e2_comparison.csv", metBatE1, metBatE2, metFaE1, metFaE2, metPsoE1, metPsoE2, metMinMin);

        System.out.println("Hasil analisis diekspor ke:");
        System.out.println("  - " + RESULTS_DIR + "/summary_metrics.csv");
        System.out.println("  - " + RESULTS_DIR + "/e1_vs_e2_comparison.csv");
        System.out.println("  - " + RESULTS_DIR + "/scale_metrics.csv");
    }

    private static void printE1vsE2Row(String name, ScheduleMetrics e1, ScheduleMetrics e2) {
        double impMakespan = (e1.makespan - e2.makespan) / e1.makespan * 100.0;
        System.out.printf("%-14s %12.2f %12.2f %+9.2f%% %10.4f %10.4f%n",
                name, e1.makespan, e2.makespan, impMakespan, e1.degreeOfImbalance, e2.degreeOfImbalance);
    }

    private static void printAlgorithmMetrics(String name, ScheduleMetrics sm, double lowerBound, Problem p, int[] mapping) {
        double gap = (sm.makespan - lowerBound) / lowerBound * 100.0;
        double stdBeban = calcStdDev(sm.vmFinish);

        System.out.println(name);
        System.out.printf("  Makespan              : %.4f%n", sm.makespan);
        System.out.printf("  Batas bawah makespan  : %.4f (selisih %.2f%%)%n", lowerBound, gap);
        System.out.printf("  Avg execution time    : %.4f%n", sm.avgExecTime);
        System.out.printf("  Avg waiting time      : %.4f%n", sm.avgWaitTime);
        System.out.printf("  Resource utilization  : %.2f%%%n", sm.arur * 100.0);
        System.out.printf("  Degree of imbalance   : %.4f%n", sm.degreeOfImbalance);
        System.out.printf("  Avg turnaround        : %.4f%n", sm.avgExecTime + sm.avgWaitTime);
        System.out.printf("  Throughput (task/s)   : %.4f%n", sm.throughput);
        System.out.printf("  Std beban antar VM    : %.4f%n", stdBeban);

        printCategoryBreakdown(p, mapping);
        System.out.println();
    }

    private static void printCategoryBreakdown(Problem p, int[] mapping) {
        int n = mapping.length;
        int m = p.vmCount();
        double[] ready = new double[m];
        
        double[] waitSmall = new double[2], waitMed = new double[2], waitLarge = new double[2], waitXL = new double[2], waitHuge = new double[2];

        for (int t = 0; t < n; t++) {
            int v = mapping[t];
            double exec = p.execTime(t, v);
            double wait = ready[v];
            double turn = wait + exec;
            ready[v] += exec;

            double mi = p.mi(t);
            if (mi <= 55_000) { waitSmall[0] += turn; waitSmall[1] += wait; }
            else if (mi <= 99_000) { waitMed[0] += turn; waitMed[1] += wait; }
            else if (mi <= 135_000) { waitLarge[0] += turn; waitLarge[1] += wait; }
            else if (mi <= 525_000) { waitXL[0] += turn; waitXL[1] += wait; }
            else { waitHuge[0] += turn; waitHuge[1] += wait; }
        }

        int cS = 0, cM = 0, cL = 0, cXL = 0, cH = 0;
        for (int t = 0; t < n; t++) {
            double mi = p.mi(t);
            if (mi <= 55_000) cS++;
            else if (mi <= 99_000) cM++;
            else if (mi <= 135_000) cL++;
            else if (mi <= 525_000) cXL++;
            else cH++;
        }

        if (cH > 0)  System.out.printf("    Huge         turnaround %.2f, waiting %.2f%n", waitHuge[0] / cH, waitHuge[1] / cH);
        if (cXL > 0) System.out.printf("    Extra Large  turnaround %.2f, waiting %.2f%n", waitXL[0] / cXL, waitXL[1] / cXL);
        if (cL > 0)  System.out.printf("    Large        turnaround %.2f, waiting %.2f%n", waitLarge[0] / cL, waitLarge[1] / cL);
        if (cM > 0)  System.out.printf("    Medium       turnaround %.2f, waiting %.2f%n", waitMed[0] / cM, waitMed[1] / cM);
        if (cS > 0)  System.out.printf("    Small        turnaround %.2f, waiting %.2f%n", waitSmall[0] / cS, waitSmall[1] / cS);
    }

    private static void printComparisonTable(String targetName, ScheduleMetrics target, Map<String, ScheduleMetrics> all) {
        System.out.println(targetName + " vs pembanding (positif = " + targetName + " lebih baik)");
        System.out.printf("%-14s %10s %10s %10s %10s %10s %10s%n",
                "vs", "Makespan", "AvgTurn", "AvgWait", "DI", "Util", "Speedup");

        for (Map.Entry<String, ScheduleMetrics> e : all.entrySet()) {
            if (e.getKey().equals(targetName)) continue;
            String otherName = e.getKey();
            ScheduleMetrics other = e.getValue();

            double targetTurn = target.avgExecTime + target.avgWaitTime;
            double otherTurn = other.avgExecTime + other.avgWaitTime;

            double impMakespan = (other.makespan - target.makespan) / other.makespan * 100.0;
            double impTurn = (otherTurn - targetTurn) / otherTurn * 100.0;
            double impWait = (other.avgWaitTime - target.avgWaitTime) / other.avgWaitTime * 100.0;
            double impDI = (other.degreeOfImbalance - target.degreeOfImbalance) / other.degreeOfImbalance * 100.0;
            double impUtil = (target.arur - other.arur) / other.arur * 100.0;
            double speedup = other.makespan / target.makespan;

            System.out.printf("%-14s %9.2f%% %9.2f%% %9.2f%% %9.2f%% %9.2f%% %7.3fx%n",
                    otherName, impMakespan, impTurn, impWait, impDI, impUtil, speedup);
        }
        System.out.println();
    }

    private static double calcStdDev(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        double mean = sum / arr.length;
        double sq = 0;
        for (double v : arr) sq += (v - mean) * (v - mean);
        return Math.sqrt(sq / arr.length);
    }

    private static void exportMetricsToCsv(String filePath, Map<String, ScheduleMetrics> metrics, double lowerBound) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Algorithm,Makespan,LowerBound,GapMakespanPercent,AvgExecTime,AvgWaitTime,AvgTurnaround,ResourceUtilization,DegreeOfImbalance,Throughput,StdVMFinish");
            for (Map.Entry<String, ScheduleMetrics> e : metrics.entrySet()) {
                String name = e.getKey();
                ScheduleMetrics sm = e.getValue();
                double gap = (sm.makespan - lowerBound) / lowerBound * 100.0;
                double turn = sm.avgExecTime + sm.avgWaitTime;
                double std = calcStdDev(sm.vmFinish);

                writer.printf(Locale.US, "%s,%.4f,%.4f,%.2f,%.4f,%.4f,%.4f,%.2f,%.4f,%.4f,%.4f%n",
                        name, sm.makespan, lowerBound, gap, sm.avgExecTime, sm.avgWaitTime,
                        turn, sm.arur * 100.0, sm.degreeOfImbalance, sm.throughput, std);
            }
        }
    }

    private static void exportE1vsE2Csv(String filePath,
                                       ScheduleMetrics batE1, ScheduleMetrics batE2,
                                       ScheduleMetrics faE1, ScheduleMetrics faE2,
                                       ScheduleMetrics psoE1, ScheduleMetrics psoE2,
                                       ScheduleMetrics mm) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Algorithm,Makespan_E1,Makespan_E2,ImprovementMakespanPercent,DI_E1,DI_E2,Util_E1,Util_E2");
            writeE1vsE2Row(writer, "BA", batE1, batE2);
            writeE1vsE2Row(writer, "FA", faE1, faE2);
            writeE1vsE2Row(writer, "PSO", psoE1, psoE2);
            writeE1vsE2Row(writer, "MINMIN", mm, mm);
        }
    }

    private static void writeE1vsE2Row(PrintWriter w, String algo, ScheduleMetrics e1, ScheduleMetrics e2) {
        double imp = (e1.makespan - e2.makespan) / e1.makespan * 100.0;
        w.printf(Locale.US, "%s,%.4f,%.4f,%.2f,%.4f,%.4f,%.2f,%.2f%n",
                algo, e1.makespan, e2.makespan, imp,
                e1.degreeOfImbalance, e2.degreeOfImbalance,
                e1.arur * 100.0, e2.arur * 100.0);
    }

    private static void exportScaleCsv(String filePath, List<String[]> rows) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (String[] row : rows) {
                writer.println(String.join(",", row));
            }
        }
    }
}
