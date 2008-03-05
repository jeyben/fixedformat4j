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
 * todo: comment needed
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestDirection extends TestCase {

  public void testLeftPadding() {
    assertEquals(" ", Direction.LEFT.apply(null, 1, ' '));
    assertEquals(" ", Direction.LEFT.apply(" ", 1, ' '));
    assertEquals("r", Direction.LEFT.apply("foobar", 1, ' '));
    assertEquals("bar", Direction.LEFT.apply("foobar", 3, ' '));
    assertEquals("foobar", Direction.LEFT.apply("foobar", 6, ' '));
    assertEquals(" foobar", Direction.LEFT.apply("foobar", 7, ' '));
    assertEquals("__foobar", Direction.LEFT.apply("foobar", 8, '_'));
  }

  public void testLeftRemove() {
    assertEquals("", Direction.LEFT.remove(null, ' '));
    assertEquals("", Direction.LEFT.remove(" ", ' '));
    assertEquals("foobar  ", Direction.LEFT.remove("foobar  ", ' '));     
    assertEquals("foobar", Direction.LEFT.remove("  foobar", ' '));
  }

  public void testRightPadding() {
    assertEquals(" ", Direction.RIGHT.apply(null, 1, ' '));
    assertEquals(" ", Direction.RIGHT.apply(" ", 1, ' '));
    assertEquals("f", Direction.RIGHT.apply("foobar", 1, ' '));
    assertEquals("foo", Direction.RIGHT.apply("foobar", 3, ' '));
    assertEquals("foobar", Direction.RIGHT.apply("foobar", 6, ' '));
    assertEquals("foobar ", Direction.RIGHT.apply("foobar", 7, ' '));
    assertEquals("foobar__", Direction.RIGHT.apply("foobar", 8, '_'));
  }

  public void testRightRemove() {
    assertEquals("", Direction.RIGHT.remove(null, ' '));
    assertEquals("", Direction.RIGHT.remove(" ", ' '));
    assertEquals("foobar", Direction.RIGHT.remove("foobar  ", ' '));
    assertEquals("  foobar", Direction.RIGHT.remove("  foobar", ' '));
  }
}
