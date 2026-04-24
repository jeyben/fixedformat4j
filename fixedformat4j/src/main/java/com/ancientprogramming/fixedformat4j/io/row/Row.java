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
package com.ancientprogramming.fixedformat4j.io.row;

/**
 * A single line from a fixed-format file, as returned by {@link com.ancientprogramming.fixedformat4j.io.FixedFormatReader#readAsRows}.
 *
 * <p>Each line is represented as either a {@link ParsedRow} (the line matched a registered
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record} class and was parsed) or an
 * {@link UnmatchedRow} (the line did not match any registered pattern and is held verbatim).</p>
 *
 * <p>The ordered {@code List<Row>} returned by {@code readAsRows} preserves the original line
 * order of the file, enabling a read-edit-write round trip via {@link com.ancientprogramming.fixedformat4j.io.FixedFormatWriter}.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 * @see ParsedRow
 * @see UnmatchedRow
 */
public interface Row {
}
