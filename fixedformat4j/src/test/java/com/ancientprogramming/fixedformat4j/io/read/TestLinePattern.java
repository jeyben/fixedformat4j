package com.ancientprogramming.fixedformat4j.io.read;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestLinePattern {

  @Test
  void positionalRetainsPositionsAndLiteral() {
    LinePattern pattern = LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001");
    assertArrayEquals(new int[]{0, 1, 2, 3, 7, 8}, pattern.positions());
    assertEquals("K40001", pattern.literal());
    assertEquals(6, pattern.depth());
  }

  @Test
  void prefixCreatesPositionalFromZeroToLengthMinusOne() {
    LinePattern pattern = LinePattern.prefix("HDR");
    assertArrayEquals(new int[]{0, 1, 2}, pattern.positions());
    assertEquals("HDR", pattern.literal());
    assertEquals(3, pattern.depth());
  }

  @Test
  void matchAllHasDepthZeroAndNoLiteral() {
    LinePattern pattern = LinePattern.matchAll();
    assertEquals(0, pattern.depth());
    assertEquals(0, pattern.positions().length);
    assertNull(pattern.literal());
  }

  @Test
  void positionalArrayIsDefensivelyCopied() {
    int[] input = {0, 1, 2};
    LinePattern pattern = LinePattern.positional(input, "ABC");
    input[0] = 99;
    assertArrayEquals(new int[]{0, 1, 2}, pattern.positions());
  }

  @Test
  void positionalRejectsNullPositions() {
    assertThrows(NullPointerException.class,
        () -> LinePattern.positional(null, "ABC"));
  }

  @Test
  void positionalRejectsNullLiteral() {
    assertThrows(NullPointerException.class,
        () -> LinePattern.positional(new int[]{0, 1, 2}, null));
  }

  @Test
  void positionalRejectsLengthMismatch() {
    assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{0, 1}, "ABC"));
  }

  @Test
  void positionalRejectsEmptyPositions() {
    assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[0], ""));
  }

  @Test
  void positionalRejectsNegativePosition() {
    assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{-1, 0, 1}, "ABC"));
  }

  @Test
  void positionalRejectsDuplicatePositions() {
    assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{0, 1, 1}, "ABC"));
  }

  @Test
  void positionalRejectsUnsortedPositions() {
    assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{2, 1, 0}, "ABC"));
  }

  @Test
  void prefixRejectsNullLiteral() {
    assertThrows(NullPointerException.class,
        () -> LinePattern.prefix(null));
  }

  @Test
  void prefixRejectsEmptyLiteral() {
    assertThrows(IllegalArgumentException.class,
        () -> LinePattern.prefix(""));
  }

  @Test
  void factoryReturnsNonNull() {
    assertNotNull(LinePattern.matchAll());
    assertNotNull(LinePattern.prefix("X"));
    assertNotNull(LinePattern.positional(new int[]{0}, "X"));
  }
}
