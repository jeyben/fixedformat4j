---
title: fixedformat4j
---

# Fixedformat4j

Fixedformat4j is an easy to use, small and non-intrusive Java framework for working with flat fixed formatted text files. By annotating your code you can set up the offsets and format for your data when reading and writing to and from flat fixed format files.

Fixedformat4j handles many built-in datatypes: `String`, `Character`/`char`, `Long`/`long`, `Integer`/`int`, `Short`/`short`, `Double`/`double`, `Float`/`float`, `Boolean`/`boolean`, `Date`, and `BigDecimal`.

It is also straightforward to write and plug in your own formatters for custom datatypes.

## Why should I use Fixedformat4j?

- Support for both loading and exporting objects to and from text
- Uses annotations as a clean way to instruct how your data should be read and written
- Support for many built-in datatypes — no need to write format and parse routines
- Handles signed numbers (e.g. `'-1000'` or `'1000-'` can be treated as negative 1000)
- Detailed error reporting when parsing fails

## Getting started

Annotate your getter methods and use the `FixedFormatManager` to load and export your fixed-format text.

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

This annotated class can now be loaded and exported using a `FixedFormatManager`:

```java
public class BasicUsage {

  private static FixedFormatManager manager = new FixedFormatManagerImpl();

  public static void main(String[] args) {
    String string = "string    001232008-05-29";
    BasicRecord record = manager.load(BasicRecord.class, string);

    System.out.println("The parsed string: " + record.getStringData());
    System.out.println("The parsed integer: " + record.getIntegerData());
    System.out.println("The parsed date: " + record.getDateData());

    record.setIntegerData(100);
    System.out.println("Exported: " + manager.export(record));
  }
}
```

Running this program produces:

```
The parsed string: string
The parsed integer: 123
The parsed date: Thu May 29 00:00:00 CEST 2008
Exported: string    001002008-05-29
```

Note that the integer changed value in the exported string.

---

[Usage](usage/) | [Get It](get-it) | [FAQ](faq)
