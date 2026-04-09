---
title: FAQ
---

# Frequently Asked Questions

## Can fixedformat4j help me parse large text files?

As the API stands it does not provide file-iteration utilities, but it does not constrain you in any way either.

Fixedformat4j concentrates on mapping one single line to a Java object. You are free to loop through any large text file and use the `FixedFormatManager` to create instances of Java objects for each line.

## Can I apply my own custom formatter?

Yes.

Extend `AbstractFixedFormatter<T>` (recommended) or implement `FixedFormatter<T>` directly. The two methods you must provide are `asObject` (string → your type) and `asString` (your type → string):

```java
public class CustomFormatter extends AbstractFixedFormatter<MyType> {

  @Override
  public MyType asObject(String value, FormatInstructions instructions) {
    // 'value' has already had padding stripped by the base class
    return MyType.parse(value);
  }

  @Override
  public String asString(MyType value, FormatInstructions instructions) {
    return value.toFixedString();
  }
}
```

To access supplementary annotation data (e.g., a pattern supplied via `@FixedFormatPattern`), use the `FormatInstructions` argument:

```java
String pattern = instructions.getFixedFormatPatternData().getPattern();
```

To instruct the manager to use your custom formatter, set the `formatter` attribute on the `@Field` annotation:

```java
@Field(offset = 1, length = 10, formatter = CustomFormatter.class)
```

See [Example 5 — Custom formatter](examples#example-5--custom-formatter) for a complete working implementation.

## How do I handle records with different layouts in the same file?

Define a separate `@Record`-annotated class for each layout. When reading the file line by line, inspect a discriminator field (such as a record-type code in a known column) and call `manager.load(...)` with the matching class:

```java
while ((line = reader.readLine()) != null) {
    String type = line.substring(0, 1); // type code in column 1
    if ("H".equals(type)) {
        headers.add(manager.load(HeaderRecord.class, line));
    } else if ("D".equals(type)) {
        details.add(manager.load(DetailRecord.class, line));
    }
}
```

See [Example 4 — Processing a file line by line](examples#example-4--processing-a-file-line-by-line) for a full file-reading snippet.

