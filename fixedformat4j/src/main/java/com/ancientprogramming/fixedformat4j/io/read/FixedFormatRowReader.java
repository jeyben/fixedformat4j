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
import com.ancientprogramming.fixedformat4j.io.row.ParsedRow;
import com.ancientprogramming.fixedformat4j.io.row.Row;
import com.ancientprogramming.fixedformat4j.io.row.UnmatchedRow;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a fixed-format file or stream line by line and returns every line as an ordered
 * {@link List} of {@link Row} entries, preserving the exact line order of the source.
 *
 * <p>Lines that match a registered pattern are parsed into a {@link ParsedRow}; lines that
 * do not match any pattern become an {@link UnmatchedRow}. No line is ever dropped — there is
 * no concept of an "unmatched line strategy" in this reader.</p>
 *
 * <p>This reader is designed for <em>read-edit-write</em> round trips: read the file with
 * {@link #readAsRows}, mutate the records you care about, then write everything back with
 * {@link com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter}, which preserves unmatched lines (comments, blanks, separators)
 * verbatim.</p>
 *
 * <p>Quick start:</p>
 * <pre>{@code
 * FixedFormatRowReader reader = FixedFormatRowReader.builder()
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
 * new com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter(new com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl()).write(rows, new File("data-updated.txt"));
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 * @see com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter
 * @see FixedFormatReader
 */
public class FixedFormatRowReader {

  private final FixedFormatLineProcessor processor;

  FixedFormatRowReader(FixedFormatRowReaderBuilder builder) {
    this.processor = new FixedFormatLineProcessor(
        List.copyOf(builder.mappings),
        builder.multiMatchStrategy,
        UnmatchStrategy.skip(),  // never invoked — row reader uses the 4-arg processLine
        builder.parseErrorStrategy,
        builder.lineFilter,
        builder.manager);
  }

  /**
   * Eagerly reads all lines from {@code reader} and returns an ordered list of {@link Row}
   * entries preserving the exact line order of the source.
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
        processor.processLine(line, ++lineCounter[0],
            (clazz, record) -> rows.add(toParsedRow(clazz, record)),
            raw -> rows.add(new UnmatchedRow(raw)));
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
   * Returns a new builder for constructing a {@link FixedFormatRowReader}.
   *
   * @return a fresh builder instance
   */
  public static FixedFormatRowReaderBuilder builder() {
    return new FixedFormatRowReaderBuilder();
  }

  private static BufferedReader toBuffered(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }

  // Safe: record was produced by manager.load(clazz, line), so it is an instance of clazz.
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T> ParsedRow<T> toParsedRow(Class<?> clazz, Object record) {
    return new ParsedRow(clazz, record);
  }
}
