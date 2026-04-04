package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error/exception paths in FixedFormatManagerImpl.
 */
public class TestFixedFormatManagerImplErrors {

  private FixedFormatManager manager;

  @BeforeEach
  public void setUp() {
    manager = new FixedFormatManagerImpl();
  }

  // --- load() error paths ---

  @Test
  public void load_classWithoutRecordAnnotation_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class, () -> manager.load(String.class, "some data"));
  }

  @Test
  public void load_classWithNoDefaultConstructor_throwsFixedFormatException() {
    // NoDefaultConstructorTopLevel has no default constructor and no declaring class
    assertThrows(FixedFormatException.class, () ->
      manager.load(NoDefaultConstructorClass.MyInnerClass.class, "xyz       ")
    );
  }

  @Test
  public void load_unparsableData_throwsParseException() {
    // "foobar" cannot be parsed as an integer for the integer field
    ParseException ex = assertThrows(ParseException.class, () ->
      manager.load(SimpleIntRecord.class, "foobar")
    );
    assertEquals(SimpleIntRecord.class, ex.getAnnotatedClass());
    assertEquals("getNumber", ex.getAnnotatedMethod().getName());
    assertEquals("foobar", ex.getCompleteText());
  }

  @Test
  public void load_unparsableData_parseExceptionContainsFailedText() {
    ParseException ex = assertThrows(ParseException.class, () ->
      manager.load(SimpleIntRecord.class, "foobar")
    );
    // failedText is the substring at the field offset/length
    assertNotNull(ex.getFailedText());
    assertNotNull(ex.getFormatContext());
    assertNotNull(ex.getFormatInstructions());
  }

  // --- export() error paths ---

  @Test
  public void export_classWithoutRecordAnnotation_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class, () -> manager.export("not annotated"));
  }

  // --- Helper record classes ---

  @Record
  public static class SimpleIntRecord {
    private int number;

    @Field(offset = 1, length = 6, align = Align.RIGHT, paddingChar = '0')
    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }
  }
}
