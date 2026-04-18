package com.ancientprogramming.fixedformat4j.bench;

import com.ancientprogramming.fixedformat4j.bench.fixtures.NestedRecord;
import com.ancientprogramming.fixedformat4j.bench.fixtures.SmallRecord;
import com.ancientprogramming.fixedformat4j.bench.fixtures.WideRecord;
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
public class LoadBenchmark {

    private FixedFormatManager manager;
    private String smallData;
    private String wideData;
    private String nestedData;

    @Setup(Level.Trial)
    public void setup() {
        manager = new FixedFormatManagerImpl();
        smallData  = SmallRecord.SAMPLE_DATA;
        wideData   = WideRecord.SAMPLE_DATA;
        nestedData = NestedRecord.SAMPLE_DATA;
    }

    @Benchmark
    public SmallRecord loadSmall() {
        return manager.load(SmallRecord.class, smallData);
    }

    @Benchmark
    public WideRecord loadWide() {
        return manager.load(WideRecord.class, wideData);
    }

    @Benchmark
    public NestedRecord loadNested() {
        return manager.load(NestedRecord.class, nestedData);
    }
}
