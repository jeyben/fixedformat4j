package com.ancientprogramming.fixedformat4j.micrometer;

import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end instrumentation of FixedFormatReader through the existing core seams: the
 * instrumented manager plus counting wrappers around the exclude predicate and the
 * unmatch / parse-error strategies. No core changes — the wrappers delegate unchanged.
 */
class TestMeteredReaderStrategies {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final FixedFormatMetrics metrics = FixedFormatMetrics.of(registry);

  @Test
  void readerLinesAreCountedAsProcessedUnmatchedAndErrored() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .manager(metrics.instrument(new FixedFormatManagerImpl()))
        .addMapping(BasicRecord.class, LinePattern.prefix("R"))
        .excludeLines(metrics.countLines(line -> line.startsWith("#")))
        .unmatchStrategy(metrics.countUnmatched(UnmatchStrategy.skip()))
        .parseErrorStrategy(metrics.countParseErrors(ParseErrorStrategy.skipAndLog()))
        .build();

    String file = String.join("\n",
        "# a comment line",          // excluded
        "Rsome     00042",          // loads fine
        "no pattern matches this",   // unmatched
        "Rbad amount xxxxx");        // parse error

    ReadResult result = reader.read(new StringReader(file));

    assertEquals(1, result.get(BasicRecord.class).size());

    assertEquals(4.0, registry.find("fixedformat.reader.lines.processed").counter().count(),
        "every line, including excluded and unmatched ones, counts as processed");
    assertEquals(1.0, registry.find("fixedformat.reader.lines.unmatched").counter().count());
    assertEquals(1.0, registry.find("fixedformat.reader.lines.errors").counter().count());

    assertEquals(2, registry.find("fixedformat.load")
            .tag("record.class", BasicRecord.class.getName()).timer().count(),
        "reader-driven loads (incl. the failed one) flow through the instrumented manager");
  }

  @Test
  void wrappersDelegateToTheWrappedStrategies() {
    StringBuilder unmatchedSeen = new StringBuilder();
    StringBuilder errorsSeen = new StringBuilder();

    FixedFormatReader reader = FixedFormatReader.builder()
        .manager(metrics.instrument(new FixedFormatManagerImpl()))
        .addMapping(BasicRecord.class, LinePattern.prefix("R"))
        .unmatchStrategy(metrics.countUnmatched((lineNumber, line) -> unmatchedSeen.append(lineNumber)))
        .parseErrorStrategy(metrics.countParseErrors((wrapped, line, lineNumber) -> errorsSeen.append(lineNumber)))
        .build();

    reader.read(new StringReader("unmatched\nRbad amount xxxxx"));

    assertEquals("1", unmatchedSeen.toString(), "wrapped unmatch strategy still runs");
    assertEquals("2", errorsSeen.toString(), "wrapped parse-error strategy still runs");
  }
}
