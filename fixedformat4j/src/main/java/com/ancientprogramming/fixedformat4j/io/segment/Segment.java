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

/**
 * A single segment from a fixed-format source, as returned by
 * {@link com.ancientprogramming.fixedformat4j.io.read.FixedFormatSegmentReader#readAsSegments} and
 * {@link com.ancientprogramming.fixedformat4j.io.read.PackedRecordSegmentReader#readAsSegments}.
 *
 * <p>A segment is any string of text tried against patterns — it may be a full physical line
 * (line-based readers) or a fixed-width chunk extracted from a physical line (packed readers).
 * Each segment is represented as either a {@link ParsedSegment} (the segment matched a registered
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record} class and was parsed) or an
 * {@link UnmatchedSegment} (the segment did not match any registered pattern and is held
 * verbatim).</p>
 *
 * <p>The ordered {@code List<Segment>} returned by {@code readAsSegments} preserves the original
 * segment order of the source, enabling a read-edit-write round trip via
 * {@link com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 * @see ParsedSegment
 * @see UnmatchedSegment
 */
public interface Segment {
}
