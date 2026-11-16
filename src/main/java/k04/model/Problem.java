package k04.model;

import java.util.Random;

public class Problem {
    private final double[] mi, mips;
    private final int[] pes;

    public Problem(double[] mi, double[] mips) {
        this(mi, mips, ones(mips == null ? 0 : mips.length));
    }

    public Problem(double[] mi, double[] mips, int[] pes) {
        if (mi == null || mi.length == 0) throw new IllegalArgumentException("task kosong");
        if (mips == null || mips.length == 0) throw new IllegalArgumentException("VM kosong");
        if (pes == null || pes.length != mips.length) throw new IllegalArgumentException("pes tidak cocok dengan VM");
        for (double x : mi) if (!(x > 0)) throw new IllegalArgumentException("MI harus > 0");
        for (double x : mips) if (!(x > 0)) throw new IllegalArgumentException("MIPS harus > 0");
        for (int x : pes) if (x < 1) throw new IllegalArgumentException("PE harus >= 1");
        this.mi = mi.clone();
        this.mips = mips.clone();
        this.pes = pes.clone();
    }

    private static int[] ones(int n) { int[] a = new int[n]; java.util.Arrays.fill(a, 1); return a; }

    public int taskCount() { return mi.length; }
    public int vmCount() { return mips.length; }
    public double mi(int t) { return mi[t]; }
    public double mips(int v) { return mips[v]; }
    public int pes(int v) { return pes[v]; }
    public double execTime(int t, int v) { return mi[t] / mips[v]; }

    /** Lower bound makespan = total MI / total MIPS (57.500 untuk lingkungan desain). */
    public double lowerBound() {
        double sumMi = 0, cap = 0;
        for (double x : mi) sumMi += x;
        for (int v = 0; v < mips.length; v++) cap += mips[v] * pes[v];
        return sumMi / cap;
    }

    /** Lingkungan draft: 50 VM 1 PE. VM 0-16 = 500, 17-33 = 1000, 34-49 = 2000 MIPS. */
    public static Problem design(double[] mi) {
        double[] mips = new double[50];
        for (int v = 0; v < 50; v++) mips[v] = v < 17 ? 500 : (v < 34 ? 1000 : 2000);
        return new Problem(mi, mips);
    }

    /** Workload sintetis kecil, hanya untuk tes. */
    public static Problem generate(int tasks, int vms, long seed) {
        Random r = new Random(seed);
        double[] mi = new double[tasks];
        double[] mips = new double[vms];
        int[] c = {500, 1000, 1500, 2000, 2500};
        for (int t = 0; t < tasks; t++) mi[t] = 1000 + r.nextInt(19001);
        for (int v = 0; v < vms; v++) mips[v] = c[r.nextInt(c.length)];
        return new Problem(mi, mips);
    }
}