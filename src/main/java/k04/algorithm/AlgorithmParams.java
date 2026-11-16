package k04.algorithm;

/** Parameter bersama dan per algoritma (nilai awal sesuai draft bagian 6.6). */
public class AlgorithmParams {
    // bersama
    public int populationSize = 30;
    public int evalsPerTask = 100;          // maksimum evaluasi = evalsPerTask * jumlah task
    public double fitnessWeight = 0.7;

    // PSO
    public double psoWStart = 0.9;
    public double psoWEnd = 0.4;
    public double psoC1 = 2.0;
    public double psoC2 = 2.0;
    public double psoVmaxFraction = 0.5;    // vmax = fraksi * jumlah VM (25 untuk 50 VM)

    // Firefly (diskret)
    public double faBeta0 = 1.0;
    public double faGamma = 1.0;
    public double faAlpha0 = 0.01;
    public double faAlphaEndRatio = 0.1;    // alpha turun 10x di akhir run

    // Bat (diskret)
    public double batFMax = 0.5;
    public double batA0 = 0.9;
    public double batR0 = 0.5;
    public double batAlpha = 0.9;
    public double batGamma = 0.9;
    public double batKmaxFraction = 0.02;   // kmax = fraksi * jumlah task

    public int maxEvaluations(int taskCount) {
        return evalsPerTask * taskCount;
    }
}