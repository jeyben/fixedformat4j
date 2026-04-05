---
title: fixedformat4j
---

# Fixedformat4j

Fixedformat4j is an easy to use, small and non-intrusive Java framework for working with flat fixed formatted text files. By annotating your code you can set up the offsets and format for your data when reading and writing to and from flat fixed format files.

Fixedformat4j handles many built-in datatypes: `String`, `Character`/`char`, `Long`/`long`, `Integer`/`int`, `Short`/`short`, `Double`/`double`, `Float`/`float`, `Boolean`/`boolean`, `Date`, `LocalDate`, and `BigDecimal`.

It is also straightforward to write and plug in your own formatters for custom datatypes.

## Why should I use Fixedformat4j?

- Support for both loading and exporting objects to and from text
- Uses annotations as a clean way to instruct how your data should be read and written
- Support for many built-in datatypes — no need to write format and parse routines
- Handles signed numbers (e.g. `'-1000'` or `'1000-'` can be treated as negative 1000)
- Detailed error reporting when parsing fails

## Getting started

Annotate your getter methods and use the `FixedFormatManager` to load and export your fixed-format text. The [Quick Start guide](quickstart) walks you through each step with a full working example.

In short: add `@Record` to your class, add `@Field` to each getter with an `offset` and `length`, then call `manager.load(...)` or `manager.export(...)`:

```java
FixedFormatManager manager = new FixedFormatManagerImpl();
String line = "string    001232008-05-29";
BasicRecord record = manager.load(BasicRecord.class, line);
record.setIntegerData(100);
System.out.println(manager.export(record)); // "string    001002008-05-29"
```

See the [Quick Start](quickstart) for the full annotated class and step-by-step explanation, or jump straight to [Examples](examples) for practical scenarios.

---

[Quick Start](quickstart) | [Usage](usage/) | [Examples](examples) | [Get It](get-it) | [FAQ](faq) | [Changelog](changelog)
