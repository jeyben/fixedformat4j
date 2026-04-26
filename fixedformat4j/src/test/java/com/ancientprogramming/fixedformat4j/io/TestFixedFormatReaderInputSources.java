package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.HandlerRegistry;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderInputSources {

  @TempDir
  Path tempDir;

  private FixedFormatReader reader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .build();
  }

  private Path writeTemp(String content) throws IOException {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private static class TrackingInputStream extends ByteArrayInputStream {
    boolean closed = false;

    TrackingInputStream(byte[] buf) { super(buf); }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }
  }

  @Test
  void readsFromPath() throws IOException {
    Path path = writeTemp("hello     \nworld     ");
    List<TenCharRecord> results = reader().read(path).get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void readsFromPathWithExplicitCharset() throws IOException {
    Path path = tempDir.resolve("latin.txt");
    Files.write(path, "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));

    List<TenCharRecord> results = reader()
        .read(path, StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromInputStream() {
    InputStream is = new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    List<TenCharRecord> results = reader().read(is).get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromInputStreamWithExplicitCharset() {
    InputStream is = new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));
    List<TenCharRecord> results = reader()
        .read(is, StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void inputStreamIsClosedAfterReadAsResult() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    reader().read(is);
    assertTrue(is.closed, "InputStream should be closed after read");
  }

  @Test
  void throwsNullPointerWhenReaderIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().read((java.io.Reader) null));
  }

  @Test
  void throwsNullPointerWhenInputStreamIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().read((java.io.InputStream) null));
  }

  @Test
  void throwsNullPointerWhenInputStreamCharsetIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().read(new ByteArrayInputStream("data".getBytes()), null));
  }

  @Test
  void throwsNullPointerWhenPathIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().read((java.nio.file.Path) null));
  }

  @Test
  void throwsNullPointerWhenPathCharsetIsNull() throws IOException {
    Path path = writeTemp("hello     ");
    assertThrows(NullPointerException.class, () ->
        reader().read(path, null));
  }

  @Test
  void inputStreamIsClosedAfterProcess() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    List<String> values = new ArrayList<>();
    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .build()
        .process(is, new HandlerRegistry().on(TenCharRecord.class, r -> values.add(r.getValue())));
    assertTrue(is.closed, "InputStream should be closed after process");
  }

  // --- NPE message content ---

  @Test
  void readNullReader_npeMessageNamesParameter() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        reader().read((java.io.Reader) null));
    assertTrue(ex.getMessage().contains("reader"));
  }

  @Test
  void readNullInputStream_npeMessageNamesParameter() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        reader().read((InputStream) null));
    assertTrue(ex.getMessage().contains("inputStream"));
  }

  @Test
  void readNullPath_npeMessageNamesParameter() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        reader().read((Path) null));
    assertTrue(ex.getMessage().contains("path"));
  }

  // --- Charset default ---

  @Test
  void readInputStreamDefaultsToUtf8() {
    // U+00C0 ('À') encodes as 0xC3 0x80 in UTF-8 (2 bytes, 1 char).
    // Under ISO-8859-1 those same bytes decode as 'Ã' + U+0080, so the trimmed value differs.
    String line = "À         "; // 10 chars
    byte[] utf8Bytes = line.getBytes(StandardCharsets.UTF_8);
    List<TenCharRecord> results = reader().read(new ByteArrayInputStream(utf8Bytes))
        .get(TenCharRecord.class);
    assertEquals(1, results.size());
    assertEquals("À", results.get(0).getValue().trim());
  }

  // --- Path error message ---

  @Test
  void nonExistentPathThrowsWithPathInMessage() {
    Path missing = Path.of("/nonexistent/path/does/not/exist_fixedformat4j.txt");
    FixedFormatIOException ex = assertThrows(FixedFormatIOException.class, () ->
        reader().read(missing));
    assertTrue(ex.getMessage().contains("nonexistent"));
  }

  // --- IO error line counter ---

  @Test
  void ioErrorMidReadMessageContainsCorrectLineNumber() {
    // Subclassing BufferedReader: toBuffered() returns it as-is (instanceof check).
    BufferedReader failingOnLine3 = new BufferedReader(new StringReader("")) {
      private int callCount = 0;
      @Override
      public String readLine() throws IOException {
        callCount++;
        if (callCount == 1) return "line1     ";
        if (callCount == 2) return "line2     ";
        throw new IOException("simulated mid-read error");
      }
    };
    FixedFormatIOException ex = assertThrows(FixedFormatIOException.class, () ->
        reader().read(failingOnLine3));
    assertEquals("IO error reading line 3", ex.getMessage());
  }

  // --- toBuffered early return ---

  @Test
  void readFromAlreadyBufferedReaderWorks() {
    // Passing a BufferedReader directly exercises the instanceof early-return in toBuffered()
    BufferedReader buffered = new BufferedReader(new StringReader("hello     \nworld     "));
    List<TenCharRecord> results = reader().read(buffered).get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("world     ", results.get(1).getValue());
  }

}
