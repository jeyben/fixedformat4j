---
title: Quick Start
---

# Quick Start

This guide takes you from zero to a working fixed-format record in four steps.

## Prerequisites

- Java 11 or later
- Maven (or Gradle — adapt the dependency coordinates accordingly)

## Step 1 — Add the dependency

Add fixedformat4j to your `pom.xml`:

```xml
<dependency>
  <groupId>com.ancientprogramming.fixedformat4j</groupId>
  <artifactId>fixedformat4j</artifactId>
  <version>1.3.7</version>
</dependency>
```

The latest version is always available on [Maven Central](https://search.maven.org/artifact/com.ancientprogramming.fixedformat4j/fixedformat4j).

## Step 2 — Annotate your record class

Create a plain Java class, annotate it with `@Record`, and place a `@Field` annotation on each getter method.

```java
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.annotation.Align;
import java.util.Date;

@Record
public class EmployeeRecord {

  private String name;
  private Integer employeeId;
  private Date hireDate;

  @Field(offset = 1, length = 20)
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  @Field(offset = 21, length = 6, align = Align.RIGHT, paddingChar = '0')
  public Integer getEmployeeId() { return employeeId; }
  public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

  @Field(offset = 27, length = 10)
  @FixedFormatPattern("yyyy-MM-dd")
  public Date getHireDate() { return hireDate; }
  public void setHireDate(Date hireDate) { this.hireDate = hireDate; }
}
```

Key rules:
- The class must be annotated with `@Record`.
- Every mapped field needs a getter **and** a setter.
- `@Field` goes on the getter, not the setter or the field.
- `offset` is **1-based** — offset 1 means the first character.

## Step 3 — Load from a string

Use `FixedFormatManagerImpl` to parse a fixed-width string into your record class:

```java
FixedFormatManager manager = new FixedFormatManagerImpl();

String line = "Jane Smith          0042322024-03-15";
EmployeeRecord record = manager.load(EmployeeRecord.class, line);

System.out.println(record.getName());       // "Jane Smith"
System.out.println(record.getEmployeeId()); // 423
System.out.println(record.getHireDate());   // Fri Mar 15 00:00:00 ... 2024
```

The manager:
1. Reads each `@Field` annotation to locate the substring (`offset`, `length`).
2. Strips padding characters.
3. Converts the stripped string to the field's Java type using a built-in formatter.
4. Calls the corresponding setter to populate the object.

## Step 4 — Export to a string

Modify the object and call `export` to produce a new fixed-width string:

```java
record.setEmployeeId(999);
String exported = manager.export(record);
System.out.println(exported);
// "Jane Smith          0009992024-03-15"
```

The export re-pads every field to its declared length using the configured alignment and padding character.

---

## What's next

- [Annotations reference](usage/annotations) — full details on every annotation attribute
- [Examples](examples) — practical scenarios: financial records, booleans, file processing, custom formatters
- [Nested Records](usage/nested-records) — embedding one `@Record` class inside another
- [FAQ](faq)

---

[Home](index) | [Usage](usage/) | [Examples](examples) | [Get It](get-it) | [FAQ](faq)
