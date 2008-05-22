/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.record;

import com.ancientprogramming.fixedformat4j.annotation.*;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import javassist.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * todo: delete when logic is copied to manager
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class RecordProxyCreator {

  private static final Log LOG = LogFactory.getLog(RecordProxyCreator.class);

  public static final String CONCREATE_CLASS_PREFIX = "_Concrete";
  
  <T extends Record> Class<T> createProxy(Class<T> templateClass) throws Exception {
    ClassPool cp = ClassPool.getDefault();
    //Insert the class into the classpool
    cp.insertClassPath(new ClassClassPath(templateClass));
    CtClass abstractClass = cp.get(templateClass.getName());
    CtClass concreteClass = cp.makeClass(templateClass.getName() + CONCREATE_CLASS_PREFIX);
    concreteClass.setSuperclass(abstractClass);

    CtField newField = CtField.make("private java.lang.StringBuffer record;", concreteClass);
    concreteClass.addField(newField);

    CtMethod getRecordMethod = CtMethod.make("public java.lang.String export() { return record.toString();};", concreteClass);
    concreteClass.addMethod(getRecordMethod);

    //set constructor
    CtConstructor constructor = new CtConstructor(new CtClass[]{cp.get("java.lang.StringBuffer")}, concreteClass);
    constructor.setBody("{this.record = $1;}");

    if (LOG.isInfoEnabled()) {
      LOG.info("creating constructor: " + constructor);
    }
    concreteClass.addConstructor(constructor);

    CtField[] fields = abstractClass.getDeclaredFields();
    for (CtField field : fields) {
      Object[] annotations = field.getAvailableAnnotations();
      if (!ArrayUtils.isEmpty(annotations)) {
        for (Object annotation : annotations) {
          if (annotation instanceof Field) {
            //try to lookup FixedFormatPattern annotation if such exists
             FixedFormatPattern patternAnno = findPatternAnnotation(annotations);
             FixedFormatBoolean booleanAnno = findBooleanAnnotation(annotations);
             FixedFormatDecimal decimalAnno = findDecimalAnnotation(annotations);

            concreteClass.addMethod(createSetterMethod(concreteClass, "set", field, field.getType(), (Field) annotation, patternAnno, booleanAnno, decimalAnno));
            concreteClass.addMethod(createGetterMethod(concreteClass, "get", field, field.getType(), (Field) annotation, patternAnno, booleanAnno, decimalAnno));
          }
        }
      }
    }

    //implement abstract Field methods
    CtMethod[] methods = abstractClass.getDeclaredMethods();
    for (CtMethod method : methods) {
      Object[] annotations = method.getAvailableAnnotations();
      if (!ArrayUtils.isEmpty(annotations)) {
        for (Object annotation : annotations) {
          CtMethod newMethod;
          if (annotation instanceof Field) {
            if (!Modifier.isAbstract(method.getModifiers())) {
              if (LOG.isDebugEnabled()) {
                LOG.warn("Method[" + abstractClass.getName() + "." + method.getName() + "] is non abstract and marked as[" + Field.class.getSimpleName() + "]. The body current body will be deleted!");
              }
            }
            //try to lookup FixedFormatPattern annotation if such exists
            FixedFormatPattern patternAnno = findPatternAnnotation(annotations);
            FixedFormatBoolean booleanAnno = findBooleanAnnotation(annotations);
            FixedFormatDecimal decimalAnno = findDecimalAnnotation(annotations);

            //ok to implement method
            if (isSetter(method)) {
              newMethod = createSetterMethod(concreteClass, null, method, method.getParameterTypes()[0], (Field) annotation, patternAnno, booleanAnno, decimalAnno);
            } else if (isGetter(method)) {
              newMethod = createGetterMethod(concreteClass, null, method, method.getReturnType(), (Field) annotation, patternAnno, booleanAnno, decimalAnno);
            } else {
              throw new RecordFactoryException("Method[" + abstractClass.getName() + "." + method.getName() + "] marked as [" + Field.class.getSimpleName() + "] is not a setter or getter method");
            }
            concreteClass.addMethod(newMethod);
          }
        }
      }
    }
    return concreteClass.toClass();
  }

  private FixedFormatPattern findPatternAnnotation(Object[] annotations) {
    FixedFormatPattern result = null;
    for (Object anno : annotations) {
      if (anno instanceof FixedFormatPattern) {
        result = (FixedFormatPattern) anno;
      }
    }
    return result;
  }

  private FixedFormatBoolean findBooleanAnnotation(Object[] annotations) {
    FixedFormatBoolean result = null;
    for (Object anno : annotations) {
      if (anno instanceof FixedFormatBoolean) {
        result = (FixedFormatBoolean) anno;
      }
    }
    return result;
  }

  private FixedFormatDecimal findDecimalAnnotation(Object[] annotations) {
    FixedFormatDecimal result = null;
    for (Object anno : annotations) {
      if (anno instanceof FixedFormatDecimal) {
        result = (FixedFormatDecimal) anno;
      }
    }
    return result;
  }

  private CtMethod createSetterMethod(CtClass concreteClass, String memberPrefix, CtMember member, CtClass parameterType, Field annotation, FixedFormatPattern patternAnnotation, FixedFormatBoolean booleanAnnotation, FixedFormatDecimal decimalAnnotation) throws NotFoundException, CannotCompileException {
    String name = memberPrefix != null ? memberPrefix + StringUtils.capitalize(member.getName()) : member.getName();
    CtMethod newMethod = new CtMethod(CtClass.voidType, name, new CtClass[]{parameterType}, concreteClass);
    String fixedFormatDataSrc = getFixedFormatDataSrc(annotation, patternAnnotation, booleanAnnotation, decimalAnnotation);
    String fixedFormatMetadataSrc = getFixedFormatMetadataSrc(parameterType.getName(), annotation);

    String src = "{ " +
        "com.ancientprogramming.fixedformat4j.format.FixedFormatProcessor.setData(record, " +
        fixedFormatDataSrc + ", " +
        fixedFormatMetadataSrc + ", " +
        " $1); }";
    if (LOG.isDebugEnabled()) {
      LOG.debug("src: " + src);
    }
    newMethod.setBody(src);
    if (LOG.isInfoEnabled()) {
      LOG.info("new method: " + newMethod);
    }

    return newMethod;
  }


  private CtMethod createGetterMethod(CtClass concreteClass, String memberPrefix, CtMember member, CtClass returnType, Field annotation, FixedFormatPattern patternAnnotation, FixedFormatBoolean booleanAnnotation, FixedFormatDecimal decimalAnnotation) throws NotFoundException, CannotCompileException {
    String name = memberPrefix != null ? memberPrefix + StringUtils.capitalize(member.getName()) : member.getName();
    CtMethod newMethod = new CtMethod(returnType, name, new CtClass[]{}, concreteClass);
    String fixedFormatDataSrc = getFixedFormatDataSrc(annotation, patternAnnotation, booleanAnnotation, decimalAnnotation);
    String fixedFormatMetadataSrc = getFixedFormatMetadataSrc(returnType.getName(), annotation);

    String src = "{ " +
        " return (" + returnType.getName() + ") com.ancientprogramming.fixedformat4j.format.FixedFormatProcessor.getData(record, " +
        fixedFormatDataSrc + ", " +
        fixedFormatMetadataSrc + "); }";
    if (LOG.isDebugEnabled()) {
      LOG.debug("src: " + src);
    }
    newMethod.setBody(src);

    if (LOG.isInfoEnabled()) {
      LOG.info("new method: " + newMethod);
    }
    return newMethod;
  }

  private String getFixedFormatMetadataSrc(String dataType, Field fieldAnno) throws NotFoundException {
    int offset = fieldAnno.offset();
    Class<? extends FixedFormatter> formatterClass = fieldAnno.formatter();
    String fixedFormatDataSrc = "new com.ancientprogramming.fixedformat4j.format.FixedFormatMetadata(" +
        offset + ", " +
        dataType + ".class , " +
        formatterClass.getName() + ".class)";
    if (LOG.isDebugEnabled()) {
      LOG.debug("FixedFormatMetadata src " + fixedFormatDataSrc);
    }
    return fixedFormatDataSrc;
  }

  private String getFixedFormatDataSrc(Field fieldAnno, FixedFormatPattern patternAnno, FixedFormatBoolean booleanAnno, FixedFormatDecimal decimalAnno) throws NotFoundException {
    int length = fieldAnno.length();
    Align align = fieldAnno.align();
    String paddingChar = "'" + fieldAnno.paddingChar() + "'";
    String patternDataSrc = getPatternDataSrc(patternAnno);
    String booleanDataSrc = getBooleanDataSrc(booleanAnno);
    String decimalDataSrc = getDecimalDataSrc(decimalAnno);
    String fixedFormatDataSrc = "new com.ancientprogramming.fixedformat4j.format.FixedFormatData(" +
        length + ", " +
        align.getClass().getName() + "." + align.name() + ", " +
        paddingChar + ", " +
        patternDataSrc + ", " +
        booleanDataSrc + ", " +
        decimalDataSrc + ")";
    if (LOG.isDebugEnabled()) {
      LOG.debug("FixedFormatData src " + fixedFormatDataSrc);
    }
    return fixedFormatDataSrc;
  }

  private String getPatternDataSrc(FixedFormatPattern anno) {
    String booleanDataSrc = "null";
    if (anno != null) {
      booleanDataSrc = "new com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData(" +
          "\"" + anno.value() + "\")";
    }
    return booleanDataSrc;
  }

  private String getBooleanDataSrc(FixedFormatBoolean anno) {
    String booleanDataSrc = "null";
    if (anno != null) {
      booleanDataSrc = "new com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData(" +
          "\"" + anno.trueValue() + "\", " +
          "\"" + anno.falseValue() + "\")";
    }
    return booleanDataSrc;
  }

  private String getDecimalDataSrc(FixedFormatDecimal anno) {
    String booleanDataSrc = "null";
    if (anno != null) {
      booleanDataSrc = "new com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData(" +
          anno.decimals() + ", " +
          anno.useDecimalDelimiter() + ", " +
          "'" + anno.decimalDelimiter() + "')";
    }
    return booleanDataSrc;
  }

  private boolean isGetter(CtMethod method) {
    //todo: check for not void and zero parameters
    return method.getName().startsWith("get");
  }

  private boolean isSetter(CtMethod method) {
    //todo: check for void and has one and only one parameter
    return method.getName().startsWith("set");
  }

}
