package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestFixedFormatWriterConcurrency {

  @Test
  void concurrentWritesToIndependentWritersProduceSameOutput() throws Exception {
    FixedFormatWriter writer = FixedFormatWriter.builder()
        .lineSeparator("\n")
        .build();

    TenCharRecord a = new TenCharRecord();
    a.setValue("hello");
    TenCharRecord b = new TenCharRecord();
    b.setValue("world");
    List<TenCharRecord> records = List.of(a, b);
    String expected = "hello     \nworld     \n";

    int threadCount = 10;
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      futures.add(pool.submit(() -> {
        StringWriter sw = new StringWriter();
        writer.write(sw, records);
        return sw.toString();
      }));
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    for (Future<String> f : futures) {
      assertEquals(expected, f.get());
    }
  }
}
