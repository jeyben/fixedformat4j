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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.pattern.FixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.row.UnmatchedRow;
import com.ancientprogramming.fixedformat4j.io.strategy.ParseErrorStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fluent builder for {@link FixedFormatRowReader}.
 *
 * <p>Obtain an instance via {@link FixedFormatRowReader#builder()}.
 * At minimum, one mapping must be added via {@link #addMapping} before calling {@link #build()}.</p>
 *
 * <p>Unlike {@link FixedFormatReaderBuilder}, this builder deliberately omits
 * {@code unmatchStrategy} — {@link FixedFormatRowReader} always captures every line,
 * so the concept of an "unmatched line strategy" does not apply.</p>
 */
public class FixedFormatRowReaderBuilder {

  final List<ClassPatternMapping<?>> mappings = new ArrayList<>();
  MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.firstMatch();
  ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.throwException();
  Predicate<String> lineFilter = line -> true;
  FixedFormatManager manager = FixedFormatManagerImpl.create();

  FixedFormatRowReaderBuilder() {}

  /**
   * Registers a mapping that routes lines matching {@code pattern} to {@code clazz}.
   * Mappings are evaluated in registration order.
   *
   * @param clazz   the {@code @Record}-annotated class to instantiate when {@code pattern} matches
   * @param pattern the pattern that decides which lines are parsed as {@code clazz}
   * @param <R>     the record type
   * @return this builder
   * @throws IllegalArgumentException if {@code clazz} is not annotated with {@code @Record}
   */
  public <R> FixedFormatRowReaderBuilder addMapping(Class<R> clazz, FixedFormatMatchPattern pattern) {
    mappings.add(new ClassPatternMapping<>(clazz, pattern));
    return this;
  }

  /**
   * Sets the strategy applied when more than one pattern matches a line.
   * Defaults to {@link MultiMatchStrategy#firstMatch()}.
   *
   * @param strategy the multi-match strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatRowReaderBuilder multiMatchStrategy(MultiMatchStrategy strategy) {
    this.multiMatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when a matched line fails to parse.
   * Defaults to {@link ParseErrorStrategy#throwException()}.
   *
   * @param strategy the parse-error strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatRowReaderBuilder parseErrorStrategy(ParseErrorStrategy strategy) {
    this.parseErrorStrategy = strategy;
    return this;
  }

  /**
   * Registers a pre-match line inclusion predicate. Lines for which the predicate returns
   * {@code false} are treated as unmatched and become {@link UnmatchedRow} entries in the result.
   *
   * @param predicate returns {@code true} for lines that should be pattern-matched
   * @return this builder
   */
  public FixedFormatRowReaderBuilder includeLines(Predicate<String> predicate) {
    this.lineFilter = predicate;
    return this;
  }

  /**
   * Overrides the {@link FixedFormatManager} used to parse each line into a record object.
   *
   * @param manager the manager to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatRowReaderBuilder manager(FixedFormatManager manager) {
    this.manager = manager;
    return this;
  }

  /**
   * Builds and returns a configured {@link FixedFormatRowReader}.
   *
   * @return a new reader instance
   * @throws IllegalArgumentException if no mappings have been added
   */
  public FixedFormatRowReader build() {
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("At least one mapping must be provided");
    }
    return new FixedFormatRowReader(this);
  }
}
