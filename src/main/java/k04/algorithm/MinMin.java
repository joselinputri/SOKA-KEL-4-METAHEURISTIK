package k04.algorithm;

import k04.model.Problem;

/** Min-Min untuk VM 1 PE: pilih pasangan task-VM dengan waktu selesai tercepat, berulang. */
public final class MinMin {
    private MinMin() {}

    public static int[] schedule(Problem p) {
        int n = p.taskCount(), m = p.vmCount();
        double[] ready = new double[m];
        boolean[] done = new boolean[n];
        int[] map = new int[n];
        for (int k = 0; k < n; k++) {
            int bt = -1, bv = -1;
            double best = Double.MAX_VALUE;
            for (int t = 0; t < n; t++) {
                if (done[t]) continue;
                for (int v = 0; v < m; v++) {
                    double c = ready[v] + p.execTime(t, v);
                    if (c < best) { best = c; bt = t; bv = v; }
                }
            }
            done[bt] = true;
            map[bt] = bv;
            ready[bv] = best;
        }
        return map;
    }
}
