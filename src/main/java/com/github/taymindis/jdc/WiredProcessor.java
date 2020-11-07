package com.github.taymindis.jdc;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (final Element element : roundEnv.getElementsAnnotatedWith(Wired.class)) {
            final TypeElement typeElement = ( TypeElement )element;
            if(!hasDefaultConstructor(typeElement)) {
                return true;
            }
            final PackageElement packageElement = ( PackageElement )typeElement.getEnclosingElement();
            final String targetClassName = typeElement.getSimpleName().toString();
            final String proxyClassName = WIRE_PROXY_PREFIX+typeElement.getSimpleName()+WIRE_PROXY_SUFFIX;

            WireProxyGenerator wireProxyGenerator =
                    new WireProxyGenerator(packageElement.getQualifiedName().toString(),
                            targetClassName, proxyClassName);
            for (ExecutableElement executableElement :
                    ElementFilter.methodsIn(element.getEnclosedElements())) {

//                if (executableElement.getAnnotation(Mandatory.class) != null) {
//                    mandatoryFields.add(fieldName);
//                }
                MethodScanner methodScanner = new MethodScanner();
                MethodTree methodTree = methodScanner.scan(executableElement, this.trees);

//                methodTree.getParameters().get(0).getType().accept()
//                executableElement.getParameters().get(1).asType().toString()
//                executableElement.getParameters().get(0).asType().toString()
//                executableElement.getParameters().get(1).asType();

                WireProxyGenerator.Method method = new WireProxyGenerator.Method(executableElement, methodTree);
                for(VariableElement v: executableElement.getParameters()) {
                    method.addParameter(v.asType().toString(), v.getSimpleName().toString());
                }

                wireProxyGenerator.addMethod(method);
            }


            try {
                final JavaFileObject fileObject = processingEnv.getFiler().createSourceFile(
                        packageElement.getQualifiedName() + "." + proxyClassName);

                try( Writer writter = fileObject.openWriter() ) {
                    writter.append(wireProxyGenerator.compileToString());
                }
            } catch( final IOException ex ) {
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
}