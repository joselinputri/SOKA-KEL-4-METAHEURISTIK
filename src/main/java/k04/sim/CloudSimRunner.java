package k04.sim;

import java.util.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import k04.model.FitnessEvaluator;
import k04.model.Problem;

public final class CloudSimRunner {
    private CloudSimRunner() {}

    private static Host host(int id, int pes, double mips, int ram, long bw, long storage) {
        List<Pe> list = new ArrayList<>();
        for (int i = 0; i < pes; i++) list.add(new Pe(i, new PeProvisionerSimple(mips)));
        return new Host(id, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw),
                storage, list, new VmSchedulerTimeShared(list));
    }

    /** Sesuai draft: 30 host x 4 PE x 4000 MIPS, RAM 16 GB, BW 10 Gbps, storage 1 TB. */
    public static double[] runDesign(Problem p, int[] mapping) throws Exception {
        List<Host> hosts = new ArrayList<>();
        for (int i = 0; i < 30; i++) hosts.add(host(i, 4, 4000, 16384, 10000, 1_000_000));
        return simulate(p, mapping, hosts);
    }

    /** Untuk tes kecil: 1 host per VM. */
    public static double[] run(Problem p, int[] mapping) throws Exception {
        List<Host> hosts = new ArrayList<>();
        for (int v = 0; v < p.vmCount(); v++) hosts.add(host(v, p.pes(v), p.mips(v), 4096, 10000, 1_000_000));
        return simulate(p, mapping, hosts);
    }

    private static double[] simulate(Problem p, int[] mapping, List<Host> hosts) throws Exception {
        FitnessEvaluator.validateMapping(p, mapping);
        int n = p.taskCount(), m = p.vmCount();
        Log.disable();
        CloudSim.init(1, Calendar.getInstance(), false);

        DatacenterCharacteristics ch = new DatacenterCharacteristics("x86", "Linux", "Xen",
                hosts, 10.0, 3.0, 0.05, 0.001, 0.0);
        new Datacenter("DC0", ch, new VmAllocationPolicySimple(hosts), new LinkedList<Storage>(), 0);

        DatacenterBroker broker = new DatacenterBroker("Broker");
        List<Vm> vms = new ArrayList<>();
        for (int v = 0; v < m; v++) {
            vms.add(new Vm(v, broker.getId(), p.mips(v), p.pes(v), 2048, 1000, 10000, "Xen",
                    new CloudletSchedulerSpaceShared()));
        }
        broker.submitVmList(vms);

        List<Cloudlet> cls = new ArrayList<>();
        UtilizationModel full = new UtilizationModelFull();
        for (int t = 0; t < n; t++) {
            Cloudlet c = new Cloudlet(t, (long) p.mi(t), 1, 300, 300, full, full, full);
            c.setUserId(broker.getId());
            cls.add(c);
        }
        broker.submitCloudletList(cls);
        for (int t = 0; t < n; t++) broker.bindCloudletToVm(t, mapping[t]);

        CloudSim.startSimulation();
        List<Cloudlet> done = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        if (done.size() != n) throw new IllegalStateException("cloudlet selesai " + done.size() + " dari " + n);
        double t0 = Double.MAX_VALUE;
        for (Cloudlet c : done) t0 = Math.min(t0, c.getExecStartTime());
        double[] finish = new double[m];
        for (Cloudlet c : done) finish[c.getVmId()] = Math.max(finish[c.getVmId()], c.getFinishTime() - t0);
        return finish;
    }

    public static double makespan(double[] finish) {
        double mx = 0;
        for (double f : finish) mx = Math.max(mx, f);
        return mx;
    }
}