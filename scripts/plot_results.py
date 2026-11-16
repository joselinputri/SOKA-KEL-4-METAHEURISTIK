import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def plot_e1_vs_e2(comparison_csv, output_dir):
    if not os.path.exists(comparison_csv):
        print(f"File {comparison_csv} tidak ditemukan.")
        return

    df = pd.read_csv(comparison_csv)
    os.makedirs(output_dir, exist_ok=True)

    algos = df['Algorithm']
    x = np.arange(len(algos))
    width = 0.35

    # 1. Bar Chart: Makespan E1 vs E2
    plt.figure(figsize=(9, 5))
    plt.bar(x - width/2, df['Makespan_E1'], width, label='E1: Acak Murni', color='#e74c3c')
    plt.bar(x + width/2, df['Makespan_E2'], width, label='E2: Hibrida (Seed Min-Min)', color='#27ae60')
    plt.ylabel('Makespan (s)')
    plt.title('Efek Hibridisasi: Makespan E1 (Acak) vs E2 (Seed Min-Min)')
    plt.xticks(x, algos)
    plt.legend()
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    for i in range(len(algos)):
        plt.text(x[i] - width/2, df['Makespan_E1'][i] + 20, f"{df['Makespan_E1'][i]:.1f}", ha='center', fontsize=9)
        plt.text(x[i] + width/2, df['Makespan_E2'][i] + 20, f"{df['Makespan_E2'][i]:.1f}", ha='center', fontsize=9)
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'chart_e1_vs_e2_makespan.png'), dpi=200)
    plt.close()

    # 2. Bar Chart: Degree of Imbalance (DI) E1 vs E2
    plt.figure(figsize=(9, 5))
    plt.bar(x - width/2, df['DI_E1'], width, label='E1: Acak Murni', color='#f39c12')
    plt.bar(x + width/2, df['DI_E2'], width, label='E2: Hibrida (Seed Min-Min)', color='#2980b9')
    plt.ylabel('Degree of Imbalance (DI)')
    plt.title('Efek Hibridisasi terhadap Keseimbangan Beban (DI)')
    plt.xticks(x, algos)
    plt.legend()
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'chart_e1_vs_e2_di.png'), dpi=200)
    plt.close()

def plot_scale_metrics(scale_csv, output_dir):
    if not os.path.exists(scale_csv):
        return

    df = pd.read_csv(scale_csv)
    plt.figure(figsize=(9, 5))
    plt.plot(df['Tasks'], df['BA_Makespan'], marker='o', linewidth=2, label='BA (Utama)', color='#1f77b4')
    plt.plot(df['Tasks'], df['FA_Makespan'], marker='s', linewidth=2, label='FA (Utama)', color='#ff7f0e')
    plt.plot(df['Tasks'], df['PSO_Makespan'], marker='^', linewidth=2, label='PSO', color='#2ca02c')
    plt.plot(df['Tasks'], df['MINMIN_Makespan'], marker='d', linewidth=2, label='MINMIN', color='#d62728')
    plt.plot(df['Tasks'], df['LowerBound'], marker='x', linestyle=':', color='black', label='Lower Bound')

    plt.xlabel('Jumlah Task (GoCJ Dataset)')
    plt.ylabel('Makespan (s)')
    plt.title('Skalabilitas E4: Pengaruh Penambahan Task (100 - 1000)')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'chart_scalability_tasks.png'), dpi=200)
    plt.close()

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    results_dir = os.path.join(base_dir, 'results')
    plot_e1_vs_e2(os.path.join(results_dir, 'e1_vs_e2_comparison.csv'), results_dir)
    plot_scale_metrics(os.path.join(results_dir, 'scale_metrics.csv'), results_dir)
    print("Semua grafik perbandingan berhasil dibangkitkan di:", results_dir)
