package com.github.taymindis.jdc;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.github.taymindis.jdc.WiredConstant.WIRE_PROXY_PREFIX;
import static com.github.taymindis.jdc.WiredConstant.WIRE_PROXY_SUFFIX;

@SupportedAnnotationTypes("com.github.taymindis.jdc.Wired")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class WiredProcessor extends AbstractProcessor {

    private Trees trees;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (final Element element : roundEnv.getElementsAnnotatedWith(Wired.class)) {
            final TypeElement typeElement = (TypeElement) element;
            if (!hasDefaultConstructor(typeElement) ||
                    isFinalClass(typeElement) || !isSerializable(typeElement)) {
                return true;
            }
            final PackageElement packageElement = (PackageElement) typeElement.getEnclosingElement();
            final String targetClassName = typeElement.getSimpleName().toString();
            final String proxyClassName = WIRE_PROXY_PREFIX + typeElement.getSimpleName() + WIRE_PROXY_SUFFIX;

            WireProxyGenerator wireProxyGenerator =
                    new WireProxyGenerator(packageElement.getQualifiedName().toString(),
                            targetClassName, proxyClassName);
            for (ExecutableElement executableElement :
                    ElementFilter.methodsIn(element.getEnclosedElements())) {

//                if (executableElement.getAnnotation(Mandatory.class) != null) {
//                    mandatoryFields.add(fieldName);
//                }

//                methodTree.getParameters().get(0).getType().accept()
//                executableElement.getParameters().get(1).asType().toString()
//                executableElement.getParameters().get(0).asType().toString()
//                executableElement.getParameters().get(1).asType();


                if (executableElement.getAnnotation(SkipWire.class) != null) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.NOTE, String.format("Method %s skip wiring", executableElement.getSimpleName()));
                    continue;
                }

                if (executableElement.getReturnType().getKind().isPrimitive()) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR, String.format("Method %s return primitive type is not allowed, please change to Object type", executableElement.getSimpleName()));
                    return true;
                }


                TypeMirror returnType = executableElement.getReturnType();
                if(returnType instanceof WildcardType) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR, String.format("Wildcard return type is not support, please @SkipWire it", executableElement.getSimpleName()));
                    return true;
                }

                if(returnType instanceof TypeVariable) {
                    if(((TypeVariable) returnType).getLowerBound().getKind().
                            compareTo(TypeKind.NULL) != 0){
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR, String.format("Lower Bound return type is not support, please @SkipWire it", executableElement.getSimpleName()));
                        return true;
                    }
                }

                MethodScanner methodScanner = new MethodScanner();
                MethodTree methodTree = methodScanner.scan(executableElement, this.trees);

                WireProxyGenerator.Method method = new WireProxyGenerator.Method(executableElement, methodTree);
                for (VariableElement v : executableElement.getParameters()) {
                    method.addParameter(v.asType().toString(), v.getSimpleName().toString());
                }

                wireProxyGenerator.addMethod(method);
            }


            try {
                final JavaFileObject fileObject = processingEnv.getFiler().createSourceFile(
                        packageElement.getQualifiedName() + "." + proxyClassName);

                try (Writer writter = fileObject.openWriter()) {
                    writter.append(wireProxyGenerator.compileToString(processingEnv));
                }
            } catch (final IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
            }
        }
        return true;
    }


    private static class MethodScanner extends TreePathScanner<List<MethodTree>, Trees> {
        private List<MethodTree> methodTrees = new ArrayList<>();

        public MethodTree scan(ExecutableElement methodElement, Trees trees) {
            assert methodElement.getKind() == ElementKind.METHOD;

            List<MethodTree> methodTrees = this.scan(trees.getPath(methodElement), trees);
            assert methodTrees.size() == 1;

            return methodTrees.get(0);
        }

        @Override
        public List<MethodTree> scan(TreePath treePath, Trees trees) {
            super.scan(treePath, trees);
            return this.methodTrees;
        }

        @Override
        public List<MethodTree> visitMethod(MethodTree methodTree, Trees trees) {
            this.methodTrees.add(methodTree);
            return super.visitMethod(methodTree, trees);
        }
    }

    private boolean hasDefaultConstructor(TypeElement type) {
        for (ExecutableElement cons :
                ElementFilter.constructorsIn(type.getEnclosedElements())) {
            if (cons.getParameters().isEmpty())
                return true;
        }

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR, String.format("%s is missing a default constructor", type.getSimpleName()),
                type);
        return false;
    }

    private boolean isFinalClass(TypeElement type) {
        if (type.getModifiers().contains(Modifier.FINAL)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, String.format("%s is a final class, final class cannot be wired", type.getSimpleName()),
                    type);
            return true;
        }
        return false;
    }

    private boolean isSerializable(TypeElement type) {
        TypeMirror serializable = elementUtils.getTypeElement("java.io.Serializable").asType();
        TypeMirror wiredCommandType = elementUtils.getTypeElement(WiredCommand.class.getName()).asType();

        if (!typeUtils.isAssignable(type.asType(), serializable) &&
            !typeUtils.isAssignable(type.asType(), wiredCommandType)
        ) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, String.format("%s should be serializable " +
                            "or should have wired command", type.getSimpleName()),
                    type);
            return false;
        }
        return true;
    }
}