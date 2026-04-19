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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link FixedFormatMatchPattern} that matches lines using a regular expression.
 * The regex is compiled at construction time; an invalid expression throws
 * {@link com.ancientprogramming.fixedformat4j.exception.FixedFormatException} immediately
 * rather than at match time.
 *
 * <p>Example — match any line that starts with {@code "HDR"}:</p>
 * <pre>{@code
 * FixedFormatMatchPattern pattern = new RegexFixedFormatMatchPattern("^HDR");
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public class RegexFixedFormatMatchPattern implements FixedFormatMatchPattern {

  private final Pattern pattern;

  /**
   * Creates a new pattern that matches lines against the given regular expression.
   *
   * @param regex the regular expression to compile; uses {@link java.util.regex.Matcher#find()}
   *              semantics so the expression does not need to match the full line
   * @throws FixedFormatException if {@code regex} is not a valid regular expression
   */
  public RegexFixedFormatMatchPattern(String regex) {
    try {
      this.pattern = Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      throw new FixedFormatException("Invalid regex pattern: " + regex, e);
    }
  }

  /**
   * Returns {@code true} if the compiled regular expression finds a match anywhere in
   * {@code line}.
   *
   * @param line the raw text of the line; returns {@code false} when {@code null}
   * @return {@code true} if the pattern matches; {@code false} if not, or if {@code line}
   *         is {@code null}
   */
  @Override
  public boolean matches(String line) {
    if (line == null) {
      return false;
    }
    return pattern.matcher(line).find();
  }
}
