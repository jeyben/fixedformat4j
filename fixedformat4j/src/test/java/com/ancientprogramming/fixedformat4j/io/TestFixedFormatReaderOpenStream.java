package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderOpenStream {

  @TempDir
  Path tempDir;

  private FixedFormatReader singleTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .build();
  }

  private FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(FiveCharRecord.class, LinePattern.prefix("B"))
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
    @Override public void close() throws IOException { closed = true; super.close(); }
  }

  // -----------------------------------------------------------------------
  // Group 1 — openStream(Reader)
  // -----------------------------------------------------------------------

  @Test
  void openStream_reader_returnsAllRecords() {
    try (Stream<Object> s = singleTypeReader().openStream(new StringReader("hello     \nworld     \nfoo       "))) {
      List<Object> records = s.collect(Collectors.toList());
      assertEquals(3, records.size());
      assertEquals("hello", ((TenCharRecord) records.get(0)).getValue().trim());
      assertEquals("world", ((TenCharRecord) records.get(1)).getValue().trim());
      assertEquals("foo",   ((TenCharRecord) records.get(2)).getValue().trim());
    }
  }

  @Test
  void openStream_reader_emptyInput_returnsEmptyStream() {
    try (Stream<Object> s = singleTypeReader().openStream(new StringReader(""))) {
      assertEquals(0, s.count());
    }
  }

  @Test
  void openStream_nullReader_throwsNpe() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        singleTypeReader().openStream((Reader) null));
    assertTrue(ex.getMessage().contains("reader"));
  }

  // -----------------------------------------------------------------------
  // Group 2 — input source overloads
  // -----------------------------------------------------------------------

  @Test
  void openStream_inputStream_utf8Default() {
    byte[] bytes = "hello     \nworld     ".getBytes(StandardCharsets.UTF_8);
    try (Stream<Object> s = singleTypeReader().openStream(new ByteArrayInputStream(bytes))) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_inputStream_explicitCharset() {
    byte[] bytes = "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1);
    try (Stream<Object> s = singleTypeReader().openStream(new ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1)) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_path_utf8Default() throws IOException {
    Path p = writeTemp("hello     \nworld     ");
    try (Stream<Object> s = singleTypeReader().openStream(p)) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_path_explicitCharset() throws IOException {
    Path p = tempDir.resolve("latin.txt");
    Files.write(p, "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));
    try (Stream<Object> s = singleTypeReader().openStream(p, StandardCharsets.ISO_8859_1)) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_nullInputStream_throwsNpe() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        singleTypeReader().openStream((InputStream) null));
    assertTrue(ex.getMessage().contains("inputStream"));
  }

  @Test
  void openStream_nullInputStreamCharset_throwsNpe() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        singleTypeReader().openStream(new ByteArrayInputStream(new byte[0]), (java.nio.charset.Charset) null));
    assertTrue(ex.getMessage().contains("charset"));
  }

  @Test
  void openStream_nullPath_throwsNpe() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        singleTypeReader().openStream((Path) null));
    assertTrue(ex.getMessage().contains("path"));
  }

  @Test
  void openStream_nullPathCharset_throwsNpe() throws IOException {
    Path p = writeTemp("hello     ");
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        singleTypeReader().openStream(p, (java.nio.charset.Charset) null));
    assertTrue(ex.getMessage().contains("charset"));
  }

  @Test
  void openStream_nonExistentPath_throwsFixedFormatIOException() {
    Path missing = Path.of("/nonexistent/does-not-exist-fixedformat4j.txt");
    assertThrows(FixedFormatIOException.class, () ->
        singleTypeReader().openStream(missing));
  }

  // -----------------------------------------------------------------------
  // Group 3 — resource management
  // -----------------------------------------------------------------------

  @Test
  void openStream_closingStreamClosesUnderlyingInputStream() {
    TrackingInputStream is = new TrackingInputStream("hello     ".getBytes(StandardCharsets.UTF_8));
    try (Stream<Object> s = singleTypeReader().openStream(is)) {
      s.count();
    }
    assertTrue(is.closed, "InputStream must be closed after stream is closed");
  }

  @Test
  void openStream_earlyTermination_closesUnderlyingInputStream() {
    TrackingInputStream is = new TrackingInputStream(
        "hello     \nworld     \nfoo       ".getBytes(StandardCharsets.UTF_8));
    try (Stream<Object> s = singleTypeReader().openStream(is)) {
      s.findFirst();
    }
    assertTrue(is.closed, "InputStream must be closed even after early termination");
  }

  @Test
  void openStream_ioErrorDuringRead_throwsFixedFormatIOExceptionWithLineNumber() {
    BufferedReader failingOnLine3 = new BufferedReader(new StringReader("")) {
      private int callCount = 0;
      @Override public String readLine() throws IOException {
        callCount++;
        if (callCount == 1) return "line1     ";
        if (callCount == 2) return "line2     ";
        throw new IOException("simulated error");
      }
    };
    FixedFormatIOException ex = assertThrows(FixedFormatIOException.class, () -> {
      try (Stream<Object> s = singleTypeReader().openStream(failingOnLine3)) {
        s.count();
      }
    });
    assertEquals("IO error reading line 3", ex.getMessage());
  }

  // -----------------------------------------------------------------------
  // Group 4 — strategy interaction via Spliterator
  // -----------------------------------------------------------------------

  @Test
  void openStream_allMatchesStrategy_emitsMultipleRecordsFromOneLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();
    try (Stream<Object> s = reader.openStream(new StringReader("AAAAAAAAAA"))) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_parseErrorSkipAndLog_streamContinuesAfterErrorLine() {
    AtomicInteger callCount = new AtomicInteger(0);
    FixedFormatManager manager = new FixedFormatManager() {
      @SuppressWarnings("unchecked")
      @Override public <T> T load(Class<T> clazz, String data) {
        if (callCount.incrementAndGet() == 2) throw new FixedFormatException("bad data");
        TenCharRecord r = new TenCharRecord(); r.setValue("ok"); return (T) r;
      }
      @Override public <T> String export(T instance) { return ""; }
      @Override public <T> String export(String template, T instance) { return ""; }
    };

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy(ParseErrorStrategy.skipAndLog())
        .manager(manager)
        .build();

    try (Stream<Object> s = reader.openStream(new StringReader("line1     \nline2     \nline3     "))) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_unmatchSkip_streamContinuesAfterUnmatchedLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .unmatchStrategy(UnmatchStrategy.skip())
        .build();
    try (Stream<Object> s = reader.openStream(new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"))) {
      assertEquals(2, s.count());
    }
  }

  // -----------------------------------------------------------------------
  // Group 5 — typed openStream(source, Class<T>)
  // -----------------------------------------------------------------------

  @Test
  void openStream_typedReader_singleType_returnsTypedStreamWithoutCast() {
    try (Stream<TenCharRecord> s = singleTypeReader().openStream(new StringReader("hello     \nworld     "), TenCharRecord.class)) {
      List<TenCharRecord> records = s.collect(Collectors.toList());
      assertEquals(2, records.size());
      assertEquals("hello", records.get(0).getValue().trim());
    }
  }

  @Test
  void openStream_typedPath_multiType_filtersToRequestedType() throws IOException {
    Path p = writeTemp("AAAAAAAAAA\nBBBBB\nAAAAAAAAAAAA");
    try (Stream<TenCharRecord> s = multiTypeReader().openStream(p, TenCharRecord.class)) {
      assertEquals(2, s.count());
    }
  }

  @Test
  void openStream_typedInputStream_multiType_filtersToRequestedType() {
    byte[] bytes = "AAAAAAAAAA\nBBBBB\nAAAAAAAAAAAA".getBytes(StandardCharsets.UTF_8);
    try (Stream<FiveCharRecord> s = multiTypeReader().openStream(new ByteArrayInputStream(bytes), FiveCharRecord.class)) {
      assertEquals(1, s.count());
    }
  }

  @Test
  void openStream_typedReader_noRecordsMatchType_returnsEmptyStream() {
    try (Stream<FiveCharRecord> s = multiTypeReader().openStream(new StringReader("AAAAAAAAAA\nAAAAAAAAAAAA"), FiveCharRecord.class)) {
      assertEquals(0, s.count());
    }
  }

  @Test
  void openStream_typedReader_nullClass_throwsNpe() {
    NullPointerException ex = assertThrows(NullPointerException.class, () ->
        singleTypeReader().openStream(new StringReader(""), (Class<Object>) null));
    assertTrue(ex.getMessage().contains("clazz"));
  }

  // -----------------------------------------------------------------------
  // Group 6 — concurrency
  // -----------------------------------------------------------------------

  @Test
  void openStream_concurrentCallsOnSameReader_produceIndependentStreams() throws InterruptedException {
    int threadCount = 10;
    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Throwable> errors = new CopyOnWriteArrayList<>();
    List<Long> sizes = new CopyOnWriteArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int lineCount = (i % 3) + 1;
      executor.submit(() -> {
        try {
          barrier.await();
          StringBuilder sb = new StringBuilder();
          for (int j = 0; j < lineCount; j++) {
            if (j > 0) sb.append('\n');
            sb.append("line      ");
          }
          try (Stream<Object> s = singleTypeReader().openStream(new StringReader(sb.toString()))) {
            sizes.add(s.count());
          }
        } catch (Exception e) {
          errors.add(e);
        }
      });
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertTrue(errors.isEmpty(), "Concurrent streams must not produce errors: " + errors);
    assertEquals(threadCount, sizes.size());
  }
}
