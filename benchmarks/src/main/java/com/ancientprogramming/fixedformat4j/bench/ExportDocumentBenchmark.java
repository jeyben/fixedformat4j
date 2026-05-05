package com.ancientprogramming.fixedformat4j.bench;

import com.ancientprogramming.fixedformat4j.bench.fixtures.*;
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
public class ExportDocumentBenchmark {

    private FixedFormatManager manager;
    private SmallRecord[]       smallInstances;
    private WideRecord[]        wideInstances;
    private NestedRecord[]      nestedInstances;
    private RandomSmallRecord[] randSmallInstances;
    private RandomLargeRecord[] randLargeInstances;

    @Setup(Level.Trial)
    public void setup() {
        manager = new FixedFormatManagerImpl();
        smallInstances     = new SmallRecord[20];
        wideInstances      = new WideRecord[20];
        nestedInstances    = new NestedRecord[20];
        randSmallInstances = new RandomSmallRecord[20];
        randLargeInstances = new RandomLargeRecord[20];
        for (int i = 0; i < 20; i++) {
            smallInstances[i]     = SmallRecord.sampleInstance();
            wideInstances[i]      = WideRecord.sampleInstance();
            nestedInstances[i]    = NestedRecord.sampleInstance();
            randSmallInstances[i] = RandomSmallRecord.sampleInstance();
            randLargeInstances[i] = RandomLargeRecord.sampleInstance();
        }
    }

    @Benchmark
    public String exportDocument() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            int li = i / 5;
            switch (i % 5) {
                case 0: sb.append(manager.export(smallInstances[li]));      break;
                case 1: sb.append(manager.export(wideInstances[li]));       break;
                case 2: sb.append(manager.export(nestedInstances[li]));     break;
                case 3: sb.append(manager.export(randSmallInstances[li]));  break;
                default: sb.append(manager.export(randLargeInstances[li])); break;
            }
        }
        return sb.toString();
    }
}
