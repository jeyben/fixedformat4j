package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.HandlerRegistry;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that a single {@link FixedFormatReader} instance is safe to use concurrently
 * from multiple threads, as documented in its Javadoc.
 */
class TestFixedFormatReaderConcurrency {

  private static final int THREAD_COUNT = 20;

  private static final String A_LINE = "AAAAAAAAAA";
  private static final String B_LINE = "BBBBBBBBBB";
  private static final String TWO_LINE_INPUT = A_LINE + "\n" + B_LINE;

  private static FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(FiveCharRecord.class, LinePattern.prefix("B"))
        .build();
  }

  @Test
  void concurrentReadCallsReturnIndependentResults() throws Exception {
    FixedFormatReader reader = multiTypeReader();
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<ReadResult>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return reader.read(new StringReader(TWO_LINE_INPUT));
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    for (Future<ReadResult> future : futures) {
      ReadResult result = future.get();
      assertEquals(1, result.get(TenCharRecord.class).size(),
          "each thread must see exactly one TenCharRecord");
      assertEquals(1, result.get(FiveCharRecord.class).size(),
          "each thread must see exactly one FiveCharRecord");
      assertEquals(A_LINE, result.get(TenCharRecord.class).get(0).getValue());
      assertEquals("BBBBB", result.get(FiveCharRecord.class).get(0).getCode());
    }
  }

  @Test
  void concurrentReadCallsDoNotCrossContaminate() throws Exception {
    FixedFormatReader tenReader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .build();

    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<List<TenCharRecord>>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      final int lineCount = (i % 3) + 1;
      futures.add(executor.submit(() -> {
        barrier.await();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < lineCount; j++) {
          if (j > 0) sb.append('\n');
          sb.append(A_LINE);
        }
        return tenReader.read(new StringReader(sb.toString())).get(TenCharRecord.class);
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    for (int i = 0; i < THREAD_COUNT; i++) {
      int expectedCount = (i % 3) + 1;
      List<TenCharRecord> records = futures.get(i).get();
      assertEquals(expectedCount, records.size(),
          "thread " + i + " expected " + expectedCount + " records but got " + records.size());
    }
  }

  @Test
  void concurrentProcessCallsDispatchToIndependentHandlers() throws Exception {
    FixedFormatReader reader = multiTypeReader();
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        AtomicInteger count = new AtomicInteger();
        reader.process(
            new StringReader(TWO_LINE_INPUT),
            new HandlerRegistry()
                .on(TenCharRecord.class, r -> count.incrementAndGet())
                .on(FiveCharRecord.class, r -> count.incrementAndGet()));
        return count.get();
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    for (Future<Integer> future : futures) {
      assertEquals(2, future.get(),
          "each thread's handler must be called exactly twice (once per record type)");
    }
  }

  @Test
  void concurrentReadCallsProduceNoExceptions() throws Exception {
    FixedFormatReader reader = multiTypeReader();
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<Void>> futures = new ArrayList<>();
    CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        try {
          for (int round = 0; round < 10; round++) {
            reader.read(new StringReader(TWO_LINE_INPUT));
          }
        } catch (Exception e) {
          errors.add(e);
        }
        return null;
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

    for (Future<Void> f : futures) {
      f.get();
    }
    assertTrue(errors.isEmpty(),
        "no exceptions expected across " + THREAD_COUNT + " concurrent threads: " + errors);
  }
}
