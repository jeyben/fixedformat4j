---
title: Usage
---

# Usage

## Overview

To read and write text to and from Java objects, annotate your Java object with instructions on where data is located in the string and how to translate the data into meaningful objects.

Use an instance of `FixedFormatManager` to load and export the data to and from string representation.

Fixedformat4j requires your Java object to follow these conventions:

1. A class to be used with fixedformat4j must be annotated with `@Record`.
2. A property is a pair of a getter and a setter method.
3. A `@Field` annotation on a getter (or directly on the field itself, since 1.5.0) tells the manager how to convert to and from string representation.

### Example

The following example defines one string property on a POJO called `BasicUsageRecord`:

```java
@Record
public class BasicUsageRecord {

  private String stringData;

  @Field(offset = 1, length = 35)
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }
}
```

Since 1.5.0, `@Field` can also be placed on the field itself — see [Example 6](../examples#example-6--field-annotations-and-lombok) for the Lombok-friendly style.

## FixedFormatManager

An instance of `FixedFormatManager` translates annotation instructions and controls the actual parsing and formatting.

Fixedformat4j ships with `FixedFormatManagerImpl`:

```java
FixedFormatManager manager = new FixedFormatManagerImpl();

// load the string into an object representation
BasicUsageRecord record = manager.load(BasicUsageRecord.class, "initial string");

// operate the record in object representation
record.setStringData("some other string");

// export back to string representation
String exportedString = manager.export(record);
// ... do something with the new string
```

## FixedFormatReader — reading files

`FixedFormatReader` builds on `FixedFormatManager` to process whole files or streams. It reads line by line, routes each line to the correct `@Record` class via a configurable `LinePattern`, and returns the results as a `List`, a `Map` grouped by class, a lazy `Stream`, or via a per-record callback.

See the [File Processing](file-processing) page for the full guide including multi-type files, streaming, strategies, and error handling.

## Annotations

Annotations instruct the manager on how to load and export data. See the [complete annotations reference](annotations).

