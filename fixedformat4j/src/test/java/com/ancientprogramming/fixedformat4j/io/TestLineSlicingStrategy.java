package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.LineSlicingStrategy;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class TestLineSlicingStrategy {

  // --- singleRecord ---

  @Test
  void singleRecordAlwaysReturnsEmpty() {
    LineSlicingStrategy strategy = LineSlicingStrategy.singleRecord();

    assertEquals(OptionalInt.empty(), strategy.recordWidthFor("anything"));
    assertEquals(OptionalInt.empty(), strategy.recordWidthFor(""));
    assertEquals(OptionalInt.empty(), strategy.recordWidthFor("AAAAAAAAAA"));
  }

  // --- packed ---

  @Test
  void packedAlwaysReturnsWidth() {
    LineSlicingStrategy strategy = LineSlicingStrategy.packed(10);

    assertEquals(OptionalInt.of(10), strategy.recordWidthFor("AAAAAAAAAA"));
    assertEquals(OptionalInt.of(10), strategy.recordWidthFor("something else"));
    assertEquals(OptionalInt.of(10), strategy.recordWidthFor(""));
  }

  @Test
  void packedRejectsZeroWidth() {
    assertThrows(IllegalArgumentException.class, () -> LineSlicingStrategy.packed(0));
  }

  @Test
  void packedRejectsNegativeWidth() {
    assertThrows(IllegalArgumentException.class, () -> LineSlicingStrategy.packed(-1));
  }

  @Test
  void packedAcceptsWidthOfOne() {
    LineSlicingStrategy strategy = LineSlicingStrategy.packed(1);
    assertEquals(OptionalInt.of(1), strategy.recordWidthFor("X"));
  }

  // --- mixed ---

  @Test
  void mixedReturnsWidthForMatchingLines() {
    LineSlicingStrategy strategy = LineSlicingStrategy.mixed(line -> line.startsWith("DTL"), 128);

    assertEquals(OptionalInt.of(128), strategy.recordWidthFor("DTLsomecontent"));
  }

  @Test
  void mixedReturnsEmptyForNonMatchingLines() {
    LineSlicingStrategy strategy = LineSlicingStrategy.mixed(line -> line.startsWith("DTL"), 128);

    assertEquals(OptionalInt.empty(), strategy.recordWidthFor("HDR header"));
    assertEquals(OptionalInt.empty(), strategy.recordWidthFor("TRL trailer"));
  }

  @Test
  void mixedRejectsNullPredicate() {
    assertThrows(NullPointerException.class, () -> LineSlicingStrategy.mixed(null, 10));
  }

  @Test
  void mixedRejectsZeroWidth() {
    assertThrows(IllegalArgumentException.class,
        () -> LineSlicingStrategy.mixed(line -> true, 0));
  }

  @Test
  void mixedRejectsNegativeWidth() {
    assertThrows(IllegalArgumentException.class,
        () -> LineSlicingStrategy.mixed(line -> true, -5));
  }

  // --- lambda / custom ---

  @Test
  void customLambdaStrategyIsAccepted() {
    LineSlicingStrategy strategy = line -> line.length() > 20
        ? OptionalInt.of(10)
        : OptionalInt.empty();

    assertEquals(OptionalInt.empty(), strategy.recordWidthFor("short"));
    assertEquals(OptionalInt.of(10), strategy.recordWidthFor("this line is quite long indeed"));
  }
}
