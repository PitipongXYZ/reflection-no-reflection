package org.reflection_no_reflection.generator.module;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.reflection_no_reflection.Annotation;
import org.reflection_no_reflection.Class;
import org.reflection_no_reflection.Field;
import org.reflection_no_reflection.Method;
import org.reflection_no_reflection.visit.ClassPoolVisitor;

/**
 * @author SNI.
 */
public class ModuleDumperClassPoolVisitor implements ClassPoolVisitor {

    private JavaFile javaFile;
    private List<Class<?>> classList = new ArrayList<>();
    private Map<Class<? extends Annotation>, Set<Class<?>>> mapAnnotationTypeToClassContainingAnnotation = new HashMap<>();
    private final TypeSpec.Builder moduleType;
    public static final ClassName MODULE_TYPE_NAME = ClassName.get("org.reflection_no_reflection.runtime", "Module");
    public static final ClassName CLASS_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Class");
    public static final ClassName FIELD_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Field");
    public static final ClassName METHOD_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Method");
    public static final ClassName ANNOTATION_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Annotation");
    public static final ClassName LIST_TYPE_NAME = ClassName.get("java.util", "List");
    public static final TypeName ARRAY_OF_CLASSES_TYPE_NAME = ArrayTypeName.get(Class.class);
    public static final ClassName ARRAYLIST_TYPE_NAME = ClassName.get("java.util", "ArrayList");

    private String targetPackageName;

    public ModuleDumperClassPoolVisitor() {

        //build class list
        ClassName setTypeName = ClassName.get("java.util", "Set");
        ClassName hashSetTypeName = ClassName.get("java.util", "HashSet");
        TypeName setOfClassesTypeName = ParameterizedTypeName.get(setTypeName, CLASS_TYPE_NAME);

        FieldSpec classListField = FieldSpec.builder(setOfClassesTypeName, "classSet", Modifier.PRIVATE)
            .initializer("new $T<>()", hashSetTypeName)
            .build();

        MethodSpec getClassListMethod = MethodSpec.methodBuilder("getClassSet")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(setOfClassesTypeName)
            .addStatement("return $L", "classSet")
            .build();

        //build map of annotation type names to class set
        ClassName mapTypeName = ClassName.get("java.util", "Map");
        ClassName hashMapTypeName = ClassName.get("java.util", "HashMap");
        WildcardTypeName subClassOfAnnotationTypeName = WildcardTypeName.subtypeOf(ANNOTATION_TYPE_NAME);
        TypeName classesOfAnnotationTypeName = ParameterizedTypeName.get(CLASS_TYPE_NAME, subClassOfAnnotationTypeName);
        TypeName mapOfAnnotationTypeToClassesContainingAnnotationTypeName = ParameterizedTypeName.get(mapTypeName, classesOfAnnotationTypeName, setOfClassesTypeName);

        FieldSpec mapOfAnnotationTypeToClassesContainingAnnotationField = FieldSpec.builder(mapOfAnnotationTypeToClassesContainingAnnotationTypeName, "mapOfAnnotationTypeToClassesContainingAnnotation", Modifier.PRIVATE)
            .initializer("new $T<>()", hashMapTypeName)
            .build();

        MethodSpec getMapOfAnnotationTypeToClassesContainingAnnotationMethod = MethodSpec.methodBuilder("getMapOfAnnotationTypeToClassesContainingAnnotation")
            .addModifiers(Modifier.PUBLIC)
            .returns(mapOfAnnotationTypeToClassesContainingAnnotationTypeName)
            .addStatement("return $L", "mapOfAnnotationTypeToClassesContainingAnnotation")
            .build();

        moduleType = TypeSpec.classBuilder("ModuleImpl")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(MODULE_TYPE_NAME)
            .addField(classListField)
            .addField(mapOfAnnotationTypeToClassesContainingAnnotationField)
            .addMethod(getClassListMethod)
            .addMethod(getMapOfAnnotationTypeToClassesContainingAnnotationMethod);
    }

    @Override public <T> void visit(Class<T> clazz) {
        classList.add(clazz);
    }

    @Override public void visit(Field field) {

    }

    @Override public void visit(Method method) {

    }

    @Override public void visit(Annotation annotation) {

    }

    @Override public void visitAnnotationMethod(Annotation annotation, Method method) {

    }

    @Override public void endVisit(org.reflection_no_reflection.Class aClass) {

    }

    @Override public void endVisit(Annotation annotation) {

    }

    public Map<Class<? extends Annotation>, Set<Class<?>>> getMapAnnotationTypeToClassContainingAnnotation() {
        return mapAnnotationTypeToClassContainingAnnotation;
    }

    public JavaFile getJavaFile() {
        MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder();
        constructorSpecBuilder.addModifiers(Modifier.PUBLIC);
        int annotationCounter = 0;
        int classCounter = 0;
        int fieldCounter = 0;
        int methodCounter = 0;
        //fill class list
        for (Class clazz : classList) {
            final String clazzName = clazz.getName();
            constructorSpecBuilder.addStatement("$T c$L = Class.forNameSafe($S)", CLASS_TYPE_NAME, classCounter, clazzName);
            constructorSpecBuilder.addStatement("classSet.add(c$L)", classCounter);
            for (Field field : clazz.getFields()) {
                constructorSpecBuilder.addStatement("$T f$L = new $T($S,Class.forNameSafe($S),c$L,$L,null)", FIELD_TYPE_NAME, fieldCounter, FIELD_TYPE_NAME, field.getName(), field.getType().getName(), classCounter, field.getModifiers());
                constructorSpecBuilder.addStatement("c$L.addField(f$L)", classCounter, fieldCounter);

                if (field.getDeclaredAnnotations().length != 0) {
                    for (Annotation annotation : field.getDeclaredAnnotations()) {
                        constructorSpecBuilder.addCode("{\n");
                        constructorSpecBuilder.addStatement("int indexAnnotation = 0");
                        constructorSpecBuilder.addStatement("$T annotationImplTab = new $T($L)", LIST_TYPE_NAME, ARRAYLIST_TYPE_NAME, field.getDeclaredAnnotations().length);
                        constructorSpecBuilder.addStatement("$T a$L = Class.forNameSafe($S)", CLASS_TYPE_NAME, annotationCounter, annotation.annotationType().getName());
                        constructorSpecBuilder.addStatement("a$L.setModifiers($L)", annotationCounter, annotation.annotationType().getModifiers());
                        constructorSpecBuilder.addStatement("classSet.add(a$L)", annotationCounter);
                        annotationCounter++;
                        constructorSpecBuilder.addStatement("annotationImplTab.add(new $T())", ClassName.get(targetPackageName, annotation.annotationType().getSimpleName() + "$$Impl"));
                        constructorSpecBuilder.addStatement("indexAnnotation++");
                        constructorSpecBuilder.addStatement("f$L.setAnnotationImplList(annotationImplTab)", fieldCounter);
                        constructorSpecBuilder.addCode("}\n");
                    }
                }
                fieldCounter++;
            }

            for (Object methodObj : clazz.getMethods()) {
                Method method = (Method) methodObj;

                //params
                constructorSpecBuilder.addStatement("$T[] paramTypeTab$L = null", ARRAY_OF_CLASSES_TYPE_NAME, methodCounter);
                if (method.getParameterTypes().length != 0) {
                    constructorSpecBuilder.addStatement("paramTypeTab$L = new $T[$L]", ARRAY_OF_CLASSES_TYPE_NAME, methodCounter, ARRAY_OF_CLASSES_TYPE_NAME, method.getParameterTypes().length);
                    int indexParam = 0;
                    for (Class<?> paramClass : method.getParameterTypes()) {
                        constructorSpecBuilder.addStatement("paramTypeTab$L[$L] = Class.forNameSafe($S)", methodCounter, indexParam++, paramClass.getName());
                        indexParam++;
                    }
                }

                //exceptions
                constructorSpecBuilder.addStatement("$T[] exceptionTypeTab$L = null", ARRAY_OF_CLASSES_TYPE_NAME, methodCounter);
                if (method.getParameterTypes().length != 0) {
                    constructorSpecBuilder.addStatement("exceptionTypeTab$L = new $T[$L]", ARRAY_OF_CLASSES_TYPE_NAME, methodCounter, ARRAY_OF_CLASSES_TYPE_NAME, method.getExceptionTypes().length);
                    int indexException = 0;
                    for (Class<?> exceptionClass : method.getExceptionTypes()) {
                        constructorSpecBuilder.addStatement("exceptionTypeTab$L[$L] = Class.forNameSafe($S)", methodCounter, indexException++, exceptionClass.getName());
                        indexException++;
                    }
                }

                constructorSpecBuilder.addStatement("$T m$L = new $T(Class.forNameSafe($S),$S,paramTypeTab$L,Class.forNameSafe($S),exceptionTypeTab$L, $L)",
                                                    METHOD_TYPE_NAME,
                                                    fieldCounter,
                                                    METHOD_TYPE_NAME,
                                                    method.getDeclaringClass().getName(),
                                                    method.getName(),
                                                    methodCounter,
                                                    method.getReturnType().getName(),
                                                    methodCounter,
                                                    method.getModifiers());
                constructorSpecBuilder.addStatement("c$L.addMethod(m$L)", classCounter, fieldCounter);

                if (method.getDeclaredAnnotations().length != 0) {
                    for (Annotation annotation : method.getDeclaredAnnotations()) {
                        constructorSpecBuilder.addCode("{\n");
                        constructorSpecBuilder.addStatement("int indexAnnotation = 0");
                        constructorSpecBuilder.addStatement("$T annotationImplTab = new $T($L)", LIST_TYPE_NAME, ARRAYLIST_TYPE_NAME, method.getDeclaredAnnotations().length);
                        constructorSpecBuilder.addStatement("$T a$L = Class.forNameSafe($S)", CLASS_TYPE_NAME, annotationCounter, annotation.annotationType().getName());
                        constructorSpecBuilder.addStatement("a$L.setModifiers($L)", annotationCounter, annotation.annotationType().getModifiers());
                        constructorSpecBuilder.addStatement("classSet.add(a$L)", annotationCounter);
                        annotationCounter++;
                        constructorSpecBuilder.addStatement("annotationImplTab.add(new $T())", ClassName.get(targetPackageName, annotation.annotationType().getSimpleName() + "$$Impl"));
                        constructorSpecBuilder.addStatement("indexAnnotation++");
                        constructorSpecBuilder.addStatement("m$L.setAnnotationImplList(annotationImplTab)", fieldCounter);
                        constructorSpecBuilder.addCode("}\n");
                    }
                }

                methodCounter++;
            }
            TypeName reflectorTypeName = ClassName.get(clazzName.substring(0, clazzName.lastIndexOf('.')), clazz.getSimpleName() + "$$Reflector");
            //TODO add all protected java & android packages
            //TODO the test should be done at the introspector level, there is a dependency
            //TODO avoid dependency: introduce 3rd party
            if (!clazz.getName().startsWith("java")) {
                constructorSpecBuilder.addStatement("c$L.setReflector(new $T())", classCounter, reflectorTypeName);
            }
            constructorSpecBuilder.addStatement("c$L.setModifiers($L)", classCounter, clazz.getModifiers());

            classCounter++;
            constructorSpecBuilder.addCode("\n");
        }

        //fill class list
        ClassName setTypeName = ClassName.get("java.util", "Set");
        TypeName setOfClassesTypeName = ParameterizedTypeName.get(setTypeName, CLASS_TYPE_NAME);
        ClassName hashSetTypeName = ClassName.get("java.util", "HashSet");
        WildcardTypeName subClassOfAnnotationTypeName = WildcardTypeName.subtypeOf(ANNOTATION_TYPE_NAME);
        TypeName classesOfAnnotationTypeName = ParameterizedTypeName.get(CLASS_TYPE_NAME, subClassOfAnnotationTypeName);

        classCounter = 0;
        for (Map.Entry<Class<? extends Annotation>, Set<Class<?>>> entry : mapAnnotationTypeToClassContainingAnnotation.entrySet()) {
            constructorSpecBuilder.addStatement("$T s$L = new $T()", setOfClassesTypeName, classCounter, hashSetTypeName);
            for (Class<?> clazz : entry.getValue()) {
                constructorSpecBuilder.addStatement("s$L.add(Class.forNameSafe($S))", classCounter, clazz.getName());
            }
            String annotationTypeName = entry.getKey().getName();
            constructorSpecBuilder.addStatement("mapOfAnnotationTypeToClassesContainingAnnotation.put(($T) Class.forNameSafe($S),s$L)", classesOfAnnotationTypeName, annotationTypeName, classCounter);
            constructorSpecBuilder.addCode("\n");
            classCounter++;
        }

        MethodSpec constructorSpec = constructorSpecBuilder.build();
        moduleType.addMethod(constructorSpec);

        javaFile = JavaFile.builder(targetPackageName, moduleType.build()).build();

        return javaFile;
    }

    public void setTargetPackageName(String targetPackageName) {
        this.targetPackageName = targetPackageName;
    }
}
