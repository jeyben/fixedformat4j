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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Writes {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated objects to a
 * fixed-format file or stream, providing write-side IO symmetry with {@link
 * com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader}.
 *
 * <p>The writer owns resource management (opening, flushing, closing) and line delimiting.
 * Formatting of each record is delegated entirely to
 * {@link FixedFormatManager#export(Object)}.</p>
 *
 * <p>Instances are thread-safe: all fields are final and each {@code write} call is independent.</p>
 *
 * <p>Quick start — homogeneous list:</p>
 * <pre>{@code
 * FixedFormatWriter writer = FixedFormatWriter.builder().build();
 * writer.write(Path.of("out.txt"), records);
 * }</pre>
 *
 * <p>Quick start — lazy stream (avoids materialising all records in memory):</p>
 * <pre>{@code
 * try (Stream<MyRecord> stream = repository.findAll()) {
 *     writer.write(Path.of("out.txt"), stream);
 * }
 * }</pre>
 *
 * <p>Quick start — heterogeneous mixed-type list:</p>
 * <pre>{@code
 * writer.write(Path.of("out.txt"), List.of(header, detail1, detail2, footer));
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public final class FixedFormatWriter {

  private final FixedFormatManager manager;
  private final String lineSeparator;
  private final Charset defaultCharset;

  FixedFormatWriter(FixedFormatWriterBuilder builder) {
    this.manager = builder.manager;
    this.lineSeparator = builder.lineSeparator;
    this.defaultCharset = builder.charset;
  }

  // --- Iterable overloads ---

  /**
   * Writes all records from {@code records} to {@code writer}, appending
   * {@link FixedFormatWriterBuilder#lineSeparator(String)} after each one.
   *
   * <p>The writer is closed when this method returns, even if an exception is thrown.</p>
   *
   * @param writer  the write target; closed when this method returns
   * @param records the records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}
   * @throws NullPointerException   if {@code writer} or {@code records} is {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing or closing
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(Writer writer, Iterable<?> records) {
    Objects.requireNonNull(writer, "writer must not be null");
    requireNonNullOrClose(records, "records must not be null", writer);
    writeAndClose(toBuffered(writer), records.iterator());
  }

  /**
   * Writes all records from {@code records} to {@code out} using the default charset.
   *
   * @param out     the output stream; closed when this method returns
   * @param records the records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}
   * @throws NullPointerException   if {@code out} or {@code records} is {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing or closing
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(OutputStream out, Iterable<?> records) {
    write(out, defaultCharset, records);
  }

  /**
   * Writes all records from {@code records} to {@code out} using {@code charset}.
   *
   * @param out     the output stream; closed when this method returns
   * @param charset the character encoding to apply
   * @param records the records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}
   * @throws NullPointerException   if {@code out}, {@code charset}, or {@code records} is
   *                                {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing or closing
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(OutputStream out, Charset charset, Iterable<?> records) {
    Objects.requireNonNull(out, "out must not be null");
    requireNonNullOrClose(charset, "charset must not be null", out);
    requireNonNullOrClose(records, "records must not be null", out);
    write(openWriter(out, charset), records);
  }

  /**
   * Writes all records from {@code records} to {@code path} using the default charset,
   * truncating any existing file.
   *
   * @param path    the path to write to; created or truncated
   * @param records the records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}
   * @throws NullPointerException   if {@code path} or {@code records} is {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(Path path, Iterable<?> records) {
    write(path, defaultCharset, records);
  }

  /**
   * Writes all records from {@code records} to {@code path} using {@code charset},
   * truncating any existing file.
   *
   * @param path    the path to write to; created or truncated
   * @param charset the character encoding to apply
   * @param records the records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}
   * @throws NullPointerException   if {@code path}, {@code charset}, or {@code records} is
   *                                {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(Path path, Charset charset, Iterable<?> records) {
    Objects.requireNonNull(records, "records must not be null");
    write(openWriter(path, charset), records);
  }

  // --- Stream overloads ---

  /**
   * Writes all records from {@code records} to {@code writer}.
   *
   * <p>The stream is consumed but <em>not closed</em> — the caller retains ownership of
   * the stream lifecycle. The writer is closed when this method returns.</p>
   *
   * @param writer  the write target; closed when this method returns
   * @param records a stream of records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}; consumed but
   *                not closed
   * @throws NullPointerException   if {@code writer} or {@code records} is {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing or closing
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(Writer writer, Stream<?> records) {
    Objects.requireNonNull(writer, "writer must not be null");
    requireNonNullOrClose(records, "records must not be null", writer);
    writeAndClose(toBuffered(writer), records.iterator());
  }

  /**
   * Writes all records from {@code records} to {@code out} using the default charset.
   *
   * <p>The stream is consumed but not closed. The output stream is closed when this method
   * returns.</p>
   *
   * @param out     the output stream; closed when this method returns
   * @param records a stream of records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}; consumed but
   *                not closed
   * @throws NullPointerException   if {@code out} or {@code records} is {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing or closing
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(OutputStream out, Stream<?> records) {
    write(out, defaultCharset, records);
  }

  /**
   * Writes all records from {@code records} to {@code out} using {@code charset}.
   *
   * <p>The stream is consumed but not closed. The output stream is closed when this method
   * returns.</p>
   *
   * @param out     the output stream; closed when this method returns
   * @param charset the character encoding to apply
   * @param records a stream of records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}; consumed but
   *                not closed
   * @throws NullPointerException   if {@code out}, {@code charset}, or {@code records} is
   *                                {@code null}
   * @throws FixedFormatIOException if an IO error occurs while writing or closing
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(OutputStream out, Charset charset, Stream<?> records) {
    Objects.requireNonNull(out, "out must not be null");
    requireNonNullOrClose(charset, "charset must not be null", out);
    requireNonNullOrClose(records, "records must not be null", out);
    write(openWriter(out, charset), records);
  }

  /**
   * Writes all records from {@code records} to {@code path} using the default charset,
   * truncating any existing file.
   *
   * <p>The stream is consumed but not closed.</p>
   *
   * @param path    the path to write to; created or truncated
   * @param records a stream of records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}; consumed but
   *                not closed
   * @throws NullPointerException   if {@code path} or {@code records} is {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(Path path, Stream<?> records) {
    write(path, defaultCharset, records);
  }

  /**
   * Writes all records from {@code records} to {@code path} using {@code charset},
   * truncating any existing file.
   *
   * <p>The stream is consumed but not closed.</p>
   *
   * @param path    the path to write to; created or truncated
   * @param charset the character encoding to apply
   * @param records a stream of records to write; each element must be annotated with
   *                {@link com.ancientprogramming.fixedformat4j.annotation.Record}; consumed but
   *                not closed
   * @throws NullPointerException   if {@code path}, {@code charset}, or {@code records} is
   *                                {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   * @throws com.ancientprogramming.fixedformat4j.exception.FixedFormatException if any element
   *         in {@code records} is not annotated with {@code @Record}
   */
  public void write(Path path, Charset charset, Stream<?> records) {
    Objects.requireNonNull(records, "records must not be null");
    write(openWriter(path, charset), records);
  }

  // --- Factory ---

  /**
   * Returns a new builder for constructing a {@link FixedFormatWriter}.
   *
   * @return a fresh builder instance
   */
  public static FixedFormatWriterBuilder builder() {
    return new FixedFormatWriterBuilder();
  }

  // --- Internal ---

  private void writeAndClose(BufferedWriter bw, Iterator<?> records) {
    try (bw) {
      while (records.hasNext()) {
        bw.write(manager.export(records.next()));
        bw.write(lineSeparator);
      }
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error writing or closing fixed-format output", e);
    }
  }

  private static BufferedWriter toBuffered(Writer writer) {
    return writer instanceof BufferedWriter ? (BufferedWriter) writer : new BufferedWriter(writer);
  }

  private static OutputStreamWriter openWriter(OutputStream out, Charset charset) {
    Objects.requireNonNull(out,     "out must not be null");
    Objects.requireNonNull(charset, "charset must not be null");
    return new OutputStreamWriter(out, charset);
  }

  private static BufferedWriter openWriter(Path path, Charset charset) {
    Objects.requireNonNull(path,    "path must not be null");
    Objects.requireNonNull(charset, "charset must not be null");
    try {
      return Files.newBufferedWriter(path, charset);
    } catch (IOException e) {
      throw new FixedFormatIOException(format("Cannot open path: %s", path), e);
    }
  }

  /**
   * Returns {@code value} if non-null; otherwise closes {@code resource} (an already-open,
   * caller-supplied stream this method has taken ownership of) and throws
   * {@link NullPointerException}. Used so that validating a later parameter never leaks an
   * earlier, already-open resource.
   */
  private static <T> T requireNonNullOrClose(T value, String message, Closeable resource) {
    if (value == null) {
      closeQuietly(resource);
      throw new NullPointerException(message);
    }
    return value;
  }

  private static void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException suppressed) {
      // best-effort close after a validation failure; the validation exception is what matters
    }
  }
}
