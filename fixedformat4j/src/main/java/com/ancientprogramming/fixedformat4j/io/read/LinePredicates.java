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

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Factory methods for common {@link Predicate}{@code <String>} implementations used with
 * {@link FixedFormatReaderBuilder#addMapping}.
 *
 * <p>Designed for static import:</p>
 * <pre>{@code
 * import static com.ancientprogramming.fixedformat4j.io.read.LinePredicates.regex;
 *
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(HeaderRecord.class, regex("^HDR"))
 *     .addMapping(DetailRecord.class, regex("^DTL"))
 *     .build();
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public final class LinePredicates {

  private LinePredicates() {}

  /**
   * Returns a predicate that tests whether a line contains a match for {@code regex},
   * using find-semantics (partial match, not full match).
   *
   * @param regex the regular expression to compile
   * @return a predicate backed by the compiled pattern
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is invalid
   */
  public static Predicate<String> regex(String regex) {
    return Pattern.compile(regex).asPredicate();
  }
}
