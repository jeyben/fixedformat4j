package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestRecordInstantiator {

  private final RecordInstantiator instantiator = new RecordInstantiator();

  @Test
  void instantiate_plainClass_returnsNewInstance() {
    PlainRecord instance = instantiator.instantiate(PlainRecord.class);
    assertNotNull(instance);
  }

  @Test
  void instantiate_staticNestedClass_returnsNewInstance() {
    StaticNestedRecord instance = instantiator.instantiate(StaticNestedRecord.class);
    assertNotNull(instance);
  }

  @Test
  void instantiate_innerClass_returnsNewInstance() {
    InnerClassHost.InnerRecord instance = instantiator.instantiate(InnerClassHost.InnerRecord.class);
    assertNotNull(instance);
  }

  @Test
  void instantiate_missingDefaultConstructor_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> instantiator.instantiate(NoDefaultConstructorRecord.class));
  }

  @Test
  void instantiate_missingDefaultConstructor_exceptionMentionsClassName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> instantiator.instantiate(NoDefaultConstructorRecord.class));
    assertTrue(ex.getMessage().contains(NoDefaultConstructorRecord.class.getName()),
        "message should name the class: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("default constructor"),
        "message should mention 'default constructor': " + ex.getMessage());
  }

  @Test
  void instantiate_declaringClassMissingDefaultConstructor_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> instantiator.instantiate(NoDefaultConstructorHost.InnerRecord.class));
  }

  @Test
  void instantiate_declaringClassMissingDefaultConstructor_exceptionMentionsBothClasses() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> instantiator.instantiate(NoDefaultConstructorHost.InnerRecord.class));
    String msg = ex.getMessage();
    assertTrue(msg.contains("InnerRecord") || msg.contains("NoDefaultConstructorHost"),
        "message should mention one of the involved classes: " + msg);
    assertTrue(msg.contains("default constructor"),
        "message should mention 'default constructor': " + msg);
  }

  @Test
  void instantiate_privateNoArgConstructor_throwsFixedFormatException() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> instantiator.instantiate(PrivateConstructorRecord.class));
    assertNotNull(ex.getMessage(), "exception message should not be null");
  }

  @Test
  void instantiate_declaringClassConstructorThrows_throwsFixedFormatExceptionWithMessage() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> instantiator.instantiate(ThrowingConstructorHost.InnerRecord.class));
    String msg = ex.getMessage();
    assertTrue(msg.contains("ThrowingConstructorHost") || msg.contains("unable"),
        "message should be informative about the failure: " + msg);
  }

  // --- Fixture classes ---

  @Record
  public static class PlainRecord {
    @Field(offset = 1, length = 5)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  @Record
  public static class StaticNestedRecord {
    @Field(offset = 1, length = 5)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  public static class InnerClassHost {
    @Record
    public class InnerRecord {
      @Field(offset = 1, length = 5)
      public String getValue() { return null; }
      public void setValue(String v) {}
    }
  }

  @Record
  public static class NoDefaultConstructorRecord {
    public NoDefaultConstructorRecord(String required) {}

    @Field(offset = 1, length = 5)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  public static class NoDefaultConstructorHost {
    public NoDefaultConstructorHost(String required) {}

    @Record
    public class InnerRecord {
      @Field(offset = 1, length = 5)
      public String getValue() { return null; }
      public void setValue(String v) {}
    }
  }

  @Record
  public static class PrivateConstructorRecord {
    private PrivateConstructorRecord() {}

    @Field(offset = 1, length = 5)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  public static class ThrowingConstructorHost {
    public ThrowingConstructorHost() {
      throw new RuntimeException("host constructor always fails");
    }

    @Record
    public class InnerRecord {
      @Field(offset = 1, length = 5)
      public String getValue() { return null; }
      public void setValue(String v) {}
    }
  }
}
