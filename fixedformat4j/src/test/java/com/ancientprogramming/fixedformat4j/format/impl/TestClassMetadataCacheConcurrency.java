package com.ancientprogramming.fixedformat4j.format.impl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TestClassMetadataCacheConcurrency {

  private static final int THREAD_COUNT = 20;

  @Test
  void sameConcurrentAccessReturnsSameListInstance() throws Exception {
    ClassMetadataCache cache = new ClassMetadataCache();
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<List<FieldDescriptor>>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return cache.get(MyRecord.class);
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

    List<FieldDescriptor> reference = futures.get(0).get();
    assertEquals(10, reference.size(), "MyRecord should have 10 descriptors");
    for (Future<List<FieldDescriptor>> future : futures) {
      assertSame(reference, future.get(), "all threads must receive the same cached list instance");
    }
  }

  @Test
  void eachDescriptorHasNonNullFormatterUnderConcurrentAccess() throws Exception {
    ClassMetadataCache cache = new ClassMetadataCache();
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return cache.get(MyRecord.class).stream().allMatch(d -> d.formatter != null);
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

    for (Future<Boolean> future : futures) {
      assertTrue(future.get(), "every simple-field descriptor must have a non-null formatter");
    }
  }

  @Test
  void differentClassConcurrentBuildsDoNotCrossContaminate() throws Exception {
    Class<?>[] classes = {MyRecord.class, MultibleFieldsRecord.class, RepeatingFieldRecord.class};
    int[] expectedCounts  = {10, 4, 2};

    ClassMetadataCache cache = new ClassMetadataCache();
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<int[]>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      final int idx = i % classes.length;
      futures.add(executor.submit(() -> {
        barrier.await();
        return new int[]{idx, cache.get(classes[idx]).size()};
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

    for (Future<int[]> future : futures) {
      int[] result = future.get();
      assertEquals(expectedCounts[result[0]], result[1],
          "wrong descriptor count for " + classes[result[0]].getSimpleName());
    }
  }
}
