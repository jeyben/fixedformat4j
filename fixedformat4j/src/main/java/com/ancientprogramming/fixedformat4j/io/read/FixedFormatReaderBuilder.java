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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Fluent builder for {@link FixedFormatReader}.
 *
 * <p>Obtain an instance via {@link FixedFormatReader#builder()}.
 * At minimum, one mapping must be added via {@link #addMapping} before calling {@link #build()}.</p>
 */
public class FixedFormatReaderBuilder {

  final List<RecordMapping<?>> mappings = new ArrayList<>();
  MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.firstMatch();
  UnmatchStrategy unmatchStrategy = UnmatchStrategy.throwException();
  ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.throwException();
  Predicate<String> lineFilter = line -> true;
  FixedFormatManager manager = FixedFormatManagerImpl.create();

  FixedFormatReaderBuilder() {}

  /**
   * Registers a mapping that routes lines matching {@code pattern} to {@code clazz}.
   * Mappings are evaluated in registration order.
   *
   * @param clazz   the {@code @Record}-annotated class to instantiate when {@code pattern} matches
   * @param pattern the pattern that decides which lines are parsed as {@code clazz}
   * @return this builder
   * @throws NullPointerException     if {@code clazz} or {@code pattern} is {@code null}
   * @throws IllegalArgumentException if {@code clazz} is not annotated with {@code @Record}
   */
  public <R> FixedFormatReaderBuilder addMapping(Class<R> clazz, Predicate<String> pattern) {
    mappings.add(new RecordMapping<>(clazz, pattern));
    return this;
  }

  /**
   * Sets the strategy applied when more than one pattern matches a line.
   * Defaults to {@link MultiMatchStrategy#firstMatch()}.
   *
   * @param strategy the multi-match strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatReaderBuilder multiMatchStrategy(MultiMatchStrategy strategy) {
    Objects.requireNonNull(strategy, "strategy must not be null");
    this.multiMatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when no pattern matches a line.
   * Defaults to {@link UnmatchStrategy#throwException()}.
   *
   * @param strategy the unmatched strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatReaderBuilder unmatchStrategy(UnmatchStrategy strategy) {
    Objects.requireNonNull(strategy, "strategy must not be null");
    this.unmatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when a matched line fails to parse.
   * Defaults to {@link ParseErrorStrategy#throwException()}.
   *
   * @param strategy the parse-error strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatReaderBuilder parseErrorStrategy(ParseErrorStrategy strategy) {
    Objects.requireNonNull(strategy, "strategy must not be null");
    this.parseErrorStrategy = strategy;
    return this;
  }

  /**
   * Registers a pre-match line inclusion predicate. Lines for which the predicate returns
   * {@code false} are skipped entirely before pattern matching, bypassing the
   * {@link UnmatchStrategy}.
   *
   * @param predicate returns {@code true} for lines that should be processed
   * @return this builder
   */
  public FixedFormatReaderBuilder includeLines(Predicate<String> predicate) {
    Objects.requireNonNull(predicate, "predicate must not be null");
    this.lineFilter = predicate;
    return this;
  }

  /**
   * Overrides the {@link FixedFormatManager} used to parse each line into a record object.
   *
   * @param manager the manager to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatReaderBuilder manager(FixedFormatManager manager) {
    Objects.requireNonNull(manager, "manager must not be null");
    this.manager = manager;
    return this;
  }

  /**
   * Builds and returns a configured {@link FixedFormatReader}.
   *
   * @return a new reader instance
   * @throws IllegalArgumentException if no mappings have been added
   */
  public FixedFormatReader build() {
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("At least one mapping must be provided");
    }
    return new FixedFormatReader(this);
  }
}
