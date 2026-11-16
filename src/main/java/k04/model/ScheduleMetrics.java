package k04.model;

/** Metrik hasil satu jadwal. T(v) = waktu selesai cloudlet terakhir di VM v (0 bila VM kosong). */
public class ScheduleMetrics {
    public final double makespan;
    public final double degreeOfImbalance;   // (Tmax - Tmin) / Tavg
    public final double arur;                // Tavg / makespan, 0..1
    public final double throughput;          // task per detik
    public final double avgExecTime;
    public final double avgWaitTime;
    public final double[] vmFinish;

    public ScheduleMetrics(double[] vmFinish, int taskCount, double avgExecTime, double avgWaitTime) {
        double max = 0, min = Double.MAX_VALUE, sum = 0;
        for (double f : vmFinish) {
            max = Math.max(max, f);
            min = Math.min(min, f);
            sum += f;
        }
        double avg = sum / vmFinish.length;
        this.vmFinish = vmFinish.clone();
        this.makespan = max;
        this.degreeOfImbalance = avg > 0 ? (max - min) / avg : 0;
        this.arur = max > 0 ? avg / max : 0;
        this.throughput = max > 0 ? taskCount / max : 0;
        this.avgExecTime = avgExecTime;
        this.avgWaitTime = avgWaitTime;
    }
}