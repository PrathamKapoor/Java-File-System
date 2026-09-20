package org.microfs.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small allocation-conscious summary for nanosecond latency samples. */
public final class LatencyStats {
    private final List<Long> samples = new ArrayList<>();

    public void add(long nanos) {
        if (nanos < 0) {
            throw new IllegalArgumentException("Latency cannot be negative");
        }
        samples.add(nanos);
    }

    public int count() { return samples.size(); }

    public long minimum() { return percentile(0.0); }

    public long median() { return percentile(0.50); }

    public double mean() {
        if (samples.isEmpty()) return 0.0;
        long total = 0;
        for (long sample : samples) total += sample;
        return (double) total / samples.size();
    }

    public long p95() { return percentile(0.95); }

    public long maximum() { return percentile(1.0); }

    private long percentile(double quantile) {
        if (samples.isEmpty()) return 0L;
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
}
