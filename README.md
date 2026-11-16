# Simulasi Penjadwalan Task Cloud Data Center (Kelompok 4)
Komparasi performa algoritma metaheuristik **Bat Algorithm (BA)** dan **Firefly Algorithm (FA)** dengan algoritma pembanding **Particle Swarm Optimization (PSO)** dan **Min-Min** untuk penjadwalan task independen di lingkungan CloudSim.

---

## Ringkasan Proyek

Repositori ini memuat implementasi simulasi penjadwalan komputasi awan (*cloud task scheduling*) berbasis workload nyata dari dataset **GoCJ (Google Cloud Jobs)**. 

Fokus utama proyek:
1. **Algoritma Utama**:
   - **Bat Algorithm (BA)** (versi diskret)
   - **Firefly Algorithm (FA)** (versi diskret berbasis jarak Hamming)
2. **Algoritma Pembanding**:
   - **Particle Swarm Optimization (PSO)** (representasi kontinu dengan mapping diskret)
   - **Min-Min** (algoritma heuristik deterministik sebagai acuan/baseline)
3. **Objektif Optimasi**:
   - Meminimalkan **Makespan** (waktu selesai task terakhir).
   - Menyeimbangkan beban antar mesin (**Degree of Imbalance / DI**).
   - Memaksimalkan efisiensi penggunaan sumber daya (**Resource Utilization / ARUR**).

---

## Skenario Pengujian

1. **Eksperimen E1 (Inisialisasi Acak Murni)**
   - Semua algoritma metaheuristik (BA, FA, PSO) memulai pencarian dari solusi acak.
   - Digunakan untuk melihat kemampuan eksplorasi murni masing-masing algoritma.
2. **Eksperimen E2 (Hibrida / Seeding Min-Min)**
   - Solusi awal dari Min-Min disisipkan ke dalam populasi awal BA, FA, dan PSO.
   - Bertujuan melihat efektivitas algoritma metaheuristik dalam memoles (*fine-tune*) solusi yang sudah baik menjadi jauh lebih optimal dan seimbang.
3. **Eksperimen E4 (Skalabilitas Jumlah Task)**
   - Menguji performa algoritma pada berbagai ukuran task: 100, 300, 500, 600, 800, hingga 1000 task.

---

## Struktur Folder

```text
k04/
├── data/                    # Dataset GoCJ (100 sampai 1000 task)
├── lib/
│   └── cloudsim-3.0.3.jar   # Library CloudSim
├── results/                 # Output CSV dan grafik visualisasi (PNG)
├── scripts/
│   ├── run_metric_extractor.sh  # Script utama untuk compile, run simulasi, dan buat grafik
│   └── plot_results.py          # Script Python untuk membangkitkan grafik
├── src/main/java/k04/
│   ├── algorithm/           # BatScheduler, FireflyScheduler, PsoScheduler, MinMin
│   ├── dataset/             # GoCJLoader
│   ├── model/               # Problem, FitnessEvaluator, ScheduleMetrics
│   ├── sim/                 # CloudSimRunner
│   ├── MetricExtractor.java # Ekstraktor metrik deskriptif & evaluasi komprehensif
│   └── Runner.java          # Runner eksperimen bawaan (E1 - E4)
└── pom.xml                  # Konfigurasi Maven
```

---

## Kebutuhan Sistem

- **Java JDK 17** (atau Java 8+)
- **Python 3** dengan library:
  ```bash
  pip install pandas matplotlib
  ```

---

## Cara Menjalankan

### Cara Cepat (Satu Perintah)
Jalankan script bash berikut dari terminal:
```bash
./scripts/run_metric_extractor.sh
```
*Script ini otomatis mengompilasi Java, menjalankan pengujian E1, E2, serta E4 (skalabilitas), mengekspor hasilnya ke CSV, dan menggambar grafik komparasi secara otomatis.*

Jika ingin menguji file dataset lain (misal 1000 task):
```bash
./scripts/run_metric_extractor.sh data/GoCJ_Dataset_1000.txt
```

---

### Cara Manual

1. **Kompilasi kode Java:**
   ```bash
   javac -cp "lib/*" -d out $(find src/main/java -name "*.java")
   ```

2. **Jalankan program ekstraktor metrik:**
   ```bash
   java -cp "out:lib/*" k04.MetricExtractor data/GoCJ_Dataset_500.txt
   ```

3. **Buat visualisasi grafik:**
   ```bash
   python3 scripts/plot_results.py
   ```

---

## Hasil dan Visualisasi

Hasil simulasi otomatis disimpan pada folder `results/`:
- **Tabel Data (CSV)**:
  - `results/e1_vs_e2_comparison.csv`
  - `results/summary_metrics.csv`
  - `results/scale_metrics.csv`
- **Grafik Komparasi (PNG)**:
  - `results/chart_e1_vs_e2_makespan.png` (Perbandingan makespan acak vs hibrida)
  - `results/chart_e1_vs_e2_di.png` (Tingkat keseimbangan beban antar VM)
  - `results/chart_scalability_tasks.png` (Skalabilitas 100 - 1000 task)
