import org.microfs.benchmark.BenchmarkEngine;
import java.nio.file.*;
import java.time.Instant;

public class RunBenchmark {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting benchmark...");
        for (int size : new int[]{128, 256, 512}) {
            System.out.println("Size: " + size);
            java.util.List<BenchmarkEngine.BenchmarkResult> results = BenchmarkEngine.runDirectoryBenchmark(size, 50);
            BenchmarkEngine.printResults(results);
        }
        System.out.println("Benchmark complete.");
    }
}
