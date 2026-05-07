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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Fluent builder for {@link FixedFormatWriter}.
 *
 * <p>Obtain an instance via {@link FixedFormatWriter#builder()}.
 * All options are optional; sensible defaults are provided for each.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public final class FixedFormatWriterBuilder {

  FixedFormatManager manager = FixedFormatManagerImpl.create();
  String lineSeparator = System.lineSeparator();
  Charset charset = StandardCharsets.UTF_8;

  FixedFormatWriterBuilder() {}

  /**
   * Overrides the {@link FixedFormatManager} used to export each record to a string.
   *
   * @param manager the manager to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatWriterBuilder manager(FixedFormatManager manager) {
    Objects.requireNonNull(manager, "manager must not be null");
    this.manager = manager;
    return this;
  }

  /**
   * Overrides the line separator written after each exported record.
   * Defaults to {@link System#lineSeparator()}.
   *
   * @param lineSeparator the line separator string; must not be {@code null}
   * @return this builder
   */
  public FixedFormatWriterBuilder lineSeparator(String lineSeparator) {
    Objects.requireNonNull(lineSeparator, "lineSeparator must not be null");
    this.lineSeparator = lineSeparator;
    return this;
  }

  /**
   * Sets the default charset used when writing to {@link java.io.OutputStream} or
   * {@link java.nio.file.Path} targets without an explicit charset argument.
   * Defaults to {@link StandardCharsets#UTF_8}.
   *
   * @param charset the default charset; must not be {@code null}
   * @return this builder
   */
  public FixedFormatWriterBuilder charset(Charset charset) {
    Objects.requireNonNull(charset, "charset must not be null");
    this.charset = charset;
    return this;
  }

  /**
   * Builds and returns a configured {@link FixedFormatWriter}.
   *
   * @return a new writer instance
   */
  public FixedFormatWriter build() {
    return new FixedFormatWriter(this);
  }
}
