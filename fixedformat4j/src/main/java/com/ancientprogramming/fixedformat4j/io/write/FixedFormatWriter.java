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
package com.ancientprogramming.fixedformat4j.io.write;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.io.segment.ParsedSegment;
import com.ancientprogramming.fixedformat4j.io.segment.Segment;
import com.ancientprogramming.fixedformat4j.io.segment.UnmatchedSegment;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Writes an ordered list of {@link Segment} entries back to a file or stream, enabling
 * read-edit-write round trips on fixed-format files.
 *
 * <p>{@link ParsedSegment} entries are exported to a fixed-width string via
 * {@link FixedFormatManager#export(Object)}; {@link UnmatchedSegment} entries are emitted
 * verbatim. Each segment is followed by a {@code \n} line terminator.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * FixedFormatSegmentReader reader = FixedFormatSegmentReader.builder()
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .build();
 *
 * List<Segment> segments = reader.readAsSegments(new File("input.txt"));
 *
 * segments.stream()
 *     .filter(s -> s instanceof ParsedSegment && ((ParsedSegment<?>) s).isOf(DetailRecord.class))
 *     .map(s -> (ParsedSegment<DetailRecord>) s)
 *     .forEach(ps -> ps.getRecord().setAmount(ps.getRecord().getAmount() * 2));
 *
 * new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, new File("output.txt"));
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 * @see com.ancientprogramming.fixedformat4j.io.read.FixedFormatSegmentReader#readAsSegments(java.io.File)
 */
public class FixedFormatWriter {

  private final FixedFormatManager manager;

  /**
   * Creates a new writer that uses {@code manager} to export {@link ParsedSegment} records.
   *
   * @param manager the manager used to serialize records; must not be {@code null}
   */
  public FixedFormatWriter(FixedFormatManager manager) {
    this.manager = Objects.requireNonNull(manager, "manager must not be null");
  }

  /**
   * Writes all segments in {@code segments} to {@code writer}, one per line.
   *
   * <p>{@link ParsedSegment} entries are serialized via {@link FixedFormatManager#export(Object)};
   * {@link UnmatchedSegment} entries are written verbatim. Each segment is followed by
   * {@code \n}.</p>
   *
   * @param segments the segments to write; must not be {@code null}
   * @param writer   the destination; must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing
   */
  public void write(List<Segment> segments, Writer writer) {
    Objects.requireNonNull(segments, "segments must not be null");
    Objects.requireNonNull(writer, "writer must not be null");
    BufferedWriter buffered = writer instanceof BufferedWriter
        ? (BufferedWriter) writer : new BufferedWriter(writer);
    try {
      for (Segment segment : segments) {
        buffered.write(toLine(segment));
        buffered.write('\n');
      }
      buffered.flush();
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error writing records", e);
    }
  }

  /**
   * Writes all segments to {@code outputStream} using UTF-8 encoding.
   *
   * @param segments     the segments to write; must not be {@code null}
   * @param outputStream the destination; must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing
   */
  public void write(List<Segment> segments, OutputStream outputStream) {
    write(segments, outputStream, StandardCharsets.UTF_8);
  }

  /**
   * Writes all segments to {@code outputStream} using the given charset.
   *
   * @param segments     the segments to write; must not be {@code null}
   * @param outputStream the destination; must not be {@code null}
   * @param charset      the character encoding to apply
   * @throws FixedFormatIOException if an IO error occurs while writing
   */
  public void write(List<Segment> segments, OutputStream outputStream, Charset charset) {
    write(segments, new OutputStreamWriter(outputStream, charset));
  }

  /**
   * Writes all segments to {@code file} using UTF-8 encoding, creating or overwriting it.
   *
   * @param segments the segments to write; must not be {@code null}
   * @param file     the destination file; must not be {@code null}
   * @throws FixedFormatIOException if the file cannot be opened or an IO error occurs
   */
  public void write(List<Segment> segments, File file) {
    write(segments, file, StandardCharsets.UTF_8);
  }

  /**
   * Writes all segments to {@code file} using the given charset, creating or overwriting it.
   *
   * @param segments the segments to write; must not be {@code null}
   * @param file     the destination file; must not be {@code null}
   * @param charset  the character encoding to apply
   * @throws FixedFormatIOException if the file cannot be opened or an IO error occurs
   */
  public void write(List<Segment> segments, File file, Charset charset) {
    try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), charset)) {
      write(segments, w);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error writing file: " + file, e);
    }
  }

  /**
   * Writes all segments to {@code path} using UTF-8 encoding, creating or overwriting it.
   *
   * @param segments the segments to write; must not be {@code null}
   * @param path     the destination path; must not be {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void write(List<Segment> segments, Path path) {
    write(segments, path, StandardCharsets.UTF_8);
  }

  /**
   * Writes all segments to {@code path} using the given charset, creating or overwriting it.
   *
   * @param segments the segments to write; must not be {@code null}
   * @param path     the destination path; must not be {@code null}
   * @param charset  the character encoding to apply
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void write(List<Segment> segments, Path path, Charset charset) {
    try (OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(path), charset)) {
      write(segments, w);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error writing path: " + path, e);
    }
  }

  private String toLine(Segment segment) {
    if (segment instanceof UnmatchedSegment) {
      return ((UnmatchedSegment) segment).getRawLine();
    }
    return manager.export(((ParsedSegment<?>) segment).getRecord());
  }
}
