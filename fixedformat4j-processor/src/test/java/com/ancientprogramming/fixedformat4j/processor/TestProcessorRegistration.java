package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.compile;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The processor must be discoverable via the standard {@code ServiceLoader} mechanism (so adding
 * the artifact to a build's processor path enables it without configuration) and must stay
 * silent on a correctly configured record class.
 */
class TestProcessorRegistration {

  @Test
  void processorIsDiscoverableViaServiceLoader() {
    boolean found = StreamSupport.stream(ServiceLoader.load(Processor.class).spliterator(), false)
        .anyMatch(processor -> processor instanceof FixedFormatProcessor);
    assertTrue(found, "FixedFormatProcessor must be registered in META-INF/services");
  }

  @Test
  void validRecordClassCompilesWithoutDiagnostics() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("ValidRecord",
        "import com.ancientprogramming.fixedformat4j.annotation.Field;\n"
            + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n"
            + "@Record(length = 20)\n"
            + "public class ValidRecord {\n"
            + "  @Field(offset = 1, length = 10)\n"
            + "  public String getName() { return null; }\n"
            + "  @Field(offset = 11, length = 10)\n"
            + "  public Integer getAmount() { return null; }\n"
            + "}\n");
    assertTrue(diagnostics.isEmpty(), "expected no diagnostics but got: " + diagnostics);
  }
}
