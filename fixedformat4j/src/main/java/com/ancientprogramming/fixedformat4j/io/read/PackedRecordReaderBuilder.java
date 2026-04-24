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
import java.util.function.Predicate;

/**
 * Fluent builder for {@link PackedRecordReader}.
 *
 * <p>Obtain an instance via {@link PackedRecordReader#builder()}.
 * At minimum, {@link #recordWidth(int)} and at least one {@link #addMapping} call are required
 * before calling {@link #build()}.</p>
 */
public class PackedRecordReaderBuilder {

  int recordWidth = 0;
  final List<ClassPatternMapping<?>> mappings = new ArrayList<>();
  MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.firstMatch();
  UnmatchStrategy unmatchStrategy = UnmatchStrategy.skip();
  ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.throwException();
  PartialChunkStrategy partialChunkStrategy = PartialChunkStrategy.skip();
  Predicate<String> lineFilter = line -> true;
  FixedFormatManager manager = FixedFormatManagerImpl.create();

  PackedRecordReaderBuilder() {}

  /**
   * Sets the fixed width (in characters) of each packed record.
   * Must be greater than zero.
   *
   * @param recordWidth the width of each record chunk in characters
   * @return this builder
   */
  public PackedRecordReaderBuilder recordWidth(int recordWidth) {
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
  public <R> PackedRecordReaderBuilder addMapping(Class<R> clazz, FixedFormatMatchPattern pattern) {
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
  public PackedRecordReaderBuilder multiMatchStrategy(MultiMatchStrategy strategy) {
    this.multiMatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when no pattern matches a chunk.
   * Defaults to {@link UnmatchStrategy#skip()}.
   *
   * @param strategy the unmatched strategy to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordReaderBuilder unmatchStrategy(UnmatchStrategy strategy) {
    this.unmatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when a matched chunk fails to parse.
   * Defaults to {@link ParseErrorStrategy#throwException()}.
   *
   * @param strategy the parse-error strategy to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordReaderBuilder parseErrorStrategy(ParseErrorStrategy strategy) {
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
  public PackedRecordReaderBuilder partialChunkStrategy(PartialChunkStrategy strategy) {
    this.partialChunkStrategy = strategy;
    return this;
  }

  /**
   * Registers a pre-match line inclusion predicate. Physical lines for which the predicate returns
   * {@code false} are skipped entirely — all their chunks are dropped.
   *
   * @param predicate returns {@code true} for lines whose chunks should be processed
   * @return this builder
   */
  public PackedRecordReaderBuilder includeLines(Predicate<String> predicate) {
    this.lineFilter = predicate;
    return this;
  }

  /**
   * Overrides the {@link FixedFormatManager} used to parse each chunk into a record object.
   *
   * @param manager the manager to use; must not be {@code null}
   * @return this builder
   */
  public PackedRecordReaderBuilder manager(FixedFormatManager manager) {
    this.manager = manager;
    return this;
  }

  /**
   * Builds and returns a configured {@link PackedRecordReader}.
   *
   * @return a new reader instance
   * @throws IllegalArgumentException if no mappings have been added or {@code recordWidth} is not
   *                                  greater than zero
   */
  public PackedRecordReader build() {
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("At least one mapping must be provided");
    }
    if (recordWidth <= 0) {
      throw new IllegalArgumentException("recordWidth must be greater than zero");
    }
    return new PackedRecordReader(this);
  }
}
