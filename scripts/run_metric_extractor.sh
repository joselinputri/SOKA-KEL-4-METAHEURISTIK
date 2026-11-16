#!/bin/bash
set -e

# Tentukan direktori root project
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# Tentukan path Java (menggunakan JDK 17 yang terpasang)
JAVA_BIN="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home/bin"
if [ ! -d "$JAVA_BIN" ]; then
    JAVA_BIN="$(dirname $(which java 2>/dev/null || echo '/usr/bin'))"
fi

DATASET="${1:-data/GoCJ_Dataset_500.txt}"
RUNS="${2:-5}"

echo "1. Mengompilasi source code..."
"$JAVA_BIN/javac" -cp "lib/*" -d out $(find src/main/java -name "*.java")

echo "2. Menjalankan MetricExtractor (Dataset: $DATASET, Runs: $RUNS)..."
"$JAVA_BIN/java" -cp "out:lib/*" k04.MetricExtractor "$DATASET" "$RUNS"

MPLCONFIGDIR="/tmp/matplotlib" python3 scripts/plot_results.py

