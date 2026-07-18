package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.HandlerRegistry;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Issue 182 (bug #4) - when a {@link FixedFormatWriter}/{@link FixedFormatReader} method
 * receives an already-open, caller-supplied resource plus a later parameter that turns out to be
 * {@code null}, the resource must still be closed before the {@link NullPointerException}
 * propagates, matching the "closed when this method returns" contract documented on every such
 * method. Before the fix, the later-parameter validation ran before the resource was ever wrapped
 * or closed, so the caller-supplied stream leaked.
 *
 * @since 2.0.0
 */
class TestIssue182ResourceLeakOnValidationFailure {

  private static final FixedFormatWriter WRITER = FixedFormatWriter.builder().build();
  private static final FixedFormatReader READER = FixedFormatReader.builder()
      .addMapping(TenCharRecord.class, LinePattern.matchAll())
      .build();

  private static Writer trackingWriter(boolean[] closed) {
    return new StringWriter() {
      @Override
      public void close() throws IOException {
        closed[0] = true;
        super.close();
      }
    };
  }

  private static OutputStream trackingOutputStream(boolean[] closed) {
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        closed[0] = true;
        super.close();
      }
    };
  }

  private static Reader trackingReader(boolean[] closed) {
    return new StringReader("AAAAAAAAAA") {
      @Override
      public void close() {
        closed[0] = true;
        super.close();
      }
    };
  }

  private static InputStream trackingInputStream(boolean[] closed) {
    return new ByteArrayInputStream("AAAAAAAAAA".getBytes()) {
      @Override
      public void close() throws IOException {
        closed[0] = true;
        super.close();
      }
    };
  }

  // --- FixedFormatWriter ---

  @Test
  void writerIsClosedWhenIterableRecordsIsNull() {
    boolean[] closed = {false};
    Writer writer = trackingWriter(closed);

    assertThrows(NullPointerException.class, () -> WRITER.write(writer, (Iterable<?>) null));

    assertTrue(closed[0], "writer must be closed even when records is null");
  }

  @Test
  void writerIsClosedWhenStreamRecordsIsNull() {
    boolean[] closed = {false};
    Writer writer = trackingWriter(closed);

    assertThrows(NullPointerException.class, () -> WRITER.write(writer, (java.util.stream.Stream<?>) null));

    assertTrue(closed[0], "writer must be closed even when records is null");
  }

  @Test
  void outputStreamIsClosedWhenCharsetIsNull() {
    boolean[] closed = {false};
    OutputStream out = trackingOutputStream(closed);

    assertThrows(NullPointerException.class, () -> WRITER.write(out, null, List.of()));

    assertTrue(closed[0], "out must be closed even when charset is null");
  }

  @Test
  void outputStreamIsClosedWhenRecordsIsNull() {
    boolean[] closed = {false};
    OutputStream out = trackingOutputStream(closed);

    assertThrows(NullPointerException.class,
        () -> WRITER.write(out, java.nio.charset.StandardCharsets.UTF_8, (Iterable<?>) null));

    assertTrue(closed[0], "out must be closed even when records is null");
  }

  // --- FixedFormatReader ---

  @Test
  void readerIsClosedWhenRegistryIsNull() {
    boolean[] closed = {false};
    Reader reader = trackingReader(closed);

    assertThrows(NullPointerException.class, () -> READER.process(reader, null));

    assertTrue(closed[0], "reader must be closed even when registry is null");
  }

  @Test
  void inputStreamIsClosedWhenCharsetIsNullOnProcess() {
    boolean[] closed = {false};
    InputStream in = trackingInputStream(closed);

    assertThrows(NullPointerException.class, () -> READER.process(in, null, new HandlerRegistry()));

    assertTrue(closed[0], "inputStream must be closed even when charset is null");
  }

  @Test
  void inputStreamIsClosedWhenRegistryIsNull() {
    boolean[] closed = {false};
    InputStream in = trackingInputStream(closed);

    assertThrows(NullPointerException.class,
        () -> READER.process(in, java.nio.charset.StandardCharsets.UTF_8, null));

    assertTrue(closed[0], "inputStream must be closed even when registry is null");
  }

  @Test
  void inputStreamIsClosedWhenCharsetIsNullOnOpenStream() {
    boolean[] closed = {false};
    InputStream in = trackingInputStream(closed);

    assertThrows(NullPointerException.class, () -> READER.openStream(in, (java.nio.charset.Charset) null));

    assertTrue(closed[0], "inputStream must be closed even when charset is null");
  }

  @Test
  void readerIsClosedWhenClazzIsNullOnOpenStream() {
    boolean[] closed = {false};
    Reader reader = trackingReader(closed);

    assertThrows(NullPointerException.class, () -> READER.openStream(reader, (Class<?>) null));

    assertTrue(closed[0], "reader must be closed even when clazz is null");
  }

  @Test
  void inputStreamIsClosedWhenClazzIsNullOnOpenStream() {
    boolean[] closed = {false};
    InputStream in = trackingInputStream(closed);

    assertThrows(NullPointerException.class, () -> READER.openStream(in, (Class<?>) null));

    assertTrue(closed[0], "inputStream must be closed even when clazz is null");
  }
}
