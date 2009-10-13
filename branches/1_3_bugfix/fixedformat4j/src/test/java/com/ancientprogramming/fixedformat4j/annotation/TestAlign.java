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
package com.ancientprogramming.fixedformat4j.annotation;

import junit.framework.TestCase;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestAlign extends TestCase {

  public void testLeftPadding() {
    assertEquals(" ", Align.RIGHT.apply(null, 1, ' '));
    assertEquals(" ", Align.RIGHT.apply(" ", 1, ' '));
    assertEquals("r", Align.RIGHT.apply("foobar", 1, ' '));
    assertEquals("bar", Align.RIGHT.apply("foobar", 3, ' '));
    assertEquals("foobar", Align.RIGHT.apply("foobar", 6, ' '));
    assertEquals(" foobar", Align.RIGHT.apply("foobar", 7, ' '));
    assertEquals("__foobar", Align.RIGHT.apply("foobar", 8, '_'));
  }

  public void testLeftRemove() {
    assertEquals("", Align.RIGHT.remove(null, ' '));
    assertEquals("", Align.RIGHT.remove(" ", ' '));
    assertEquals("foobar  ", Align.RIGHT.remove("foobar  ", ' '));
    assertEquals("foobar", Align.RIGHT.remove("  foobar", ' '));
  }

  public void testRightPadding() {
    assertEquals(" ", Align.LEFT.apply(null, 1, ' '));
    assertEquals(" ", Align.LEFT.apply(" ", 1, ' '));
    assertEquals("f", Align.LEFT.apply("foobar", 1, ' '));
    assertEquals("foo", Align.LEFT.apply("foobar", 3, ' '));
    assertEquals("foobar", Align.LEFT.apply("foobar", 6, ' '));
    assertEquals("foobar ", Align.LEFT.apply("foobar", 7, ' '));
    assertEquals("foobar__", Align.LEFT.apply("foobar", 8, '_'));
  }

  public void testRightRemove() {
    assertEquals("", Align.LEFT.remove(null, ' '));
    assertEquals("", Align.LEFT.remove(" ", ' '));
    assertEquals("foobar", Align.LEFT.remove("foobar  ", ' '));
    assertEquals("  foobar", Align.LEFT.remove("  foobar", ' '));
  }
}
