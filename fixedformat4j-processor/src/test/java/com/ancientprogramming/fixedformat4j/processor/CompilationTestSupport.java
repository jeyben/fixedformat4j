package com.ancientprogramming.fixedformat4j.processor;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Compiles an in-memory Java source with {@link FixedFormatProcessor} attached and returns the
 * diagnostics it produced. Uses {@code -proc:only} so no class files are written.
 */
final class CompilationTestSupport {

  private CompilationTestSupport() {
  }

  static List<Diagnostic<? extends JavaFileObject>> compile(String className, String source) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, null, StandardCharsets.UTF_8)) {
      JavaCompiler.CompilationTask task = compiler.getTask(
          new PrintWriter(Writer.nullWriter()),
          fileManager,
          collector,
          List.of("-proc:only", "-classpath", System.getProperty("java.class.path")),
          null,
          List.of(new StringSource(className, source)));
      task.setProcessors(List.of(new FixedFormatProcessor()));
      task.call();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return collector.getDiagnostics();
  }

  static List<Diagnostic<? extends JavaFileObject>> compileErrors(String className, String source) {
    return compile(className, source).stream()
        .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
        .collect(Collectors.toList());
  }

  static List<String> errorMessages(String className, String source) {
    return compileErrors(className, source).stream()
        .map(diagnostic -> diagnostic.getMessage(null))
        .collect(Collectors.toList());
  }

  private static final class StringSource extends SimpleJavaFileObject {

    private final String source;

    StringSource(String className, String source) {
      super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return source;
    }
  }
}
