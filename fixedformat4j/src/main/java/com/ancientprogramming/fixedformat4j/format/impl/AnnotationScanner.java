package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static java.lang.String.format;

/**
 * Discovers all {@link Field} and {@link Fields} annotation targets on a class.
 *
 * <p>Pass 1 walks public methods; pass 2 walks declared fields including superclasses.
 * Field annotations take priority over method annotations for the same property.
 * A conflict (both annotated) is logged as a warning.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.5.1
 */
class AnnotationScanner {

  private static final Logger LOG = LoggerFactory.getLogger(AnnotationScanner.class);

  /**
   * Returns all annotation targets for the given class in declaration order.
   */
  List<AnnotationTarget> scan(Class<?> clazz) {
    LinkedHashMap<String, AnnotationTarget> targets = new LinkedHashMap<>();

    // Pass 1: method annotations
    for (Method method : clazz.getMethods()) {
      if (method.getAnnotation(Field.class) != null || method.getAnnotation(Fields.class) != null) {
        targets.put(stripMethodPrefix(method.getName()), AnnotationTarget.ofMethod(method));
      }
    }

    // Pass 2: field annotations — walk class hierarchy
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      for (java.lang.reflect.Field javaField : current.getDeclaredFields()) {
        if (javaField.getAnnotation(Field.class) == null && javaField.getAnnotation(Fields.class) == null) {
          continue;
        }
        Method getter = findGetter(clazz, javaField);
        String key = stripMethodPrefix(getter.getName());
        if (targets.containsKey(key)) {
          LOG.error("Configuration mismatch: @Field annotation found on both field '{}' and its getter method '{}' in class '{}'. The field annotation will be used.",
              javaField.getName(), getter.getName(), clazz.getName());
        }
        targets.put(key, AnnotationTarget.ofField(getter, javaField));
      }
      current = current.getSuperclass();
    }

    return new ArrayList<>(targets.values());
  }

  private Method findGetter(Class<?> clazz, java.lang.reflect.Field field) {
    String name = field.getName();
    String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    try {
      return clazz.getMethod("get" + cap);
    } catch (NoSuchMethodException e) {
      try {
        return clazz.getMethod("is" + cap);
      } catch (NoSuchMethodException e2) {
        throw new FixedFormatException(format("No getter found for field '%s' in class %s. Expected 'get%s()' or 'is%s()'.", name, clazz.getName(), cap, cap));
      }
    }
  }

  String stripMethodPrefix(String name) {
    if (name.startsWith("get") || name.startsWith("set")) {
      return name.substring(3);
    } else if (name.startsWith("is")) {
      return name.substring(2);
    } else {
      return name;
    }
  }
}
