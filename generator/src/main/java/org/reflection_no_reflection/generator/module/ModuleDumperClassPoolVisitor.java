package org.reflection_no_reflection.generator.module;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.reflection_no_reflection.Annotation;
import org.reflection_no_reflection.Class;
import org.reflection_no_reflection.Constructor;
import org.reflection_no_reflection.Field;
import org.reflection_no_reflection.Invokable;
import org.reflection_no_reflection.Method;
import org.reflection_no_reflection.generator.introspector.IntrospectorDumperClassPoolVisitor;
import org.reflection_no_reflection.generator.introspector.IntrospectorUtil;
import org.reflection_no_reflection.visit.ClassPoolVisitor;

/**
 * @author SNI.
 */
public class ModuleDumperClassPoolVisitor implements ClassPoolVisitor {

    private JavaFile javaFile;
    private Collection<Class<?>> classList = new HashSet<>();
    private Map<Class<? extends Annotation>, Set<Class<?>>> mapAnnotationTypeToClassContainingAnnotation = new HashMap<>();
    private final TypeSpec.Builder moduleType;
    public static final ClassName MODULE_TYPE_NAME = ClassName.get("org.reflection_no_reflection.runtime", "Module");
    public static final ClassName STRING_TYPE_NAME = ClassName.get("java.lang", "String");
    public static final ClassName CLASS_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Class");
    public static final ClassName FIELD_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Field");
    public static final ClassName METHOD_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Method");
    public static final ClassName CONSTRUCTOR_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Constructor");
    public static final ClassName ANNOTATION_TYPE_NAME = ClassName.get("org.reflection_no_reflection", "Annotation");
    public static final ClassName LIST_TYPE_NAME = ClassName.get("java.util", "List");
    public static final TypeName ARRAY_OF_CLASSES_TYPE_NAME = ArrayTypeName.get(Class.class);
    public static final ClassName ARRAYLIST_TYPE_NAME = ClassName.get("java.util", "ArrayList");

    private String targetPackageName;
    private IntrospectorUtil util = new IntrospectorUtil();
    private IntrospectorDumperClassPoolVisitor introspectorDumperClassPoolVisitor;

    public ModuleDumperClassPoolVisitor(IntrospectorDumperClassPoolVisitor introspectorDumperClassPoolVisitor) {
        this.introspectorDumperClassPoolVisitor = introspectorDumperClassPoolVisitor;

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

        //build map of annotation type names to class name set
        TypeName setOfStringTypeName = ParameterizedTypeName.get(setTypeName, STRING_TYPE_NAME);

        ClassName mapTypeName = ClassName.get("java.util", "Map");
        ClassName hashMapTypeName = ClassName.get("java.util", "HashMap");
        TypeName mapAnnotationNameToNameOfClassesContainingAnnotation = ParameterizedTypeName.get(mapTypeName, STRING_TYPE_NAME, setOfStringTypeName);

        FieldSpec mapOfAnnotationTypeToClassesContainingAnnotationField = FieldSpec.builder(mapAnnotationNameToNameOfClassesContainingAnnotation, "mapAnnotationNameToNameOfClassesContainingAnnotation", Modifier.PRIVATE)
            .initializer("new $T<>()", hashMapTypeName)
            .build();

        MethodSpec getMapOfAnnotationTypeToClassesContainingAnnotationMethod = MethodSpec.methodBuilder("getMapAnnotationNameToNameOfClassesContainingAnnotation")
            .addModifiers(Modifier.PUBLIC)
            .returns(mapAnnotationNameToNameOfClassesContainingAnnotation)
            .addStatement("return $L", "mapAnnotationNameToNameOfClassesContainingAnnotation")
            .build();

        moduleType = TypeSpec.classBuilder("ModuleImpl")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(MODULE_TYPE_NAME)
            .addField(classListField)
            .addField(mapOfAnnotationTypeToClassesContainingAnnotationField)
            .addMethod(getClassListMethod)
            .addMethod(getMapOfAnnotationTypeToClassesContainingAnnotationMethod);
    }

    @Override
    public <T> void visit(Class<T> clazz) {
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
        MethodSpec loadClassMethod = createLoadClassMethod();
        moduleType.addMethod(loadClassMethod);

        //fill class list
        MethodSpec constructorSpec = createConstructor();
        moduleType.addMethod(constructorSpec);

        javaFile = JavaFile.builder(targetPackageName, moduleType.build()).indent("\t").build();
        return javaFile;
    }

    private MethodSpec createConstructor() {
        MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder();
        constructorSpecBuilder.addModifiers(Modifier.PUBLIC);

        ClassName setTypeName = ClassName.get("java.util", "Set");
        TypeName setOfStringTypeName = ParameterizedTypeName.get(setTypeName, STRING_TYPE_NAME);
        ClassName hashSetTypeName = ClassName.get("java.util", "HashSet");

        int annotationClassCounter = 0;
        for (Map.Entry<Class<? extends Annotation>, Set<Class<?>>> entry : mapAnnotationTypeToClassContainingAnnotation.entrySet()) {
            constructorSpecBuilder.addStatement("$T s$L = new $T()", setOfStringTypeName, annotationClassCounter, hashSetTypeName);
            for (Class<?> clazz : entry.getValue()) {
                constructorSpecBuilder.addStatement("s$L.add($S)", annotationClassCounter, clazz.getName());
            }
            String annotationTypeName = entry.getKey().getName();
            constructorSpecBuilder.addStatement("mapAnnotationNameToNameOfClassesContainingAnnotation.put($S,s$L)", annotationTypeName, annotationClassCounter);
            constructorSpecBuilder.addCode("\n");
            annotationClassCounter++;
        }
        return constructorSpecBuilder.build();
    }

    private MethodSpec createLoadClassMethod() {
        MethodSpec.Builder loadClassMethodBuilder = MethodSpec.methodBuilder("loadClass");
        ParameterSpec paramClassName = ParameterSpec.builder(STRING_TYPE_NAME, "className").build();
        loadClassMethodBuilder
            .addModifiers(Modifier.PUBLIC)
            .returns(CLASS_TYPE_NAME)
            .addParameter(paramClassName);

        //fill class list
        loadClassMethodBuilder.beginControlFlow("switch(className)");
        for (Class clazz : classList) {
            String clazzName = clazz.getName();
            final String simpleClazzName = clazz.getSimpleName();
            final int endIndex = clazzName.lastIndexOf('.');
            if (endIndex == -1) {
                continue;
            }
            final String packageName = clazzName.substring(0, endIndex);

            loadClassMethodBuilder.beginControlFlow("case $S:", clazzName);
            loadClassMethodBuilder.addStatement("$T c = Class.forNameSafe($S, true)", CLASS_TYPE_NAME, clazzName);
            loadClassMethodBuilder.addStatement("classSet.add(c)");


            if(clazz.getSuperclass() != null) {
                loadClassMethodBuilder.addStatement("c.setSuperclass(Class.forNameSafe($S))", clazz.getSuperclass().getName());
            }

            if(clazz.getInterfaces()!=null) {
                loadClassMethodBuilder.addStatement("Class[] interfaces = new Class[$L]", clazz.getInterfaces().length);
                loadClassMethodBuilder.addStatement("int indexInterface = 0");
                for (Class interfaceClass : clazz.getInterfaces()) {
                    loadClassMethodBuilder.addStatement("interfaces[indexInterface++] = Class.forNameSafe($S)", interfaceClass.getName());
                }
                loadClassMethodBuilder.addStatement("c.setInterfaces(interfaces)");
            }

            for (Object constructorObj : clazz.getConstructors()) {
                generateConstructor(loadClassMethodBuilder, (Constructor) constructorObj);
            }

            for (Field field : clazz.getFields()) {
                generateField(loadClassMethodBuilder, field);
            }

            for (Object methodObj : clazz.getMethods()) {
                generateMethod(loadClassMethodBuilder, (Method) methodObj);
            }

            doGenerateSetReflector(loadClassMethodBuilder, clazz, simpleClazzName, packageName);
            loadClassMethodBuilder.addStatement("c.setModifiers($L)", clazz.getModifiers());
            loadClassMethodBuilder.addStatement("c.setIsInterface($L)", clazz.isInterface());
            loadClassMethodBuilder.addStatement("return c");
            loadClassMethodBuilder.endControlFlow();
        }

        loadClassMethodBuilder.addStatement("default : return null");

        loadClassMethodBuilder.endControlFlow();

        return loadClassMethodBuilder.build();
    }

    private void doGenerateSetReflector(MethodSpec.Builder loadClassMethodBuilder, Class clazz, String simpleClazzName, String packageName) {//TODO add all protected java & android packages
        //TODO the test should be done at the introspector level, there is a dependency
        //TODO avoid dependency: introduce 3rd party
        if (introspectorDumperClassPoolVisitor.filter(clazz.getName())) {
            TypeName reflectorTypeName = createReflectorTypeName(packageName, simpleClazzName);
            loadClassMethodBuilder.addStatement("c.setReflector(new $T())", reflectorTypeName);
        }
    }

    private ClassName createReflectorTypeName(String packageName, String simpleClassName) {
        return ClassName.get(packageName, simpleClassName + "$$Reflector");
    }

    private void generateMethod(MethodSpec.Builder loadClassMethodBuilder, Method method) {
        loadClassMethodBuilder.beginControlFlow("");

        generateParams(loadClassMethodBuilder, method);

        generateExceptions(loadClassMethodBuilder, method);

        loadClassMethodBuilder.addStatement("$T m = new $T(c,$S,paramTypeTab,Class.forNameSafe($S),exceptionTypeTab, $L)",
                                            METHOD_TYPE_NAME,
                                            METHOD_TYPE_NAME,
                                            method.getName(),
                                            method.getReturnType().getName(),
                                            method.getModifiers());
        loadClassMethodBuilder.addStatement("c.addMethod(m)");

        doGenerateAnnotationsForMember(loadClassMethodBuilder, "m", method.getAnnotations());
        loadClassMethodBuilder.addStatement("m.setIsVarArgs($L)", method.isVarArgs());

        loadClassMethodBuilder.endControlFlow("");
    }

    private void generateConstructor(MethodSpec.Builder loadClassMethodBuilder, Constructor constructor) {
        loadClassMethodBuilder.beginControlFlow("");

        generateParams(loadClassMethodBuilder, constructor);

        generateExceptions(loadClassMethodBuilder, constructor);

        loadClassMethodBuilder.addStatement("$T co = new $T(c,paramTypeTab,exceptionTypeTab, $L)",
                                            CONSTRUCTOR_TYPE_NAME,
                                            CONSTRUCTOR_TYPE_NAME,
                                            constructor.getModifiers());
        loadClassMethodBuilder.addStatement("c.addConstructor(co)");

        doGenerateAnnotationsForMember(loadClassMethodBuilder, "co", constructor.getAnnotations());
        loadClassMethodBuilder.addStatement("co.setIsVarArgs($L)", constructor.isVarArgs());

        loadClassMethodBuilder.endControlFlow("");
    }

    private void generateExceptions(MethodSpec.Builder loadClassMethodBuilder, Invokable invokable) {
        if (invokable.getExceptionTypes().length != 0) {
            loadClassMethodBuilder.addStatement("$T[] exceptionTypeTab = new $T[$L]", ARRAY_OF_CLASSES_TYPE_NAME, ARRAY_OF_CLASSES_TYPE_NAME, invokable.getExceptionTypes().length);
            int indexException = 0;
            for (Class<?> exceptionClass : invokable.getExceptionTypes()) {
                loadClassMethodBuilder.addStatement("exceptionTypeTab[$L] = Class.forNameSafe($S)", indexException, exceptionClass.getName());
                indexException++;
            }
        } else {
            loadClassMethodBuilder.addStatement("$T[] exceptionTypeTab = new $T[0]", ARRAY_OF_CLASSES_TYPE_NAME, ARRAY_OF_CLASSES_TYPE_NAME);
        }
    }

    private void generateParams(MethodSpec.Builder loadClassMethodBuilder, Invokable invokable) {
        if (invokable.getParameterTypes().length != 0) {
            loadClassMethodBuilder.addStatement("$T[] paramTypeTab = new $T[$L]", ARRAY_OF_CLASSES_TYPE_NAME, ARRAY_OF_CLASSES_TYPE_NAME, invokable.getParameterTypes().length);
            int indexParam = 0;
            for (Class<?> paramClass : invokable.getParameterTypes()) {
                //TODO handle generics T[]
                //type is null
                if (paramClass != null) {
                    loadClassMethodBuilder.addStatement("paramTypeTab[$L] = Class.forNameSafe($S)", indexParam, paramClass.getName());
                    indexParam++;
                }
            }
        } else {
            loadClassMethodBuilder.addStatement("$T[] paramTypeTab = new $T[0]", ARRAY_OF_CLASSES_TYPE_NAME, ARRAY_OF_CLASSES_TYPE_NAME);
        }
    }

    private void generateField(MethodSpec.Builder loadClassMethodBuilder, Field field) {
        loadClassMethodBuilder.beginControlFlow("");
        loadClassMethodBuilder.addStatement("$T f = new $T($S,Class.forNameSafe($S),c,$L,null)", FIELD_TYPE_NAME, FIELD_TYPE_NAME, field.getName(), field.getType().getName(), field.getModifiers());
        loadClassMethodBuilder.addStatement("c.addField(f)");

        final String memberInGenCode = "f";
        final java.lang.annotation.Annotation[] declaredAnnotations = field.getAnnotations();
        doGenerateAnnotationsForMember(loadClassMethodBuilder, memberInGenCode, declaredAnnotations);
        loadClassMethodBuilder.endControlFlow();
    }

    private void doGenerateAnnotationsForMember(MethodSpec.Builder loadClassMethodBuilder, String memberInGenCode, java.lang.annotation.Annotation[] declaredAnnotations) {
        if (declaredAnnotations.length != 0) {
            loadClassMethodBuilder.addStatement("$T annotationImplTab = new $T($L)", LIST_TYPE_NAME, ARRAYLIST_TYPE_NAME, declaredAnnotations.length);
            for (java.lang.annotation.Annotation annotation : declaredAnnotations) {
                Annotation rnrAnnotation = (Annotation) annotation;
                loadClassMethodBuilder.beginControlFlow("");
                loadClassMethodBuilder.addStatement("$T a = Class.forNameSafe($S)", CLASS_TYPE_NAME, rnrAnnotation.rnrAnnotationType().getName());
                loadClassMethodBuilder.addStatement("a.setModifiers($L)", rnrAnnotation.rnrAnnotationType().getModifiers());
                loadClassMethodBuilder.addStatement("classSet.add(a)");
                final ClassName annotationImplClassName = ClassName.get(targetPackageName, rnrAnnotation.rnrAnnotationType().getSimpleName() + "$$Impl");
                loadClassMethodBuilder.addStatement("$T aImpl = new $T()", annotationImplClassName, annotationImplClassName);
                for (Method method : rnrAnnotation.getMethods()) {
                    try {
                        final Object value = rnrAnnotation.getValue(method.getName());
                        if (method.getReturnType().getName().equals("java.lang.String")) {
                            loadClassMethodBuilder.addStatement("aImpl.set$L($S)", util.createCapitalizedName(method.getName()), value);
                        } else if (method.getReturnType().getName().equals("java.lang.String[]")) {
                            loadClassMethodBuilder.addStatement("aImpl.set$L(new String[] {$S})", util.createCapitalizedName(method.getName()), value);
                        } else {
                            loadClassMethodBuilder.addStatement("aImpl.set$L($L)", util.createCapitalizedName(method.getName()), value);
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }

                loadClassMethodBuilder.addStatement("annotationImplTab.add(aImpl)");
                loadClassMethodBuilder.endControlFlow();
            }
            loadClassMethodBuilder.addStatement(memberInGenCode + ".setAnnotationImplList(annotationImplTab)");
        }
    }

    public void setTargetPackageName(String targetPackageName) {
        this.targetPackageName = targetPackageName;
    }
}
