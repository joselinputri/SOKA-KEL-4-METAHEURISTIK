#!/bin/bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

JAVA_BIN="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home/bin"
if [ ! -d "$JAVA_BIN" ]; then
    JAVA_BIN="$(dirname $(which java 2>/dev/null || echo '/usr/bin'))"
fi

DATASET="${1:-data/GoCJ_Dataset_500.txt}"

clear
echo "================================================================================"
echo "          DEMO SIMULASI PENJADWALAN TUGAS CLOUD (KELOMPOK 4)"
echo "          Metaheuristik: Bat Algorithm (BA) & Firefly Algorithm (FA)"
echo "          Pembanding   : Min-Min (Heuristik) & PSO (Metaheuristik)"
echo "================================================================================"
echo "Dataset Uji: $DATASET"
echo ""

echo "[1/3] Mengompilasi source code..."
"$JAVA_BIN/javac" -cp "lib/*" -d out $(find src/main/java -name "*.java")
echo "      Kompilasi selesai tanpa error."
echo ""

echo "[2/3] Menjalankan simulasi komparasi E1 vs E2..."
echo "      (Mohon tunggu beberapa detik saat algoritma melakukan optimasi)"
echo ""
"$JAVA_BIN/java" -cp "out:lib/*" k04.MetricExtractor "$DATASET" 3

echo ""
echo "[3/3] Membangkitkan visualisasi grafik perbandingan..."
MPLCONFIGDIR="/tmp/matplotlib" python3 scripts/plot_results.py

echo ""
echo "================================================================================"
echo "DEMO SELESAI!"
echo "Semua data CSV dan grafik PNG telah diperbarui di folder results/."
echo "File grafik utama:"
echo "  1. results/chart_e1_vs_e2_makespan.png"
echo "  2. results/chart_e1_vs_e2_di.png"
echo "  3. results/chart_scalability_tasks.png"
echo "================================================================================"
