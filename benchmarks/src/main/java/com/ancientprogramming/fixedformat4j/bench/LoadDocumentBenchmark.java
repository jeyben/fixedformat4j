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
public class LoadDocumentBenchmark {

    private FixedFormatManager manager;
    private String smallDoc;
    private String wideDoc;
    private String nestedDoc;
    private String randSmallDoc;
    private String randLargeDoc;
    private int smallLen;
    private int wideLen;
    private int nestedLen;
    private int randSmallLen;
    private int randLargeLen;

    @Setup(Level.Trial)
    public void setup() {
        manager = new FixedFormatManagerImpl();
        smallDoc      = SmallRecord.SAMPLE_DATA.repeat(20);
        wideDoc       = WideRecord.SAMPLE_DATA.repeat(20);
        nestedDoc     = NestedRecord.SAMPLE_DATA.repeat(20);
        randSmallDoc  = RandomSmallRecord.SAMPLE_DATA.repeat(20);
        randLargeDoc  = RandomLargeRecord.SAMPLE_DATA.repeat(20);
        smallLen      = SmallRecord.SAMPLE_DATA.length();
        wideLen       = WideRecord.SAMPLE_DATA.length();
        nestedLen     = NestedRecord.SAMPLE_DATA.length();
        randSmallLen  = RandomSmallRecord.SAMPLE_DATA.length();
        randLargeLen  = RandomLargeRecord.SAMPLE_DATA.length();
    }

    @Benchmark
    public int loadDocument() {
        int consumed = 0;
        for (int i = 0; i < 100; i++) {
            int li = i / 5;
            switch (i % 5) {
                case 0: manager.load(SmallRecord.class,        smallDoc.substring(li * smallLen,         (li + 1) * smallLen));      break;
                case 1: manager.load(WideRecord.class,         wideDoc.substring(li * wideLen,           (li + 1) * wideLen));       break;
                case 2: manager.load(NestedRecord.class,       nestedDoc.substring(li * nestedLen,       (li + 1) * nestedLen));     break;
                case 3: manager.load(RandomSmallRecord.class,  randSmallDoc.substring(li * randSmallLen, (li + 1) * randSmallLen));  break;
                default: manager.load(RandomLargeRecord.class, randLargeDoc.substring(li * randLargeLen, (li + 1) * randLargeLen)); break;
            }
            consumed++;
        }
        return consumed;
    }
}
