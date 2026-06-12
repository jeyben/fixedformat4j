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

  @Test
  void parseFailuresAreCountedWithClassAndFieldTagsAndStillPropagate() {
    org.junit.jupiter.api.Assertions.assertThrows(
        com.ancientprogramming.fixedformat4j.format.ParseException.class,
        () -> manager.load(BasicRecord.class, "some text xx,42"));

    io.micrometer.core.instrument.Counter counter = registry.find("fixedformat.parse.errors")
        .tag("record.class", BasicRecord.class.getName())
        .tag("field", "getAmount")
        .counter();
    assertNotNull(counter, "fixedformat.parse.errors counter must carry record.class and field tags");
    assertEquals(1.0, counter.count());
  }

  @Test
  void gaugeReportsDistinctRecordClassesSeenThroughThisManager() {
    manager.load(BasicRecord.class, "some text 00042");
    manager.load(BasicRecord.class, "other text00001");

    io.micrometer.core.instrument.Gauge gauge =
        registry.find("fixedformat.metadata.cache.classes").gauge();
    assertNotNull(gauge, "fixedformat.metadata.cache.classes gauge must be registered");
    assertEquals(1.0, gauge.value(), "the same class loaded twice counts once");

    BasicRecord record = new BasicRecord();
    record.setText("t");
    record.setAmount(1);
    manager.export(record);
    assertEquals(1.0, gauge.value(), "export of an already-seen class does not change the count");

    manager.load(OtherRecord.class, "x");
    assertEquals(2.0, gauge.value(), "a second distinct class increases the count");
  }

  @com.ancientprogramming.fixedformat4j.annotation.Record
  public static class OtherRecord {

    private String value;

    @com.ancientprogramming.fixedformat4j.annotation.Field(offset = 1, length = 1)
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
