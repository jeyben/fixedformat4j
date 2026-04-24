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
package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.io.row.ParsedRow;
import com.ancientprogramming.fixedformat4j.io.row.Row;
import com.ancientprogramming.fixedformat4j.io.row.UnmatchedRow;
import com.ancientprogramming.fixedformat4j.io.strategy.PartialChunkStrategy;
import com.ancientprogramming.fixedformat4j.io.strategy.UnmatchStrategy;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Reads fixed-format files where multiple records are packed end-to-end within a single physical
 * line, and returns every chunk as an ordered {@link List} of {@link Row} entries for
 * read-edit-write round trips.
 *
 * <p>Matched chunks become {@link ParsedRow} entries; unmatched chunks become
 * {@link UnmatchedRow} entries (holding the raw chunk text). Physical lines rejected by the
 * {@link PackedRecordRowReaderBuilder#includeLines(Predicate) includeLines} predicate become a
 * single {@link UnmatchedRow} holding the entire raw line. Partial trailing chunks are handled
 * by the configured {@link PartialChunkStrategy}; chunks resolved to
 * {@link Optional#empty()} are silently dropped.</p>
 *
 * <p>Quick start:</p>
 * <pre>{@code
 * PackedRecordRowReader reader = PackedRecordRowReader.builder()
 *     .recordWidth(128)
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .build();
 *
 * List<Row> rows = reader.readAsRows(new File("data.txt"));
 *
 * rows.stream()
 *     .filter(r -> r instanceof ParsedRow && ((ParsedRow<?>) r).isOf(DetailRecord.class))
 *     .map(r -> (ParsedRow<DetailRecord>) r)
 *     .forEach(pr -> pr.getRecord().setAmount(pr.getRecord().getAmount() * 2));
 *
 * // FixedFormatWriter writes one row per line, effectively unpacking the packed structure
 * new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, new File("data-updated.txt"));
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 * @see PackedRecordRowReaderBuilder
 * @see PackedRecordReader
 * @see FixedFormatWriter
 */
public class PackedRecordRowReader {

  private final int recordWidth;
  private final FixedFormatLineProcessor processor;
  private final PartialChunkStrategy partialChunkStrategy;
  private final Predicate<String> lineFilter;

  PackedRecordRowReader(PackedRecordRowReaderBuilder builder) {
    this.recordWidth = builder.recordWidth;
    this.partialChunkStrategy = builder.partialChunkStrategy;
    this.lineFilter = builder.lineFilter;
    this.processor = new FixedFormatLineProcessor(
        List.copyOf(builder.mappings),
        builder.multiMatchStrategy,
        UnmatchStrategy.skip(),  // never invoked — row reader uses the 4-arg processLine
        builder.parseErrorStrategy,
        line -> true,  // line filter applied in sliceLine; processor sees full chunks
        builder.manager);
  }

  /**
   * Eagerly reads all lines from {@code reader} and returns an ordered list of {@link Row}
   * entries preserving the exact chunk order of the source.
   *
   * @param reader the source of lines; wrapped in a {@link BufferedReader} if not already one
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Row> readAsRows(Reader reader) {
    List<Row> rows = new ArrayList<>();
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};
    try (buffered) {
      String line;
      while ((line = buffered.readLine()) != null) {
        sliceLine(line, ++lineCounter[0], rows);
      }
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading line " + (lineCounter[0] + 1), e);
    }
    return rows;
  }

  /**
   * Eagerly reads all lines from {@code inputStream} using UTF-8 encoding.
   *
   * @param inputStream the source stream
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Row> readAsRows(InputStream inputStream) {
    return readAsRows(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all lines from {@code inputStream} using the given charset.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Row> readAsRows(InputStream inputStream, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(inputStream, charset)) {
      return readAsRows(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Eagerly reads all lines from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Row> readAsRows(File file) {
    return readAsRows(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all lines from {@code file} using the given charset.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Row> readAsRows(File file, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      return readAsRows(r);
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
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Row> readAsRows(Path path) {
    return readAsRows(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all lines from {@code path} using the given charset.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return an ordered, mutable list of {@link Row} entries; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Row> readAsRows(Path path, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      return readAsRows(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  /**
   * Returns a new builder for constructing a {@link PackedRecordRowReader}.
   *
   * @return a fresh builder instance
   */
  public static PackedRecordRowReaderBuilder builder() {
    return new PackedRecordRowReaderBuilder();
  }

  private void sliceLine(String line, long lineNumber, List<Row> rows) {
    if (!lineFilter.test(line)) {
      rows.add(new UnmatchedRow(line));
      return;
    }
    int offset = 0;
    int chunkIndex = 0;
    while (offset < line.length()) {
      chunkIndex++;
      int remaining = line.length() - offset;
      if (remaining >= recordWidth) {
        String chunk = line.substring(offset, offset + recordWidth);
        offset += recordWidth;
        processor.processLine(chunk, lineNumber,
            (clazz, record) -> rows.add(toParsedRow(clazz, record)),
            rawChunk -> rows.add(new UnmatchedRow(rawChunk)));
      } else {
        String partial = line.substring(offset);
        offset = line.length();
        Optional<String> resolved = partialChunkStrategy.resolve(
            lineNumber, chunkIndex, partial, recordWidth);
        resolved.ifPresent(padded -> processor.processLine(padded, lineNumber,
            (clazz, record) -> rows.add(toParsedRow(clazz, record)),
            rawChunk -> rows.add(new UnmatchedRow(rawChunk))));
      }
    }
  }

  private static BufferedReader toBuffered(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }

  // Safe: record was produced by manager.load(clazz, chunk), so it is an instance of clazz.
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T> ParsedRow<T> toParsedRow(Class<?> clazz, Object record) {
    return new ParsedRow(clazz, record);
  }
}
