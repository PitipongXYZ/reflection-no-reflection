package org.reflection_no_reflection.generator.module;

import com.google.common.base.Joiner;
import com.google.testing.compile.CompileTester;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;
import org.reflection_no_reflection.generator.Generator;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static org.truth0.Truth.ASSERT;

public class ModuleTest {

    protected javax.annotation.processing.Processor processor;
    protected JavaFileObject javaSourceCode;
    protected JavaFileObject generatedSourceCode;

    @Before
    public void setup() {
        processor = new Generator();
        javaSourceCode = null;
        generatedSourceCode = null;
    }

    @Test
    public void test() {
        javaSourceCode("test.Foo", //
                       "package test;", //
                       "public class Foo {",//
                       "@javax.inject.Inject public String s;", //
                       "@javax.inject.Inject public void m() {}", //
                       "@javax.inject.Inject public int n() {return 4;}", //
                       "}" //
        );

        //generatedSourceCode("org.reflection_no_reflection.generator.sample.gen.ModuleImpl",
        //                    "package org.reflection_no_reflection.generator.sample.gen;", //
        //                        "", //
        //                        "import java.lang.Override;", //
        //                        "import java.util.ArrayList;", //
        //                        "import java.util.HashMap;", //
        //                        "import java.util.HashSet;", //
        //                        "import java.util.List;", //
        //                        "import java.util.Map;", //
        //                        "import java.util.Set;", //
        //                        "import org.reflection_no_reflection.Annotation;", //
        //                        "import org.reflection_no_reflection.Class;", //
        //                        "import org.reflection_no_reflection.Field;", //
        //                        "import org.reflection_no_reflection.runtime.Module;", //
        //                        "import test.Foo$$Reflector;", //
        //                        "", //
        //                        "public final class ModuleImpl implements Module {", //
        //                        "  private Set<Class> classSet = new HashSet<>();", //
        //                        "", //
        //                        "  private Map<Class<? extends Annotation>, Set<Class>> mapOfAnnotationTypeToClassesContainingAnnotation = new HashMap<>();", //
        //                        "", //
        //                        "  public ModuleImpl() {", //
        //                        "    Class c0 = Class.forNameSafe(\"test.Foo\");", //
        //                        "    classSet.add(c0);", //
        //                        "    Field f0 = new Field(\"s\",Class.forNameSafe(\"java.lang.String\"),c0,1,null);", //
        //                        "    c0.addField(f0);", //
        //                        "    {", //
        //                        "    int indexAnnotation = 0;", //
        //                        "    List annotationImplTab = new ArrayList(1);", //
        //                        "    Class a0 = Class.forNameSafe(\"javax.inject.Inject\");", //
        //                        "    a0.setModifiers(8192);", //
        //                        "    classSet.add(a0);", //
        //                        "    annotationImplTab.add(new Inject$$Impl());", //
        //                        "    indexAnnotation++;", //
        //                        "    f0.setAnnotationImplList(annotationImplTab);", //
        //                        "    }", //
        //                        "    c0.setReflector(new Foo$$Reflector());", //
        //                        "    c0.setModifiers(0);", //
        //                        "", //
        //                        "    Set<Class> s0 = new HashSet();", //
        //                        "    s0.add(Class.forNameSafe(\"test.Foo\"));", //
        //                        "    mapOfAnnotationTypeToClassesContainingAnnotation.put((Class<? extends Annotation>) Class.forNameSafe(\"javax.inject.Inject\"),s0);", //
        //                        "", //
        //                        "  }", //
        //                        "", //
        //                        "  @Override", //
        //                        "  public Set<Class> getClassSet() {", //
        //                        "    return classSet;", //
        //                        "  }", //
        //                        "", //
        //                        "  public Map<Class<? extends Annotation>, Set<Class>> getMapOfAnnotationTypeToClassesContainingAnnotation() {", //
        //                        "    return mapOfAnnotationTypeToClassesContainingAnnotation;", //
        //                        "  }", //
        //                        "}");

        //assertJavaSourceCompileWithoutErrorAndGenerateExpectedSource();
        assertJavaSourceCompileWithoutError();
    }

    protected void javaSourceCode(String fullyQualifiedName, String... source) {
        javaSourceCode = JavaFileObjects.forSourceString(fullyQualifiedName, Joiner.on('\n').join(source));
    }

    protected void generatedSourceCode(String fullyQualifiedName, String... source) {
        generatedSourceCode = JavaFileObjects.forSourceString(fullyQualifiedName, Joiner.on('\n').join(source));
    }

    protected Iterable<? extends javax.annotation.processing.Processor> rnrProcessors() {
        return Arrays.asList(processor);
    }

    protected CompileTester.SuccessfulCompilationClause assertJavaSourceCompileWithoutError() {
        return ASSERT.about(javaSource())
            .that(javaSourceCode)
            .processedWith(rnrProcessors())
            .compilesWithoutError();
    }

    protected CompileTester.SuccessfulCompilationClause assertJavaSourceCompileWithoutErrorAndGenerateExpectedSource() {
        return assertJavaSourceCompileWithoutError()
            .and()
            .generatesSources(generatedSourceCode);
    }
}
