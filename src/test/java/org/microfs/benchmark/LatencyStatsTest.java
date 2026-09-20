package org.microfs.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatencyStatsTest {
    @Test
    void computesMinimumMedianMeanP95AndMaximum() {
        LatencyStats stats = new LatencyStats();
        for (long value : new long[]{1, 2, 3, 4, 5}) {
            stats.add(value);
        }

        assertEquals(1L, stats.minimum());
        assertEquals(3L, stats.median());
        assertEquals(3.0, stats.mean());
        assertEquals(5L, stats.p95());
        assertEquals(5L, stats.maximum());
    }
}
