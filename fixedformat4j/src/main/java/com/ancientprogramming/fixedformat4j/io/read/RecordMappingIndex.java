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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bucketed lookup of {@link RecordMapping}s keyed by their {@link LinePattern}. Built once at
 * reader-build time and queried per line by {@link FixedFormatLineProcessor}.
 *
 * <p>Patterns sharing the same positions array share a hash bucket; within a bucket, mappings
 * are keyed by the literal extracted from those positions. Match-all patterns are kept in a
 * separate list and appended to every result.</p>
 *
 * <p>{@link #findMatches(String)} returns matched mappings ordered by depth descending then by
 * registration order — so the most detailed match is first and ties fall back to insertion order.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
final class RecordMappingIndex {

  private static final Comparator<IndexedMapping> BY_DEPTH_DESC_THEN_SEQUENCE =
      Comparator.<IndexedMapping>comparingInt(im -> im.depth).reversed()
          .thenComparingInt(im -> im.sequence);

  private final List<IndexedMapping> matchAll;
  private final List<Bucket> buckets;

  private RecordMappingIndex(List<IndexedMapping> matchAll, List<Bucket> buckets) {
    this.matchAll = matchAll;
    this.buckets = buckets;
  }

  List<RecordMapping<?>> findMatches(String line) {
    List<IndexedMapping> hits = new ArrayList<>();
    for (Bucket bucket : buckets) {
      if (line.length() <= bucket.maxPosition) {
        continue;
      }
      List<IndexedMapping> atKey = bucket.byLiteral.get(bucket.extractKey(line));
      if (atKey != null) {
        hits.addAll(atKey);
      }
    }
    hits.addAll(matchAll);
    hits.sort(BY_DEPTH_DESC_THEN_SEQUENCE);
    List<RecordMapping<?>> result = new ArrayList<>(hits.size());
    for (IndexedMapping im : hits) {
      result.add(im.mapping);
    }
    return result;
  }

  static Builder builder() {
    return new Builder();
  }

  static final class Builder {
    private final List<IndexedMapping> matchAll = new ArrayList<>();
    private final Map<PositionsKey, Bucket> byPositions = new HashMap<>();
    private int sequence = 0;

    Builder add(LinePattern pattern, RecordMapping<?> mapping) {
      Objects.requireNonNull(pattern, "pattern must not be null");
      Objects.requireNonNull(mapping, "mapping must not be null");
      IndexedMapping im = new IndexedMapping(sequence++, pattern.depth(), mapping);
      if (pattern.depth() == 0) {
        matchAll.add(im);
        return this;
      }
      int[] positions = pattern.positions();
      Bucket bucket = byPositions.computeIfAbsent(new PositionsKey(positions),
          k -> new Bucket(positions));
      bucket.byLiteral.computeIfAbsent(pattern.literal(), k -> new ArrayList<>()).add(im);
      return this;
    }

    RecordMappingIndex build() {
      return new RecordMappingIndex(List.copyOf(matchAll), List.copyOf(byPositions.values()));
    }
  }

  private static final class IndexedMapping {
    final int sequence;
    final int depth;
    final RecordMapping<?> mapping;

    IndexedMapping(int sequence, int depth, RecordMapping<?> mapping) {
      this.sequence = sequence;
      this.depth = depth;
      this.mapping = mapping;
    }
  }

  private static final class Bucket {
    final int[] positions;
    final int maxPosition;
    final Map<String, List<IndexedMapping>> byLiteral = new HashMap<>();

    Bucket(int[] positions) {
      this.positions = positions;
      this.maxPosition = positions[positions.length - 1];
    }

    String extractKey(String line) {
      char[] chars = new char[positions.length];
      for (int i = 0; i < positions.length; i++) {
        chars[i] = line.charAt(positions[i]);
      }
      return new String(chars);
    }
  }

  private static final class PositionsKey {
    private final int[] positions;
    private final int hash;

    PositionsKey(int[] positions) {
      this.positions = positions;
      this.hash = Arrays.hashCode(positions);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof PositionsKey
          && Arrays.equals(positions, ((PositionsKey) o).positions);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }
}
