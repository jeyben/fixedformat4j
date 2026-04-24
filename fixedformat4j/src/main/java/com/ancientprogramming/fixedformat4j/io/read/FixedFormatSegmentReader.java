/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.io.read;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.io.segment.ParsedSegment;
import com.ancientprogramming.fixedformat4j.io.segment.Segment;
import com.ancientprogramming.fixedformat4j.io.segment.UnmatchedSegment;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Reads a fixed-format file or stream line by line (or chunk by chunk for packed lines) and returns
 * every record as an ordered {@link List} of {@link Segment} entries, preserving the exact source
 * order for read-edit-write round trips.
 *
 * <p>Lines that match a registered pattern are parsed into a {@link ParsedSegment}; lines or chunks
 * that do not match any pattern become an {@link UnmatchedSegment}. Lines excluded by the
 * {@link FixedFormatSegmentReaderBuilder#includeLines(Predicate) includeLines} predicate also
 * become a single {@link UnmatchedSegment} holding the entire raw line. No content is ever
 * silently dropped.</p>
 *
 * <p>The per-line {@link LineSlicingStrategy} controls whether a physical line is treated as one
 * record or sliced into fixed-width chunks:</p>
 * <ul>
 *   <li>{@link LineSlicingStrategy#singleRecord()} (default) — one record per line</li>
 *   <li>{@link LineSlicingStrategy#packed(int)} — multiple records packed per line</li>
 *   <li>{@link LineSlicingStrategy#mixed(java.util.function.Predicate, int)} — some lines single,
 *       some packed</li>
 * </ul>
 *
 * <p>Quick start — single record per line:</p>
 * <pre>{@code
 * FixedFormatSegmentReader reader = FixedFormatSegmentReader.builder()
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .build();
 *
 * List<Segment> segments = reader.readAsSegments(new File("data.txt"));
 *
 * segments.stream()
 *     .filter(s -> s instanceof ParsedSegment && ((ParsedSegment<?>) s).isOf(DetailRecord.class))
 *     .map(s -> (ParsedSegment<DetailRecord>) s)
 *     .forEach(ps -> ps.getRecord().setAmount(ps.getRecord().getAmount() * 2));
 *
 * new com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter(
 *         new com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl())
 *     .write(segments, new File("data-updated.txt"));
 * }</pre>
 *
 * <p>Quick start — packed records (multiple records per line):</p>
 * <pre>{@code
 * FixedFormatSegmentReader reader = FixedFormatSegmentReader.builder()
 *     .lineSlicing(LineSlicingStrategy.packed(128))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .build();
 * }</pre>
 *
 * <p>Quick start — mixed file (header single-record, details packed, trailer single-record):</p>
 * <pre>{@code
 * FixedFormatSegmentReader reader = FixedFormatSegmentReader.builder()
 *     .lineSlicing(LineSlicingStrategy.mixed(line -> line.startsWith("DTL"), 128))
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .addMapping(TrailerRecord.class, new RegexFixedFormatMatchPattern("^TRL"))
 *     .build();
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 * @see LineSlicingStrategy
 * @see com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter
 * @see FixedFormatReader
 */
public class FixedFormatSegmentReader {

  private final FixedFormatLineProcessor processor;
  private final LineSlicingStrategy lineSlicingStrategy;
  private final PartialChunkStrategy partialChunkStrategy;
  private final Predicate<String> lineFilter;

  FixedFormatSegmentReader(FixedFormatSegmentReaderBuilder builder) {
    this.lineFilter = builder.lineFilter;
    this.lineSlicingStrategy = builder.lineSlicingStrategy;
    this.partialChunkStrategy = builder.partialChunkStrategy;
    this.processor = new FixedFormatLineProcessor(
        List.copyOf(builder.mappings),
        builder.multiMatchStrategy,
        UnmatchStrategy.skip(),  // never invoked — segment reader uses the 4-arg processLine
        builder.parseErrorStrategy,
        line -> true,  // lineFilter applied at reader level for consistency across all slicing modes
        builder.manager);
  }

  /**
   * Eagerly reads all lines from {@code reader} and returns an ordered list of {@link Segment}
   * entries preserving the exact source order.
   *
   * @param reader the source of lines; wrapped in a {@link BufferedReader} if not already one
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Segment> readAsSegments(Reader reader) {
    List<Segment> segments = new ArrayList<>();
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};
    try (buffered) {
      String line;
      while ((line = buffered.readLine()) != null) {
        long lineNum = ++lineCounter[0];
        if (!lineFilter.test(line)) {
          segments.add(new UnmatchedSegment(line));
          continue;
        }
        OptionalInt width = lineSlicingStrategy.recordWidthFor(line);
        if (width.isEmpty()) {
          processor.processLine(line, lineNum,
              (clazz, record) -> segments.add(toParsedSegment(clazz, record)),
              raw -> segments.add(new UnmatchedSegment(raw)));
        } else {
          sliceLine(line, width.getAsInt(), lineNum, segments);
        }
      }
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading line " + (lineCounter[0] + 1), e);
    }
    return segments;
  }

  /**
   * Eagerly reads all lines from {@code inputStream} using UTF-8 encoding.
   *
   * @param inputStream the source stream
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Segment> readAsSegments(InputStream inputStream) {
    return readAsSegments(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all lines from {@code inputStream} using the given charset.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Segment> readAsSegments(InputStream inputStream, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(inputStream, charset)) {
      return readAsSegments(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Eagerly reads all lines from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Segment> readAsSegments(File file) {
    return readAsSegments(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all lines from {@code file} using the given charset.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Segment> readAsSegments(File file, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      return readAsSegments(r);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Eagerly reads all lines from {@code path} using UTF-8 encoding.
   *
   * @param path the path to read
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Segment> readAsSegments(Path path) {
    return readAsSegments(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all lines from {@code path} using the given charset.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return an ordered, mutable list of {@link Segment} entries; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Segment> readAsSegments(Path path, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      return readAsSegments(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  /**
   * Returns a new builder for constructing a {@link FixedFormatSegmentReader}.
   *
   * @return a fresh builder instance
   */
  public static FixedFormatSegmentReaderBuilder builder() {
    return new FixedFormatSegmentReaderBuilder();
  }

  private void sliceLine(String line, int recordWidth, long lineNumber, List<Segment> segments) {
    int offset = 0;
    int chunkIndex = 0;
    while (offset < line.length()) {
      chunkIndex++;
      int remaining = line.length() - offset;
      if (remaining >= recordWidth) {
        String chunk = line.substring(offset, offset + recordWidth);
        offset += recordWidth;
        processor.processLine(chunk, lineNumber,
            (clazz, record) -> segments.add(toParsedSegment(clazz, record)),
            rawChunk -> segments.add(new UnmatchedSegment(rawChunk)));
      } else {
        String partial = line.substring(offset);
        offset = line.length();
        Optional<String> resolved = partialChunkStrategy.resolve(lineNumber, chunkIndex, partial, recordWidth);
        resolved.ifPresent(padded -> processor.processLine(padded, lineNumber,
            (clazz, record) -> segments.add(toParsedSegment(clazz, record)),
            rawChunk -> segments.add(new UnmatchedSegment(rawChunk))));
      }
    }
  }

  private static BufferedReader toBuffered(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }

  // Safe: record was produced by manager.load(clazz, line), so it is an instance of clazz.
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T> ParsedSegment<T> toParsedSegment(Class<?> clazz, Object record) {
    return new ParsedSegment(clazz, record);
  }
}
