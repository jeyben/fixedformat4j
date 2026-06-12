package com.ancientprogramming.fixedformat4j.micrometer;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The instrumented manager behaves exactly like its delegate while publishing
 * fixedformat.load / fixedformat.export timers tagged with the record class.
 */
class TestMeteredManager {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final FixedFormatManager manager =
      FixedFormatMetrics.of(registry).instrument(new FixedFormatManagerImpl());

  @Test
  void loadIsTimedPerRecordClassAndReturnsTheDelegateResult() {
    BasicRecord loaded = manager.load(BasicRecord.class, "some text 00042");

    assertEquals("some text", loaded.getText());
    assertEquals(42, loaded.getAmount());

    Timer timer = registry.find("fixedformat.load")
        .tag("record.class", BasicRecord.class.getName())
        .timer();
    assertNotNull(timer, "fixedformat.load timer must be registered with record.class tag");
    assertEquals(1, timer.count());
  }

  @Test
  void exportIsTimedPerRecordClassAndReturnsTheDelegateResult() {
    BasicRecord record = new BasicRecord();
    record.setText("some text");
    record.setAmount(42);

    String exported = manager.export(record);

    assertEquals("some text 00042", exported);

    Timer timer = registry.find("fixedformat.export")
        .tag("record.class", BasicRecord.class.getName())
        .timer();
    assertNotNull(timer, "fixedformat.export timer must be registered with record.class tag");
    assertEquals(1, timer.count());
  }

  @Test
  void templateExportIsTimedThroughTheSameTimer() {
    BasicRecord record = new BasicRecord();
    record.setText("some text");
    record.setAmount(42);

    String exported = manager.export("xxxxxxxxxxxxxxx", record);

    assertEquals("some text 00042", exported);
    assertEquals(1, registry.find("fixedformat.export")
        .tag("record.class", BasicRecord.class.getName())
        .timer().count());
  }
}
