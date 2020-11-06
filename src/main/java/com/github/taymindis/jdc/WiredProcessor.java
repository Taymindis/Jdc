package com.github.taymindis.jdc;

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes("com.github.taymindis.jdc.Wired")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class WiredProcessor extends AbstractProcessor {
    protected static final String WIRE_PREFIX = "_wire_";
    private static final String WIRED_SUPER_CLASS = "com.github.taymindis.jdc.WiredContext";
    private static int methodIndex = 0;
//    private Trees trees;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
//        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {


        try {
            final ClassPool pool = ClassPool.getDefault();

            CtClass wiredSuperClass = getWiredSuperClass(pool);
            CtClass invocationExceptionClass = pool.get("java.lang.reflect.InvocationTargetException");
            CtClass illegalAccessExceptionClass = pool.get("java.lang.IllegalAccessException");


            for (final Element element : roundEnv.getElementsAnnotatedWith(Wired.class)) {
                if (element instanceof TypeElement) {
                    final TypeElement typeElement = (TypeElement) element;

                    final String className = typeElement.getQualifiedName() + "";
                    Class<?> clz = Class.forName(className);

                    ClassClassPath ccpath = new ClassClassPath(clz);
                    pool.insertClassPath(ccpath);
                    final CtClass compiledClass;
                    compiledClass = pool.get(className);
                    compiledClass.setSuperclass(wiredSuperClass);

                    final CtMethod methods[] = compiledClass.getDeclaredMethods();

                    for (CtMethod method : methods) {

                        String coreMethodName = String.format("_%d%s%s", methodIndex++, WIRE_PREFIX, method.getName());

                        CtMethod coreMethod = CtNewMethod.copy(method, coreMethodName,
                                compiledClass, null);
                        compiledClass.addMethod(coreMethod);

                        boolean isVoidReturn = method.getReturnType().equals(CtClass.voidType);
                        boolean isStatic = Modifier.isStatic(method.getModifiers());

//                        MethodInfo methodInfo = method.getMethodInfo();
//                        LocalVariableAttribute table = (LocalVariableAttribute) methodInfo.getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
//                        List<String> parameterName = new ArrayList<>();
//                        for (int i = 0, sz = method.getParameterTypes().length; i < sz; i++) {
//                            parameterName.add(methodInfo.getConstPool().getUtf8Info(table.nameIndex(i + 1)));
//                        }

//                         Bug stack mapping issue
//                        setBodyKeepParamInfos(method, "{}", false);

                        CtClass ctClasses[] = method.getExceptionTypes();

                        Set<CtClass> newCtclasses = new HashSet<>();

                        for (CtClass ctClass : ctClasses) {
                            newCtclasses.add(ctClass);
                        }

                        newCtclasses.add(invocationExceptionClass);
                        newCtclasses.add(illegalAccessExceptionClass);
                        method.setExceptionTypes(newCtclasses.toArray(new CtClass[0]));

                        StringBuilder coreBlock = new StringBuilder("{  Object rs;  ");

                        // If conditionaling both side usage
                        coreBlock.append("if(!this.is_wiringjdc_()) { ");
                        coreBlock.append(" rs = ($r) ").append(coreMethod.getName()).append("($$);");
                        coreBlock.append(" } else {  ");

                        coreBlock.append("rs = ($r) this.invoke(\"");
                        coreBlock.append(coreMethodName).append("\"");
                        coreBlock.append(isStatic ? ",true" : ", false");
                        coreBlock.append(", $sig, $args");
                        coreBlock.append("); } ");
                        coreBlock.append("  return ($r) rs; } ");

//                        setBodyKeepParamInfos(method, coreBlock.toString(), true);
//                        method.insertAt(0, coreBlock.toString());
//                        method.instrument(new MethodReplacer(method,coreMethod));

                        CtMethod newM = CtNewMethod.make(method.getModifiers(),
                                method.getReturnType(),
                                method.getName(), method.getParameterTypes(), method.getExceptionTypes(), coreBlock.toString(), compiledClass);

//                        newM.setName("X" + method.getName());

                        compiledClass.removeMethod(method);
                        compiledClass.addMethod(newM);

                    }

                    compiledClass.writeFile(clz.getProtectionDomain().getCodeSource().getLocation()
                            .getPath());

                }
            }

        } catch (NotFoundException | CannotCompileException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Issue ");
        }
        return true;
    }

    private CtClass getWiredSuperClass(final ClassPool pool) throws NotFoundException, ClassNotFoundException {
        String wireClassName = WIRED_SUPER_CLASS;
        Class<?> superClz = Class.forName(wireClassName);
        pool.insertClassPath(new ClassClassPath(superClz));
        return pool.get(wireClassName);
    }

    private static void setBodyKeepParamInfos(CtMethod m, String src, boolean rebuild) throws CannotCompileException {
        CtClass cc = m.getDeclaringClass();
        if (cc.isFrozen()) {
            throw new RuntimeException(cc.getName() + " class is frozen");
        }
        CodeAttribute ca = m.getMethodInfo().getCodeAttribute();
        if (ca == null) {
            throw new CannotCompileException("no method body");
        } else {
            CodeIterator iterator = ca.iterator();
            Javac jv = new Javac(cc);

            try {
                int nvars = jv.recordParams(m.getParameterTypes(), Modifier.isStatic(m.getModifiers()));
                jv.recordParamNames(ca, nvars);
                jv.recordLocalVariables(ca, 0);
                jv.recordReturnType(Descriptor.getReturnType(m.getMethodInfo().getDescriptor(), cc.getClassPool()), false);
                //jv.compileStmnt(src);
                //Bytecode b = jv.getBytecode();
                Bytecode b = jv.compileBody(m, src);
                int stack = b.getMaxStack();
                int locals = b.getMaxLocals();
                if (stack > ca.getMaxStack()) {
                    ca.setMaxStack(stack);
                }

                if (locals > ca.getMaxLocals()) {
                    ca.setMaxLocals(locals);
                }
                int pos = iterator.insertEx(b.get());
                iterator.insert(b.getExceptionTable(), pos);
                if (rebuild) {
                    m.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
                }
            } catch (NotFoundException var12) {
                throw new CannotCompileException(var12);
            } catch (CompileError var13) {
                throw new CannotCompileException(var13);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }
        }
    }
}
