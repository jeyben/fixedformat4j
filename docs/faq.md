---
title: FAQ
---

# Frequently Asked Questions

## Can fixedformat4j help me parse large text files?

As the API stands it does not provide file-iteration utilities, but it does not constrain you in any way either.

Fixedformat4j concentrates on mapping one single line to a Java object. You are free to loop through any large text file and use the `FixedFormatManager` to create instances of Java objects for each line.

## Can I apply my own custom formatter?

Yes.

To map a domain object, create a custom formatter implementing the `FixedFormatter` interface.

Your custom formatter will have access to all annotation data through a `FormatInstructions` instance, which is passed to your formatter's `parse` and `format` methods when the `FixedFormatManager` calls them. For example, to access the value given to `@FixedFormatPattern` you call `formatInstructions.getFixedFormatPatternData().getPattern()`.

To instruct the manager to use your custom formatter, set the `formatter` attribute on the `@Field` annotation:

```java
@Field(offset = 1, length = 10, formatter = CustomFormatter.class)
```

---

[Home](index) | [Usage](usage/) | [Get It](get-it)
