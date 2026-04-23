package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.issues.LombokRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderLombok {

  // Layout: 1-10 name, 11-15 age (right/zero-pad), 16-23 birthDate (yyyyMMdd),
  //         24 active (Y/N), 25-34 salary (2 implicit decimals)
  private static final String TEST_DATA = "Jacob     0004219850315Y0000123456";

  @TempDir
  Path tempDir;

  private FixedFormatReader reader() {
    return FixedFormatReader.builder()
        .addMapping(LombokRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();
  }

  @Test
  void readsLombokRecordFromReader() {
    List<LombokRecord> results = reader()
        .readAsTypedResult(new StringReader(TEST_DATA))
        .get(LombokRecord.class);
    assertEquals(1, results.size());
    assertEquals("Jacob", results.get(0).getName());
    assertEquals(42, results.get(0).getAge());
    assertTrue(results.get(0).getActive());
  }

  @Test
  void readsMultipleLombokRecordsFromReader() {
    List<LombokRecord> results = reader()
        .readAsTypedResult(new StringReader(TEST_DATA + "\n" + TEST_DATA))
        .get(LombokRecord.class);
    assertEquals(2, results.size());
    assertEquals("Jacob", results.get(0).getName());
    assertEquals("Jacob", results.get(1).getName());
  }

  @Test
  void readsLombokRecordFromFile() throws IOException {
    Path file = tempDir.resolve("records.txt");
    Files.writeString(file, TEST_DATA, StandardCharsets.UTF_8);

    List<LombokRecord> results = reader()
        .readAsTypedResult(file.toFile())
        .get(LombokRecord.class);
    assertEquals(1, results.size());
    assertEquals("Jacob", results.get(0).getName());
  }

  @Test
  void readsLombokRecordFromPath() throws IOException {
    Path file = tempDir.resolve("records.txt");
    Files.writeString(file, TEST_DATA, StandardCharsets.UTF_8);

    List<LombokRecord> results = reader()
        .readAsTypedResult(file)
        .get(LombokRecord.class);
    assertEquals(1, results.size());
    assertEquals("Jacob", results.get(0).getName());
  }

  @Test
  void patternMatchesOnlyLombokLines() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(LombokRecord.class, new RegexFixedFormatMatchPattern("^Jacob"))
        .build();

    String input = TEST_DATA + "\nOther     0000119990101N0000000001";
    List<LombokRecord> results = reader
        .readAsTypedResult(new StringReader(input))
        .get(LombokRecord.class);

    assertEquals(1, results.size());
    assertEquals("Jacob", results.get(0).getName());
  }

  @Test
  void unmatchedLineForwardedToLambdaStrategy() {
    List<String> captured = new ArrayList<>();
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(LombokRecord.class, new RegexFixedFormatMatchPattern("^Jacob"))
        .unmatchedLineStrategy((lineNumber, line) -> captured.add(lineNumber + ":" + line))
        .build();

    String input = TEST_DATA + "\nOther     0000119990101N0000000001";
    List<LombokRecord> results = reader
        .readAsTypedResult(new StringReader(input))
        .get(LombokRecord.class);

    assertEquals(1, results.size());
    assertEquals(1, captured.size());
    assertEquals("2:Other     0000119990101N0000000001", captured.get(0));
  }
}
