package com.ancientprogramming.fixedformat4j.bench;

import com.ancientprogramming.fixedformat4j.bench.fixtures.SmallRecord;
import com.ancientprogramming.fixedformat4j.bench.fixtures.WideRecord;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.micrometer.FixedFormatMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Measures the per-call overhead of the fixedformat4j-micrometer decorator (issue #140):
 * each load/export pair runs once against the bare manager and once against the instrumented
 * one — the delta is the decorator cost. Compiled only under the {@code micrometer-bench}
 * profile because the module does not exist in the older target versions run.sh sweeps.
 */
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G"})
@State(Scope.Benchmark)
public class MeteredManagerBenchmark {

    private FixedFormatManager bare;
    private FixedFormatManager instrumented;
    private String smallData;
    private String wideData;
    private SmallRecord smallInstance;

    @Setup(Level.Trial)
    public void setup() {
        bare = new FixedFormatManagerImpl();
        instrumented = FixedFormatMetrics.of(new SimpleMeterRegistry())
            .instrument(new FixedFormatManagerImpl());
        smallData = SmallRecord.SAMPLE_DATA;
        wideData = WideRecord.SAMPLE_DATA;
        smallInstance = SmallRecord.sampleInstance();
    }

    @Benchmark
    public SmallRecord loadSmallBare() {
        return bare.load(SmallRecord.class, smallData);
    }

    @Benchmark
    public SmallRecord loadSmallInstrumented() {
        return instrumented.load(SmallRecord.class, smallData);
    }

    @Benchmark
    public WideRecord loadWideBare() {
        return bare.load(WideRecord.class, wideData);
    }

    @Benchmark
    public WideRecord loadWideInstrumented() {
        return instrumented.load(WideRecord.class, wideData);
    }

    @Benchmark
    public String exportSmallBare() {
        return bare.export(smallInstance);
    }

    @Benchmark
    public String exportSmallInstrumented() {
        return instrumented.export(smallInstance);
    }
}
