package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.lang.String.format;

/**
 * Builds {@link FormatInstructions} and {@link FormatContext} from field annotations.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.6.0
 */
class FormatInstructionsBuilder {

  FormatInstructions build(AnnotatedElement annotationSource, Field fieldAnno, Class<?> datatype) {
    FixedFormatPatternData patternData = patternData(annotationSource.getAnnotation(FixedFormatPattern.class), datatype);
    FixedFormatBooleanData booleanData = booleanData(annotationSource.getAnnotation(FixedFormatBoolean.class));
    FixedFormatNumberData numberData = numberData(annotationSource.getAnnotation(FixedFormatNumber.class));
    FixedFormatDecimalData decimalData = decimalData(annotationSource.getAnnotation(FixedFormatDecimal.class));
    return new FormatInstructions(fieldAnno.length(), fieldAnno.align(), fieldAnno.paddingChar(), patternData, booleanData, numberData, decimalData);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  FormatContext<?> context(Class<?> datatype, Field fieldAnno) {
    if (fieldAnno != null) {
      return new FormatContext(fieldAnno.offset(), datatype, fieldAnno.formatter());
    }
    return null;
  }

  Class<?> datatype(Method method, Field fieldAnno) {
    if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
      return method.getReturnType();
    }
    throw new FixedFormatException(format(
        "Cannot annotate method %s, with %s annotation. %s annotations must be placed on methods starting with 'get' or 'is'",
        method.getName(), fieldAnno.getClass().getName(), fieldAnno.getClass().getName()));
  }

  private FixedFormatPatternData patternData(FixedFormatPattern annotation, Class<?> datatype) {
    if (annotation != null) {
      return new FixedFormatPatternData(annotation.value());
    }
    if (LocalDate.class.equals(datatype)) {
      return FixedFormatPatternData.LOCALDATE_DEFAULT;
    }
    if (LocalDateTime.class.equals(datatype)) {
      return FixedFormatPatternData.DATETIME_DEFAULT;
    }
    return FixedFormatPatternData.DEFAULT;
  }

  private FixedFormatBooleanData booleanData(FixedFormatBoolean annotation) {
    if (annotation != null) {
      return new FixedFormatBooleanData(annotation.trueValue(), annotation.falseValue());
    }
    return FixedFormatBooleanData.DEFAULT;
  }

  private FixedFormatNumberData numberData(FixedFormatNumber annotation) {
    if (annotation != null) {
      return new FixedFormatNumberData(annotation.sign(), annotation.positiveSign(), annotation.negativeSign());
    }
    return FixedFormatNumberData.DEFAULT;
  }

  private FixedFormatDecimalData decimalData(FixedFormatDecimal annotation) {
    if (annotation != null) {
      return new FixedFormatDecimalData(annotation.decimals(), annotation.useDecimalDelimiter(), annotation.decimalDelimiter(), RoundingMode.valueOf(annotation.roundingMode()));
    }
    return FixedFormatDecimalData.DEFAULT;
  }
}
