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
package com.ancientprogramming.fixedformat4j.io.segment;

import java.util.Objects;

/**
 * A {@link Segment} whose text did not match any registered
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record} pattern and is therefore
 * held verbatim as the raw string (without any trailing newline).
 *
 * <p>When written back via
 * {@link com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter},
 * the raw text is emitted unchanged, preserving comment lines, blank lines, header separators,
 * and any other non-record content in the original source.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public final class UnmatchedSegment implements Segment {

  private final String rawLine;

  /**
   * Creates a new {@code UnmatchedSegment} holding {@code rawLine} verbatim.
   *
   * @param rawLine the text as read, without a trailing newline; must not be {@code null}
   */
  public UnmatchedSegment(String rawLine) {
    this.rawLine = Objects.requireNonNull(rawLine, "rawLine must not be null");
  }

  /**
   * Returns the raw text exactly as it was read from the source, without any trailing newline.
   *
   * @return the raw text; never {@code null}
   */
  public String getRawLine() {
    return rawLine;
  }
}
