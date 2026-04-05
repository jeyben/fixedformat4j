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
3. The getter method is annotated with instructions on how to convert to and from string representation.

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

## Annotations

Annotations instruct the manager on how to load and export data. See the [complete annotations reference](annotations).

---

[Home](../index) | [Annotations](annotations) | [Nested Records](nested-records) | [Get It](../get-it)
