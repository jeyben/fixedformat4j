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
public class ExportBenchmark {

    private FixedFormatManager manager;
    private SmallRecord smallInstance;
    private WideRecord wideInstance;
    private NestedRecord nestedInstance;

    @Setup(Level.Trial)
    public void setup() {
        manager = new FixedFormatManagerImpl();
        smallInstance  = SmallRecord.sampleInstance();
        wideInstance   = WideRecord.sampleInstance();
        nestedInstance = NestedRecord.sampleInstance();
    }

    @Benchmark
    public String exportSmall() {
        return manager.export(smallInstance);
    }

    @Benchmark
    public String exportWide() {
        return manager.export(wideInstance);
    }

    @Benchmark
    public String exportNested() {
        return manager.export(nestedInstance);
    }
}
