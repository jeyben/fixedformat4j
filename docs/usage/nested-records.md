---
title: Nested Records
---

# Nested Records

## Overview

A `@Record`-annotated class can contain nested `@Record`-annotated member objects that will be processed by the `FixedFormatManager`.

When the `FixedFormatManager` encounters a `@Field`-annotated getter method whose return type is itself annotated with `@Record`, it formats the field as a record and does not use any configured `formatter`.

## Example

The following shows how nested records work. First, the inner record (`BasicRecord`):

```java
@Record
public class BasicRecord {

  private String stringData;
  private Integer integerData;
  private Date dateData;

  @Field(offset = 1, length = 10)
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }

  @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
  public Integer getIntegerData() {
    return integerData;
  }

  public void setIntegerData(Integer integerData) {
    this.integerData = integerData;
  }

  @Field(offset = 16, length = 10)
  @FixedFormatPattern("yyyy-MM-dd")
  public Date getDateData() {
    return dateData;
  }

  public void setDateData(Date dateData) {
    this.dateData = dateData;
  }
}
```

Now the outer record that embeds `BasicRecord`:

```java
@Record
public class NestedRecord {

  private String stringData;
  private BasicRecord record; //this instance is @Record annotated

  @Field(offset = 1, length = 5)
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }

  @Field(offset = 6, length = 25)
  public BasicRecord getRecord() {
    return record;
  }

  public void setRecord(BasicRecord record) {
    this.record = record;
  }
}
```

A simple program showing how to access nested record data:

```java
FixedFormatManager manager = new FixedFormatManagerImpl();

// load the string into an object representation
String text = "foo  bar       001232008-10-21";
System.out.println(text);
NestedRecord record = manager.load(NestedRecord.class, text);

// print the output
System.out.println(record.getStringData());
BasicRecord nestedRecord = record.getRecord();
System.out.println(nestedRecord.getStringData());
System.out.println(nestedRecord.getIntegerData());
System.out.println(nestedRecord.getDateData());

// modify the nested record and export
nestedRecord.setStringData("fubar");
nestedRecord.setIntegerData(9876);

String exportedString = manager.export(record);
System.out.println(exportedString);
```

The output looks like this:

```
foo  bar       001232008-10-21
foo
bar
123
Tue Oct 21 00:00:00 CEST 2008
foo  fubar     098762008-10-21
```

---

[Home](../index) | [Usage](index) | [Annotations](annotations)
