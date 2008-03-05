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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Constructor;


/**
 * todo: comment needed
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class RecordFactory {

  private static final Log LOG = LogFactory.getLog(RecordFactory.class);

  private RecordFactory() {
    //not instanciatable
  }

  public static synchronized Record createInstance(Class<? extends Record> clazz, StringBuffer recordContent) throws Exception {
    Class<? extends Record> recordClass;
    String classname = clazz.getName() + RecordProxyCreator.CONCREATE_CLASS_PREFIX;
    try {
      recordClass = (Class<? extends Record>) RecordFactory.class.getClassLoader().loadClass(classname);
    } catch (ClassNotFoundException e) {
      recordClass = new RecordProxyCreator().createProxy(clazz);
      recordClass = (Class<? extends Record>) RecordFactory.class.getClassLoader().loadClass(classname);
    }
    return createRecord(recordClass, recordContent);
  }

  public static synchronized Record createInstance(Class<? extends Record> clazz) throws Exception {
    return createInstance(clazz, null);

  }



  static Record createRecord(Class<? extends Record> c, StringBuffer recordContent) throws Exception {
    if (recordContent == null) {
      recordContent = new StringBuffer("");
    }
    Constructor<? extends Record> constructor = c.getConstructor(StringBuffer.class);
    return constructor.newInstance(recordContent);
  }

}
