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
import com.ancientprogramming.fixedformat4j.io.strategy.PartialChunkStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fluent builder for {@link PackedRecordRowReader}.
 *
 * <p>Obtain an instance via {@link PackedRecordRowReader#builder()}.
 * At minimum, {@link #recordWidth(int)} and at least one {@link #addMapping} call are required
 * before calling {@link #build()}.</p>
 *
 * <p>Unlike {@link PackedRecordReaderBuilder}, this builder deliberately omits
 * {@code unmatchStrategy} — {@link PackedRecordRowReader} always captures every chunk,
 * so the concept of an "unmatched chunk strategy" does not apply.</p>
 */
public class PackedRecordRowReaderBuilder {

  int recordWidth = 0;
  final List<ClassPatternMapping<?>> mappings = new ArrayList<>();
  MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.firstMatch();
  ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.throwException();
  PartialChunkStrategy partialChunkStrategy = PartialChunkStrategy.skip();
  Predicate<String> lineFilter = line -> true;
  FixedFormatManager manager = FixedFormatManagerImpl.create();

  PackedRecordRowReaderBuilder() {}

  /**
   * Sets the fixed width (in characters) of each packed record.
   * Must be greater than zero.
   *
   * @param recordWidth the width of each record chunk in characters
   * @return this builder
   */
  public PackedRecordRowReaderBuilder recordWidth(int recordWidth) {
    this.recordWidth = recordWidth;
    return this;
  }

  /**
   * Registers a mapping that routes chunks matching {@code pattern} to {@code clazz}.
   * Mappings are evaluated in registration order.
   *
   * @param clazz   the {@code @Record}-annotated class to instantiate when {@code pattern} matches
   * @param pattern the pattern that decides which chunks are parsed as {@code clazz}
   * @param <R>     the record type
   * @return this builder
   */
  public <R> PackedRecordRowReaderBuilder addMapping(Class<R> clazz, FixedFormatMatchPattern pattern) {
    mappings.add(new ClassPatternMapping<>(clazz, pattern));
    return this;
  }

  /**
   * Sets the strategy applied when more than one pattern matches a chunk.
   * Defaults to {@link MultiMatchStrategy#firstMatch()}.
   *
   * @param strategy the multi-match strategy to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordRowReaderBuilder multiMatchStrategy(MultiMatchStrategy strategy) {
    this.multiMatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when a matched chunk fails to parse.
   * Defaults to {@link ParseErrorStrategy#throwException()}.
   *
   * @param strategy the parse-error strategy to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordRowReaderBuilder parseErrorStrategy(ParseErrorStrategy strategy) {
    this.parseErrorStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when the last chunk on a line is shorter than {@link #recordWidth}.
   * Defaults to {@link PartialChunkStrategy#skip()}.
   *
   * @param strategy the partial-chunk strategy to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordRowReaderBuilder partialChunkStrategy(PartialChunkStrategy strategy) {
    this.partialChunkStrategy = strategy;
    return this;
  }

  /**
   * Registers a pre-match line inclusion predicate. Physical lines for which the predicate returns
   * {@code false} are treated as unmatched and become a single {@link UnmatchedRow} entry in
   * the result.
   *
   * @param predicate returns {@code true} for lines whose chunks should be processed
   * @return this builder
   */
  public PackedRecordRowReaderBuilder includeLines(Predicate<String> predicate) {
    this.lineFilter = predicate;
    return this;
  }

  /**
   * Overrides the {@link FixedFormatManager} used to parse each chunk into a record object.
   *
   * @param manager the manager to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordRowReaderBuilder manager(FixedFormatManager manager) {
    this.manager = manager;
    return this;
  }

  /**
   * Builds and returns a configured {@link PackedRecordRowReader}.
   *
   * @return a new reader instance
   * @throws IllegalArgumentException if no mappings have been added or {@code recordWidth} is not
   *                                  greater than zero
   */
  public PackedRecordRowReader build() {
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("At least one mapping must be provided");
    }
    if (recordWidth <= 0) {
      throw new IllegalArgumentException("recordWidth must be greater than zero");
    }
    return new PackedRecordRowReader(this);
  }
}
