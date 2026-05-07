package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriterBuilder;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatWriterBuilder {

  @Test
  void buildsWithNoOptions() {
    assertNotNull(FixedFormatWriter.builder().build());
  }

  @Test
  void builderIsFluentReturnsItself() {
    FixedFormatWriterBuilder builder = FixedFormatWriter.builder();
    assertSame(builder, builder.manager(FixedFormatManagerImpl.create()));
    assertSame(builder, builder.lineSeparator("\n"));
    assertSame(builder, builder.charset(StandardCharsets.UTF_8));
  }

  @Test
  void throwsNullPointerWhenManagerIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatWriter.builder().manager(null)
    );
  }

  @Test
  void throwsNullPointerWhenLineSeparatorIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatWriter.builder().lineSeparator(null)
    );
  }

  @Test
  void throwsNullPointerWhenCharsetIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatWriter.builder().charset(null)
    );
  }

  @Test
  void customManagerIsUsedWhenWriting() {
    boolean[] exportCalled = {false};
    FixedFormatManager trackingManager = new FixedFormatManagerImpl() {
      @Override
      public <T> String export(T instance) {
        exportCalled[0] = true;
        return super.export(instance);
      }
    };

    FixedFormatWriter writer = FixedFormatWriter.builder()
        .manager(trackingManager)
        .lineSeparator("\n")
        .build();

    TenCharRecord record = new TenCharRecord();
    record.setValue("hello");
    writer.write(new StringWriter(), List.of(record));

    assertTrue(exportCalled[0], "custom manager export must be called");
  }

  @Test
  void customLineSeparatorIsAppliedWhenWriting() {
    FixedFormatWriter writer = FixedFormatWriter.builder()
        .lineSeparator("\r\n")
        .build();

    TenCharRecord record = new TenCharRecord();
    record.setValue("hello");
    StringWriter sw = new StringWriter();
    writer.write(sw, List.of(record));

    assertTrue(sw.toString().endsWith("\r\n"), "output must use CRLF separator");
  }
}
