package com.ancientprogramming.fixedformat4j.bench;

import com.ancientprogramming.fixedformat4j.bench.fixtures.RepeatingRecord;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G"})
@State(Scope.Benchmark)
public class RepeatingBenchmark {

    private FixedFormatManager manager;
    private String repeatingData;
    private RepeatingRecord repeatingRecord;

    @Setup(Level.Trial)
    public void setup() {
        manager = new FixedFormatManagerImpl();
        repeatingData = RepeatingRecord.SAMPLE_DATA;
        repeatingRecord = RepeatingRecord.sampleInstance();
    }

    @Benchmark
    public RepeatingRecord loadRepeating() {
        return manager.load(RepeatingRecord.class, repeatingData);
    }

    @Benchmark
    public String exportRepeating() {
        return manager.export(repeatingRecord);
    }
}
