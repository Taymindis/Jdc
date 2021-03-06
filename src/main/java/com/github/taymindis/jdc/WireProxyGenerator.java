package com.github.taymindis.jdc;

import com.sun.source.tree.MethodTree;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class WireProxyGenerator {

    private List<Method> methods = new ArrayList<>();
    private String packageQualifiedName;
    private String targetClassName;
    private String proxyClassName;

    public WireProxyGenerator(String packageQualifiedName, String targetClassName, String proxyClassName) {
        this.packageQualifiedName = packageQualifiedName != null
                && !packageQualifiedName.isEmpty()
        ? "package ".concat(packageQualifiedName).concat(";") : "";
        this.targetClassName = targetClassName;
        this.proxyClassName = proxyClassName;
    }

    public void addMethod(Method method) {
        this.methods.add(method);
    }


    public String compileToString(ProcessingEnvironment processingEnv) {
        StringBuilder methodBuilder = new StringBuilder();

        for (Method m : methods) {
            methodBuilder.append(m.compileToString(processingEnv, targetClassName));
        }

        return String.format("%s\n" +
                        "\n" +
                        "import java.lang.reflect.Method;\n" +
                        "\n" +
                        "public class %s extends %s {\n" +
                        "    private Object ctx;\n" +
                        "    private boolean _wiringjdc_;\n" +
                        "    private Class<?> classUsing;\n" +
                        "    public %s() {\n" +
                        "        _wiringjdc_ = false;\n" +
                        "    }\n" +
                        "\n" +
                        "    public %s(Object ctx, Class<?> classUsing) {\n" +
                        "        this.ctx = ctx;\n" +
                        "        this._wiringjdc_ = false;\n" +
                        "        this.classUsing = classUsing;\n" +
                        "    }\n" +
                        "\n" +
                        "    public Object getCtx() {\n" +
                        "        return this.ctx;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setCtx(Object ctx) {\n" +
                        "        this.ctx = ctx;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setClassUsing(Class<?> classUsing) {\n" +
                        "        this.classUsing = classUsing;\n" +
                        "    }\n" +
                        "\n" +
                        "    public boolean is_wiringjdc_() {\n" +
                        "        return _wiringjdc_;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void set_wiringjdc_(boolean _wiringjdc_) {\n" +
                        "        this._wiringjdc_ = _wiringjdc_;\n" +
                        "    }\n" +
                        "\n" +
                        "    %s\n" +
                        "}", packageQualifiedName, proxyClassName, targetClassName,
                proxyClassName, proxyClassName, methodBuilder.toString());

    }


    protected static class Method {
        private final String resultCastType;
        private final String name;
        private final List<String> parameterNames;
        private final List<String> parameterTypes;
        private final String returnType;
        private final String modifier;
        //        private final String methodBody;
        private final boolean isVoid;
        private final boolean isStatic;

        public Method(ExecutableElement executableElement, MethodTree tree) {
            this.name = tree.getName() + "";
            if(isGenericReturnType(executableElement)) {
                String boundType = "";
                if(executableElement.getReturnType() instanceof TypeVariable) {
                    boundType = ((TypeVariable) executableElement.getReturnType()).getUpperBound() + "";
                }
                this.modifier = tree.getModifiers().toString().concat("<").concat(executableElement.getReturnType().toString())
                        .concat(" extends ").concat(boundType).concat("> ");
            } else {
                this.modifier = tree.getModifiers().toString();
            }

            this.returnType = executableElement.getReturnType().toString();

            this.parameterNames = new ArrayList<>();
            this.parameterTypes = new ArrayList<>();
//            this.methodBody = tree.getBody().toString();
            isVoid = this.returnType.equals("void");
            this.isStatic = executableElement.getModifiers().contains(Modifier.STATIC);
            this.resultCastType = isVoid ? "" : String.format("(%s) rs", this.returnType);
        }

        public void addParameter(String parameterType, String parameterName) {
            parameterTypes.add(parameterType);
            parameterNames.add(parameterName);
        }

        public String compileToString(ProcessingEnvironment processingEnv, String targetClassName) {

            StringBuilder argsBuilder = new StringBuilder();
            StringBuilder allTypes = new StringBuilder();
            StringBuilder allParams = new StringBuilder();
            if (!parameterNames.isEmpty()) {
                for (int i = 0, sz = parameterNames.size(); i < sz; i++) {
                    String type = parameterTypes.get(i);
                    String name = parameterNames.get(i);

                    argsBuilder.append(type).append(" ").append(name).append(",");
                    allTypes.append(", ").append(type).append(".class");
                    allParams.append(", ").append(name);
                }
                argsBuilder.setLength(argsBuilder.length() - 1);
            }

            if(isStatic) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Static Class have to use proxy class to call.");
            }


            return String.format("%s %s %s(%s) {\n" +
                            "        try {\n" +
                            "            Method m = %s.getDeclaredMethod(\"%s\"%s);\n" +
                            "            Object rs = m.invoke(%s%s);\n" +
                            "            return %s;\n" +
                            "        } catch (Exception e) {\n" +
                            "            e.printStackTrace();\n" +
                            "        }\n" +
                            "        return %s;\n" +
                            "\n" +
                            " }\n\n", modifier, returnType, name, argsBuilder.toString(),
                    isStatic ? "com.github.taymindis.jdc.Jdc.wireClass(".concat(targetClassName).concat(".class)") : "this.classUsing",
                    name, allTypes.toString(),
                    isStatic ? "null" : "this.ctx",
                    allParams.toString(),
                    resultCastType, isVoid ? "" : "null");


        }
    }

//    private static String getFullQualifiedName(TypeMirror type) {
////        ((TypeElement)processingEnv.getTypeUtils().asElement(executableElement.getParameters().get(1).asType())).getQualifiedName()
//        return type.toString();
//    }

    private static boolean isGenericReturnType(ExecutableElement executableElement) {
        return executableElement.getReturnType().getKind().compareTo(TypeKind.TYPEVAR) == 0;
    }
}