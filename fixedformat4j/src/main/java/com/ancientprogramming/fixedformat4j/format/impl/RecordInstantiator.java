package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.lang.reflect.Constructor;

import static java.lang.String.format;

/**
 * Creates instances of {@code @Record}-annotated classes via reflection,
 * including support for static nested classes and non-static inner classes.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.5.1
 */
class RecordInstantiator {

  <T> T instantiate(Class<T> fixedFormatRecordClass) {
    try {
      Constructor<T> constructor = fixedFormatRecordClass.getDeclaredConstructor();
      return constructor.newInstance();
    } catch (NoSuchMethodException e) {
      Class<?> declaringClass = fixedFormatRecordClass.getDeclaringClass();
      if (declaringClass != null) {
        return instantiateInnerClass(fixedFormatRecordClass, declaringClass, e);
      }
      throw new FixedFormatException(format(
          "%s is missing a default constructor which is nessesary to be loaded through %s",
          fixedFormatRecordClass.getName(), getClass().getName()));
    } catch (Exception e) {
      throw new FixedFormatException(format("unable to create instance of %s", fixedFormatRecordClass.getName()), e);
    }
  }

  private <T> T instantiateInnerClass(Class<T> innerClass, Class<?> declaringClass, Exception outerException) {
    Object declaringClassInstance;
    try {
      Constructor<?> declaringClassConstructor = declaringClass.getDeclaredConstructor();
      declaringClassInstance = declaringClassConstructor.newInstance();
    } catch (NoSuchMethodException dex) {
      throw new FixedFormatException(format(
          "Trying to create instance of innerclass %s, but the declaring class %s is missing a default constructor which is nessesary to be loaded through %s",
          innerClass.getName(), declaringClass.getName(), getClass().getName()));
    } catch (Exception de) {
      throw new FixedFormatException(format(
          "unable to create instance of declaring class %s, which is needed to instansiate %s",
          declaringClass.getName(), innerClass.getName()), outerException);
    }

    try {
      Constructor<T> constructor = innerClass.getDeclaredConstructor(declaringClass);
      return constructor.newInstance(declaringClassInstance);
    } catch (NoSuchMethodException ex) {
      throw new FixedFormatException(format(
          "%s is missing a default constructor which is nessesary to be loaded through %s",
          innerClass.getName(), getClass().getName()));
    } catch (Exception ex) {
      throw new FixedFormatException(format("unable to create instance of %s", innerClass.getName()), outerException);
    }
  }
}
