package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Sign;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ancientprogramming.fixedformat4j.format.impl.TestFixedFormatManagerImpl.MY_RECORD_DATA;
import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatManagerConcurrency {

  private static final int THREAD_COUNT = 20;

  private FixedFormatManager manager;
  private MyRecord myRecord;

  @BeforeEach
  void setUp() {
    manager = new FixedFormatManagerImpl();

    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 4, 14, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    myRecord = new MyRecord();
    myRecord.setBooleanData(true);
    myRecord.setCharData('C');
    myRecord.setDateData(someDay.getTime());
    myRecord.setDoubleData(10.35);
    myRecord.setFloatData(20.56F);
    myRecord.setLongData(11L);
    myRecord.setIntegerData(123);
    myRecord.setStringData("some text ");
    myRecord.setBigDecimalData(new BigDecimal(-12.012));
    myRecord.setSimpleFloatData(20.56F);
  }

  @Test
  void concurrentLoadProducesCorrectRecords() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<MyRecord>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return manager.load(MyRecord.class, MY_RECORD_DATA);
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

    for (Future<MyRecord> future : futures) {
      MyRecord loaded = future.get();
      assertEquals("some text ", loaded.getStringData());
      assertTrue(loaded.isBooleanData());
      assertEquals(123, loaded.getIntegerData());
      assertEquals(11L, loaded.getLongData());
    }
  }

  @Test
  void concurrentExportProducesCorrectString() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return manager.export(myRecord);
      }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

    for (Future<String> future : futures) {
      assertEquals(MY_RECORD_DATA, future.get());
    }
  }
}
