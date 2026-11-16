package k04;

import java.util.Arrays;

public final class Stats {
    private Stats() {}

    public static double mean(double[] x) {
        double s = 0;
        for (double v : x) s += v;
        return s / x.length;
    }

    public static double std(double[] x) {
        if (x.length < 2) return 0;
        double m = mean(x), s = 0;
        for (double v : x) s += (v - m) * (v - m);
        return Math.sqrt(s / (x.length - 1));
    }

    public static double median(double[] x) {
        double[] s = x.clone();
        Arrays.sort(s);
        int n = s.length;
        return n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2;
    }

    /** Wilcoxon rank-sum / Mann-Whitney U dua sisi, aproksimasi normal + koreksi ties. Mengembalikan p-value. */
    public static double mannWhitneyP(double[] a, double[] b) {
        int n1 = a.length, n2 = b.length, n = n1 + n2;
        double[][] all = new double[n][2];
        for (int i = 0; i < n1; i++) { all[i][0] = a[i]; all[i][1] = 0; }
        for (int i = 0; i < n2; i++) { all[n1 + i][0] = b[i]; all[n1 + i][1] = 1; }
        Arrays.sort(all, (x, y) -> Double.compare(x[0], y[0]));
        double[] rank = new double[n];
        double tieSum = 0;
        for (int i = 0; i < n; ) {
            int j = i;
            while (j + 1 < n && all[j + 1][0] == all[i][0]) j++;
            double r = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) rank[k] = r;
            double t = j - i + 1;
            tieSum += t * t * t - t;
            i = j + 1;
        }
        double r1 = 0;
        for (int i = 0; i < n; i++) if (all[i][1] == 0) r1 += rank[i];
        double u1 = r1 - n1 * (n1 + 1) / 2.0;
        double mu = n1 * n2 / 2.0;
        double sigma = Math.sqrt(n1 * n2 / 12.0 * ((n + 1) - tieSum / ((double) n * (n - 1))));
        if (sigma == 0) return 1.0;
        double z = (u1 - mu) / sigma;
        return 2 * (1 - normCdf(Math.abs(z)));
    }

    private static double normCdf(double z) { return 0.5 * (1 + erf(z / Math.sqrt(2))); }

    private static double erf(double x) {   // Abramowitz-Stegun 7.1.26
        double s = Math.signum(x);
        x = Math.abs(x);
        double t = 1 / (1 + 0.3275911 * x);
        double y = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return s * y;
    }
}