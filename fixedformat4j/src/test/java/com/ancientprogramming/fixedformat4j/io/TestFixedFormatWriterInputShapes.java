package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatWriterInputShapes {

  private static final FixedFormatWriter WRITER = FixedFormatWriter.builder()
      .lineSeparator("\n")
      .build();

  private static TenCharRecord tenChar(String value) {
    TenCharRecord r = new TenCharRecord();
    r.setValue(value);
    return r;
  }

  private static FiveCharRecord fiveChar(String code) {
    FiveCharRecord r = new FiveCharRecord();
    r.setCode(code);
    return r;
  }

  @Test
  void writesList() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, List.of(tenChar("hello"), tenChar("world")));
    assertEquals("hello     \nworld     \n", sw.toString());
  }

  @Test
  void writesSet() {
    Set<TenCharRecord> records = new LinkedHashSet<>();
    records.add(tenChar("hello"));
    StringWriter sw = new StringWriter();
    WRITER.write(sw, records);
    assertEquals("hello     \n", sw.toString());
  }

  @Test
  void writesStream() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, Stream.of(tenChar("hello"), tenChar("world")));
    assertEquals("hello     \nworld     \n", sw.toString());
  }

  @Test
  void streamIsConsumedNotClosedByWriter() {
    AtomicBoolean closeCalled = new AtomicBoolean(false);
    Stream<TenCharRecord> stream = Stream.of(tenChar("hello"))
        .onClose(() -> closeCalled.set(true));

    WRITER.write(new StringWriter(), stream);

    assertFalse(closeCalled.get(), "writer must not close the caller's stream");
  }

  @Test
  void writesHeterogeneousMixedTypeList() {
    StringWriter sw = new StringWriter();
    List<Object> mixed = List.of(tenChar("hello"), fiveChar("AB"), tenChar("world"));
    WRITER.write(sw, mixed);
    assertEquals("hello     \nAB   \nworld     \n", sw.toString());
  }

  @Test
  void writesEmptyIterable() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, List.of());
    assertEquals("", sw.toString());
  }

  @Test
  void writesEmptyStream() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, Stream.empty());
    assertEquals("", sw.toString());
  }

  @Test
  void singleRecord() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, List.of(tenChar("hello")));
    assertEquals("hello     \n", sw.toString());
  }

  @Test
  void throwsFixedFormatExceptionForNonAnnotatedRecord() {
    assertThrows(FixedFormatException.class,
        () -> WRITER.write(new StringWriter(), List.of(new Object())));
  }
}
