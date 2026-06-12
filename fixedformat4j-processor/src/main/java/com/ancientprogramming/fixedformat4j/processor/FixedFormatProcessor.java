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
package com.ancientprogramming.fixedformat4j.processor;

import com.ancientprogramming.fixedformat4j.annotation.Record;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Optional compile-time validator for {@link Record @Record} classes.
 *
 * <p>Replicates the statically decidable subset of the runtime annotation validation performed
 * by {@code FixedFormatManagerImpl} on first use of a record class, so misconfigurations surface
 * as {@code javac} errors instead of runtime {@code FixedFormatException}s. The runtime
 * validation is unchanged and remains the safety net for classes compiled without this
 * processor on the processor path.
 *
 * <p>This processor only reports diagnostics. It claims no annotations, generates no code, and
 * adds nothing to the runtime classpath of the consuming project.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
@SupportedAnnotationTypes("com.ancientprogramming.fixedformat4j.annotation.Record")
public class FixedFormatProcessor extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(Record.class)) {
      if (element instanceof TypeElement) {
        new RecordValidator(processingEnv).validate((TypeElement) element);
      }
    }
    return false;
  }
}
