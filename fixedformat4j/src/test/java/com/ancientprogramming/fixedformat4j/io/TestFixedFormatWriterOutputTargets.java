package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatWriterOutputTargets {

  private static final FixedFormatWriter WRITER = FixedFormatWriter.builder()
      .lineSeparator("\n")
      .build();

  private static TenCharRecord record(String value) {
    TenCharRecord r = new TenCharRecord();
    r.setValue(value);
    return r;
  }

  // --- Writer target ---

  @Test
  void writesToWriter() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, List.of(record("hello"), record("world")));
    assertEquals("hello     \nworld     \n", sw.toString());
  }

  @Test
  void writesToWriterStream() {
    StringWriter sw = new StringWriter();
    WRITER.write(sw, List.of(record("hello"), record("world")).stream());
    assertEquals("hello     \nworld     \n", sw.toString());
  }

  @Test
  void writerIsClosedAfterWrite() throws IOException {
    boolean[] closed = {false};
    Writer trackingWriter = new StringWriter() {
      @Override
      public void close() throws IOException {
        closed[0] = true;
        super.close();
      }
    };

    WRITER.write(trackingWriter, List.of(record("hello")));

    assertTrue(closed[0], "writer must be closed after write");
  }

  @Test
  void writerIsClosedAfterStreamWrite() throws IOException {
    boolean[] closed = {false};
    Writer trackingWriter = new StringWriter() {
      @Override
      public void close() throws IOException {
        closed[0] = true;
        super.close();
      }
    };

    WRITER.write(trackingWriter, List.of(record("hello")).stream());

    assertTrue(closed[0], "writer must be closed after stream write");
  }

  // --- OutputStream target ---

  @Test
  void writesToOutputStream() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    WRITER.write(baos, List.of(record("hello")));
    assertEquals("hello     \n", baos.toString(StandardCharsets.UTF_8.name()));
  }

  @Test
  void writesToOutputStreamWithExplicitCharset() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    WRITER.write(baos, StandardCharsets.ISO_8859_1, List.of(record("hello")));
    assertEquals("hello     \n", baos.toString(StandardCharsets.ISO_8859_1.name()));
  }

  @Test
  void writesToOutputStreamStream() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    WRITER.write(baos, List.of(record("hello")).stream());
    assertEquals("hello     \n", baos.toString(StandardCharsets.UTF_8.name()));
  }

  @Test
  void writesToOutputStreamWithExplicitCharsetStream() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    WRITER.write(baos, StandardCharsets.ISO_8859_1, List.of(record("hello")).stream());
    assertEquals("hello     \n", baos.toString(StandardCharsets.ISO_8859_1.name()));
  }

  // --- Path target ---

  @Test
  void writesToPath(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("out.txt");
    WRITER.write(file, List.of(record("hello"), record("world")));
    assertEquals("hello     \nworld     \n", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void writesToPathWithExplicitCharset(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("out.txt");
    WRITER.write(file, StandardCharsets.ISO_8859_1, List.of(record("hello")));
    assertEquals("hello     \n", Files.readString(file, StandardCharsets.ISO_8859_1));
  }

  @Test
  void writesToPathStream(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("out.txt");
    WRITER.write(file, List.of(record("hello")).stream());
    assertEquals("hello     \n", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void writesToPathWithExplicitCharsetStream(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("out.txt");
    WRITER.write(file, StandardCharsets.ISO_8859_1, List.of(record("hello")).stream());
    assertEquals("hello     \n", Files.readString(file, StandardCharsets.ISO_8859_1));
  }

  @Test
  void writingToPathTruncatesExistingFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("out.txt");
    Files.writeString(file, "previous content that should be gone");

    WRITER.write(file, List.of(record("hello")));

    assertEquals("hello     \n", Files.readString(file, StandardCharsets.UTF_8));
  }

  // --- Null safety ---

  @Test
  void throwsNullPointerWhenWriterIsNull() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write((Writer) null, List.of())
    );
  }

  @Test
  void throwsNullPointerWhenIterableRecordsIsNullForWriter() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(new StringWriter(), (Iterable<?>) null)
    );
  }

  @Test
  void throwsNullPointerWhenStreamRecordsIsNullForWriter() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(new StringWriter(), (java.util.stream.Stream<?>) null)
    );
  }

  @Test
  void throwsNullPointerWhenOutputStreamIsNull() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write((OutputStream) null, List.of())
    );
  }

  @Test
  void throwsNullPointerWhenOutputStreamCharsetIsNull() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(new ByteArrayOutputStream(), null, List.of())
    );
  }

  @Test
  void throwsNullPointerWhenOutputStreamCharsetIsNullForStream() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(new ByteArrayOutputStream(), null, List.of().stream())
    );
  }

  @Test
  void throwsNullPointerWhenStreamRecordsIsNullForOutputStreamWithCharset() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(new ByteArrayOutputStream(), StandardCharsets.UTF_8, (java.util.stream.Stream<?>) null)
    );
  }

  @Test
  void throwsNullPointerWhenPathIsNull() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write((Path) null, List.of())
    );
  }

  @Test
  void throwsNullPointerWhenPathCharsetIsNull() {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(Path.of("out.txt"), null, List.of())
    );
  }

  @Test
  void throwsNullPointerWhenPathCharsetIsNullForStream(@TempDir Path dir) {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(dir.resolve("out.txt"), null, List.of().stream())
    );
  }

  @Test
  void throwsNullPointerWhenStreamRecordsIsNullForPathWithCharset(@TempDir Path dir) {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(dir.resolve("out.txt"), StandardCharsets.UTF_8, (java.util.stream.Stream<?>) null)
    );
  }

  @Test
  void throwsNullPointerWhenIterableRecordsIsNullForPath(@TempDir Path dir) {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(dir.resolve("out.txt"), (Iterable<?>) null)
    );
  }

  @Test
  void throwsNullPointerWhenStreamRecordsIsNullForPath(@TempDir Path dir) {
    assertThrows(NullPointerException.class, () ->
        WRITER.write(dir.resolve("out.txt"), (java.util.stream.Stream<?>) null)
    );
  }

  // --- IO error handling ---

  @Test
  void wrapsIoExceptionWhenPathCannotBeOpenedForIterableWithCharset(@TempDir Path dir) {
    Path nonExistentDir = dir.resolve("missing-dir/out.txt");
    assertThrows(FixedFormatIOException.class, () ->
        WRITER.write(nonExistentDir, StandardCharsets.UTF_8, List.of(record("hello")))
    );
  }

  @Test
  void wrapsIoExceptionWhenPathCannotBeOpenedForStream(@TempDir Path dir) {
    Path nonExistentDir = dir.resolve("missing-dir/out.txt");
    assertThrows(FixedFormatIOException.class, () ->
        WRITER.write(nonExistentDir, List.of(record("hello")).stream())
    );
  }

  @Test
  void wrapsIoExceptionWhenPathCannotBeOpenedForStreamWithCharset(@TempDir Path dir) {
    Path nonExistentDir = dir.resolve("missing-dir/out.txt");
    assertThrows(FixedFormatIOException.class, () ->
        WRITER.write(nonExistentDir, StandardCharsets.UTF_8, List.of(record("hello")).stream())
    );
  }

  @Test
  void wrapsIoExceptionInFixedFormatIOException() {
    Writer failingWriter = new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        throw new IOException("disk full");
      }
      @Override public void flush() throws IOException {}
      @Override public void close() throws IOException {}
    };

    assertThrows(FixedFormatIOException.class, () ->
        WRITER.write(failingWriter, List.of(record("hello")))
    );
  }
}
