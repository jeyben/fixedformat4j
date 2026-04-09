package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.fetchData;
import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance;
import static java.lang.String.format;

/**
 * Handles all {@code count > 1} (repeating) field logic for both reading and exporting.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.5.1
 */
class RepeatingFieldSupport {

  private static final Logger LOG = LoggerFactory.getLogger(RepeatingFieldSupport.class);

  private final FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();

  // -------------------------------------------------------------------------
  // Read
  // -------------------------------------------------------------------------

  @SuppressWarnings({"unchecked", "rawtypes"})
  Object read(Class<?> clazz, String data, Method getter, AnnotatedElement annotationSource, Field fieldAnno) {
    int count = fieldAnno.count();
    Class<?> elementType = resolveElementType(getter);
    FormatInstructions formatdata = instructionsBuilder.build(annotationSource, fieldAnno);

    FormatContext protoContext = new FormatContext(fieldAnno.offset(), elementType, fieldAnno.formatter());
    FixedFormatter<Object> formatter = (FixedFormatter<Object>) getFixedFormatterInstance(protoContext.getFormatter(), protoContext);

    List<Object> elements = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int elementOffset = fieldAnno.offset() + fieldAnno.length() * i;
      FormatContext elementContext = new FormatContext(elementOffset, elementType, fieldAnno.formatter());
      String dataToParse = fetchData(data, formatdata, elementContext);
      try {
        elements.add(formatter.parse(dataToParse, formatdata));
      } catch (RuntimeException e) {
        throw new ParseException(data, dataToParse, clazz, getter, elementContext, formatdata, e);
      }
    }

    return assembleCollection(getter, elements);
  }

  // -------------------------------------------------------------------------
  // Export
  // -------------------------------------------------------------------------

  @SuppressWarnings({"unchecked", "rawtypes"})
  <T> void export(T fixedFormatRecord, AnnotationTarget target, Field fieldAnno, HashMap<Integer, String> foundData) {
    validateCount(target.getter, fieldAnno);

    Object value;
    try {
      value = target.getter.invoke(fixedFormatRecord);
    } catch (Exception e) {
      throw new FixedFormatException(format("could not invoke %s", fieldLabel(target.getter)), e);
    }

    if (value == null) {
      throw new FixedFormatException(format("Cannot export null repeating field on %s", fieldLabel(target.getter)));
    }

    int count = fieldAnno.count();
    int actualSize = value.getClass().isArray() ? Array.getLength(value) : ((Collection<?>) value).size();

    if (actualSize != count) {
      if (fieldAnno.strictExportCount()) {
        throw new FixedFormatException(
            format("Repeating field %s has count=%d but collection size=%d", fieldLabel(target.getter), count, actualSize));
      } else {
        LOG.warn("Repeating field {} has count={} but collection size={}. Exporting {} elements.",
            fieldLabel(target.getter), count, actualSize, Math.min(count, actualSize));
      }
    }

    int exportCount = Math.min(count, actualSize);
    Class<?> elementType = resolveElementType(target.getter);
    FormatInstructions formatdata = instructionsBuilder.build(target.annotationSource, fieldAnno);
    FormatContext protoContext = new FormatContext(fieldAnno.offset(), elementType, fieldAnno.formatter());
    FixedFormatter<Object> formatter = (FixedFormatter<Object>) getFixedFormatterInstance(protoContext.getFormatter(), protoContext);

    Iterable<?> iterable = value.getClass().isArray() ? arrayToIterable(value, exportCount) : (Collection<?>) value;

    int i = 0;
    for (Object element : iterable) {
      if (i >= exportCount) break;
      int elementOffset = fieldAnno.offset() + fieldAnno.length() * i;
      foundData.put(elementOffset, formatter.format(element, formatdata));
      i++;
    }
  }

  // -------------------------------------------------------------------------
  // Validation & utilities — package-private for direct testing
  // -------------------------------------------------------------------------

  void validateCount(Method method, Field fieldAnnotation) {
    int count = fieldAnnotation.count();
    Class<?> returnType = method.getReturnType();
    boolean isArrayOrCollection = returnType.isArray()
        || Collection.class.isAssignableFrom(returnType)
        || Iterable.class.isAssignableFrom(returnType);

    if (count < 1) {
      throw new FixedFormatException(
          format("@Field count must be >= 1 on %s, was: %d", fieldLabel(method), count));
    }
    if (count == 1 && isArrayOrCollection) {
      throw new FixedFormatException(
          format("@Field count=1 but return type is array/collection on %s. Use count > 1 for repeating fields.", fieldLabel(method)));
    }
    if (count > 1 && !isArrayOrCollection) {
      throw new FixedFormatException(
          format("@Field count=%d requires array or Collection return type on %s, found: %s", count, fieldLabel(method), returnType.getName()));
    }
  }

  Class<?> resolveElementType(Method method) {
    Class<?> returnType = method.getReturnType();
    if (returnType.isArray()) {
      return returnType.getComponentType();
    }
    Type genericReturnType = method.getGenericReturnType();
    if (genericReturnType instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) genericReturnType;
      Type[] typeArgs = pt.getActualTypeArguments();
      if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
        return (Class<?>) typeArgs[0];
      }
    }
    throw new FixedFormatException(format("Cannot determine element type for repeating field on %s. Ensure the collection is parameterized (e.g. List<String>, not List).", fieldLabel(method)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  Object assembleCollection(Method getter, List<Object> elements) {
    Class<?> returnType = getter.getReturnType();
    if (returnType.isArray()) {
      Object array = Array.newInstance(returnType.getComponentType(), elements.size());
      for (int i = 0; i < elements.size(); i++) {
        Array.set(array, i, elements.get(i));
      }
      return array;
    } else if (LinkedList.class.isAssignableFrom(returnType)) {
      return new LinkedList<>(elements);
    } else if (List.class.isAssignableFrom(returnType)) {
      return new ArrayList<>(elements);
    } else if (SortedSet.class.isAssignableFrom(returnType)) {
      return new TreeSet<>(elements);
    } else if (Set.class.isAssignableFrom(returnType)) {
      return new LinkedHashSet<>(elements);
    } else if (Collection.class.isAssignableFrom(returnType) || Iterable.class.isAssignableFrom(returnType)) {
      return new ArrayList<>(elements);
    } else {
      throw new FixedFormatException(format("Unsupported collection type %s on %s. Supported types: arrays, List, LinkedList, Set, SortedSet, Collection.", returnType.getName(), fieldLabel(getter)));
    }
  }

  private Iterable<Object> arrayToIterable(Object array, int limit) {
    List<Object> list = new ArrayList<>(limit);
    for (int i = 0; i < limit; i++) {
      list.add(Array.get(array, i));
    }
    return list;
  }

  private static String fieldLabel(Method method) {
    return format("%s#%s()", method.getDeclaringClass().getName(), method.getName());
  }
}
