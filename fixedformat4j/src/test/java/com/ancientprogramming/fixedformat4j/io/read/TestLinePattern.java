package com.ancientprogramming.fixedformat4j.io.read;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  // --- Message content assertions ---

  @Test
  void positionalNullPositions_npeContainsParameterName() {
    NullPointerException ex = assertThrows(NullPointerException.class,
        () -> LinePattern.positional(null, "ABC"));
    assertTrue(ex.getMessage().contains("positions"));
  }

  @Test
  void positionalNullLiteral_npeContainsParameterName() {
    NullPointerException ex = assertThrows(NullPointerException.class,
        () -> LinePattern.positional(new int[]{0, 1, 2}, null));
    assertTrue(ex.getMessage().contains("literal"));
  }

  @Test
  void positionalEmptyPositions_messageSuggestsMatchAll() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[0], ""));
    assertTrue(ex.getMessage().contains("matchAll()"));
  }

  @Test
  void positionalLengthMismatch_messageMentionsBothLengths() {
    // positions.length=2, literal.length()=3 → "positions length 2 does not equal literal length 3"
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{0, 1}, "ABC"));
    assertTrue(ex.getMessage().contains("2"));
    assertTrue(ex.getMessage().contains("3"));
  }

  @Test
  void positionalNegativePosition_messageMentionsNegativeValue() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{-7, 0}, "AB"));
    assertTrue(ex.getMessage().contains("-7"));
  }

  @Test
  void positionalDuplicateAtIndex1_messageContainsAscendingViolation() {
    // positions[1]=0 <= positions[0]=0 → "got 0 after 0 at index 1"
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{0, 0, 2}, "ABC"));
    assertTrue(ex.getMessage().contains("got 0 after 0 at index 1"));
  }

  @Test
  void positionalDescendingAtLastIndex_messageContainsViolationDetails() {
    // positions[2]=1 <= positions[1]=2 → "got 1 after 2 at index 2"
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{0, 2, 1}, "ABC"));
    assertTrue(ex.getMessage().contains("got 1 after 2 at index 2"));
  }

  @Test
  void positionalDuplicateAtMiddleIndex_messageContainsViolationDetails() {
    // positions[2]=1 <= positions[1]=1 → "got 1 after 1 at index 2"
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.positional(new int[]{0, 1, 1, 3}, "ABCD"));
    assertTrue(ex.getMessage().contains("got 1 after 1 at index 2"));
  }

  @Test
  void prefixNullLiteral_npeContainsParameterName() {
    NullPointerException ex = assertThrows(NullPointerException.class,
        () -> LinePattern.prefix(null));
    assertTrue(ex.getMessage().contains("literal"));
  }

  @Test
  void prefixEmptyLiteral_messageSuggestsMatchAll() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> LinePattern.prefix(""));
    assertTrue(ex.getMessage().contains("matchAll()"));
  }
}
