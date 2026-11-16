package k04.dataset;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class GoCJLoader {
    private GoCJLoader() {}

    /** Satu baris = satu job (MI). Baris kosong dilewati. */
    public static double[] read(String path) throws IOException {
        List<Double> out = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(path))) {
            String s = line.trim();
            if (!s.isEmpty()) out.add(Double.parseDouble(s));
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }
}