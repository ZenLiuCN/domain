/*
 * Source of domain
 * Copyright (C) 2023.  Zen.Liu
 *
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0"
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; version 2.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Class Path Exception
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *  As a special exception, the copyright holders of this library give you permission to link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this library, you may extend this exception to your version of the library, but you are not obligated to do so. If you do not wish to do so, delete this exception statement from your version.
 */

package cn.zenliu.domain.modeler.processor;

import cn.zenliu.domain.modeler.annotation.Generated;
import cn.zenliu.domain.modeler.processor.safer.BaseProcUtil;
import com.squareup.javapoet.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.helpers.MessageFormatter;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@ApiStatus.AvailableSince("0.1.2")
public interface ProcUtil extends BaseProcUtil {

    //region Logger
    default void log(Diagnostic.Kind kind, String msg, Element element, AnnotationMirror a, AnnotationValue v) {
        var m = env().getMessager();
        if (element != null)
            if (a != null)
                if (v != null)
                    m.printMessage(kind, msg, element, a, v);
                else
                    m.printMessage(kind, msg, element, a);
            else
                m.printMessage(kind, msg);
    }

    default void log(Diagnostic.Kind kind, String msg) {
        log(kind, msg, null, null, null);
    }

    default void log(Diagnostic.Kind kind, MessageInfo msg) {
        log(kind, msg.message(), msg.element(), msg.annotation(), msg.value());
    }

    default void log(Diagnostic.Kind kind, String pattern, Object... args) {
        log(kind, formatter(pattern, args));
    }

    default void other(String msg) {
        log(Diagnostic.Kind.OTHER, msg);
    }

    default void other(MessageInfo msg) {
        log(Diagnostic.Kind.OTHER, msg);
    }

    default void other(String pattern, Object... args) {
        other(formatter(pattern, args));
    }

    default void other(AbstractProcessor proc, String pattern, Object... args) {
        other(formatter("[" + proc.name() + "\t] [OTHER] " + pattern, args));
    }

    default void note(String msg) {
        log(Diagnostic.Kind.NOTE, msg);
    }

    default void note(MessageInfo msg) {
        log(Diagnostic.Kind.NOTE, msg);
    }

    default void note(String pattern, Object... args) {
        note(formatter(pattern, args));
    }

    default void note(AbstractProcessor proc, String pattern, Object... args) {
        note(formatter("[" + proc.name() + "\t] [NOTE] " + pattern, args));
    }

    default void warn(String msg) {
        log(Diagnostic.Kind.WARNING, msg);
    }

    default void warn(MessageInfo msg) {
        log(Diagnostic.Kind.WARNING, msg);
    }

    default void warn(String pattern, Object... args) {
        warn(formatter(pattern, args));
    }

    default void warn(AbstractProcessor proc, String pattern, Object... args) {
        warn(formatter("[" + proc.name() + "\t] [WARN] " + pattern, args));
    }

    default void mandatoryWarn(String msg) {
        log(Diagnostic.Kind.MANDATORY_WARNING, msg);
    }

    default void mandatoryWarn(MessageInfo msg) {
        log(Diagnostic.Kind.MANDATORY_WARNING, msg);
    }

    default void mandatoryWarn(String pattern, Object... args) {
        mandatoryWarn(formatter(pattern, args));
    }

    default void mandatoryWarn(AbstractProcessor proc, String pattern, Object... args) {
        mandatoryWarn(formatter("[" + proc.name() + "\t] [WARN] " + pattern, args));
    }

    default void error(String msg) {
        log(Diagnostic.Kind.ERROR, msg);
    }

    default void error(MessageInfo msg) {
        log(Diagnostic.Kind.ERROR, msg);
    }

    default void error(String pattern, Object... args) {
        error(formatter(pattern, args));
    }

    default void error(AbstractProcessor proc, String pattern, Object... args) {
        error(formatter("[" + proc.name() + "\t] [ERROR] " + pattern, args));
    }

    /**
     * @return IllegalStateException
     */
    default RuntimeException fatal(String msg) {
        log(Diagnostic.Kind.ERROR, msg);
        return new IllegalStateException(msg);
    }

    default String fatal(MessageInfo msg) {
        log(Diagnostic.Kind.ERROR, msg);
        return msg.message();
    }

    /**
     * @return IllegalStateException
     */
    default String fatal(String pattern, Object... args) {
        return fatal(formatter(pattern, args));

    }

    default String fatal(AbstractProcessor proc, String pattern, Object... args) {
        return fatal(formatter("[" + proc.name() + "\t] [FATAL] " + pattern, args));

    }

    default MessageInfo formatter(String pattern, Object... args) {
        return MessageInfo.format(pattern, args);
    }


    /**
     * print info to {@link System#out}, nothrow.<br/>
     * <b>Note:</b> use {@link ProcUtil#note(String)} when processing.
     *
     * @param pattern SLF4J pattern
     * @param args    values
     */
    default void debug(AbstractProcessor processor, String pattern, Object... args) {
        System.out.println(MessageFormatter.arrayFormat("[" + processor.name() + "\t] [DEBUG] " + pattern, args).getMessage());
    }

    default void debug(String name, String pattern, Object... args) {
        System.out.println(MessageFormatter.arrayFormat("[" + name + "\t] [DEBUG] " + pattern, args).getMessage());
    }

    /**
     * print into {@link System#err} ,if args have throwable,the throw error.<br/>
     * <b>Note:</b> use {@link ProcUtil#fatal(String)} when processing.
     *
     * @param pattern SLF4J pattern
     * @param args    values
     */
    @SneakyThrows
    default void errorf(AbstractProcessor processor, String pattern, Object... args) {
        var m = MessageFormatter.arrayFormat("[" + processor.name() + "\t] [ERROR] " + pattern, args);
        System.err.println(m.getMessage());
        if (m.getThrowable() != null) {
            m.getThrowable().printStackTrace(System.err);
            throw m.getThrowable();
        }
    }

    @Getter
    @Accessors(fluent = true)
    final class MessageInfo {
        private final String message;
        private final Throwable throwable;
        private final Element element;
        private final AnnotationMirror annotation;
        private final AnnotationValue value;

        public MessageInfo(String pattern, Object... args) {
            var t = MessageFormatter.arrayFormat(pattern, args);
            message = t.getMessage();
            throwable = t.getThrowable();
            Element ele = null;
            AnnotationMirror a = null;
            AnnotationValue av = null;
            var ar = t.getArgArray();
            for (Object arg : ar) {
                if (arg instanceof Element) ele = (Element) arg;
                else if (arg instanceof AnnotationMirror) a = (AnnotationMirror) arg;
                else if (arg instanceof AnnotationValue) av = (AnnotationValue) arg;
            }
            this.element = ele;
            this.annotation = a;
            this.value = av;
        }

        public MessageInfo(String pattern, Object arg) {
            var t = MessageFormatter.format(pattern, arg, arg);
            message = t.getMessage();
            throwable = t.getThrowable();
            Element ele = null;
            AnnotationMirror a = null;
            AnnotationValue av = null;
            if (throwable != null) {
                this.element = null;
                this.annotation = null;
                this.value = null;
                return;
            }
            if (arg instanceof Element) ele = (Element) arg;
            else if (arg instanceof AnnotationMirror) a = (AnnotationMirror) arg;
            else if (arg instanceof AnnotationValue) av = (AnnotationValue) arg;

            this.element = ele;
            this.annotation = a;
            this.value = av;
        }

        public MessageInfo(String pattern, Object arg1, Object arg2) {
            var t = MessageFormatter.format(pattern, arg1, arg2);
            message = t.getMessage();
            throwable = t.getThrowable();
            Element ele = null;
            AnnotationMirror a = null;
            AnnotationValue av = null;
            if (arg1 instanceof Element) ele = (Element) arg1;
            else if (arg1 instanceof AnnotationMirror) a = (AnnotationMirror) arg1;
            else if (arg1 instanceof AnnotationValue) av = (AnnotationValue) arg1;
            if (arg2 instanceof Element) ele = (Element) arg2;
            else if (arg2 instanceof AnnotationMirror) a = (AnnotationMirror) arg2;
            else if (arg2 instanceof AnnotationValue) av = (AnnotationValue) arg2;
            this.element = ele;
            this.annotation = a;
            this.value = av;
        }

        public static MessageInfo format(String pattern, Object arg1) {
            return new MessageInfo(pattern, arg1);
        }

        public static MessageInfo format(String pattern, Object arg1, Object arg2) {
            return new MessageInfo(pattern, arg1, arg2);
        }

        public static MessageInfo format(String pattern, Object... args) {
            return new MessageInfo(pattern, args);
        }
    }

    //endregion


    //region Types

    default TypeMirror typeOf(String fqn) {
        return elements().getTypeElement(fqn).asType();
    }

    default TypeMirror typeOf(Class<?> type) {
        return elements().getTypeElement(type.getCanonicalName()).asType();
    }

    default TypeElement typeElementOf(TypeMirror type) {
        return ((TypeElement) types().asElement(type));
    }

    default TypeElement typeElementOf(String qualifiedName) {
        return typeElementOf(typeOf(qualifiedName));
    }

    default boolean isAssignableTo(TypeMirror type1, TypeMirror type2) {
        if (type1.getKind() == TypeKind.ERROR || type2.getKind() == TypeKind.ERROR) {
            env().getMessager().printMessage(Diagnostic.Kind.OTHER, "one of " + type1 + " and " + type2 + " not resolved");
            var t1 = typeElementOf(type1);
            var t2 = typeElementOf(type2);
            if (t1 == null || t2 == null) return false;
            return t1.getQualifiedName().equals(t2.getQualifiedName());
        }
        return types().isAssignable(type1, type2);
    }

    default boolean isAssignableTo(TypeMirror type1, Class<?> type2) {
        if (type1.getKind() == TypeKind.ERROR) {
            env().getMessager().printMessage(Diagnostic.Kind.OTHER, "one of " + type1 + " and " + type2 + " not resolved");
            var t1 = typeElementOf(type1);
            if (t1 == null) return false;
            return t1.getQualifiedName().toString().equals(type2.getCanonicalName());
        }
        return types().isAssignable(type1, typeOf(type2));
    }

    default boolean isSameType(TypeMirror type1, Class<?> type2) {
        return types().isSameType(type1, typeOf(type2));
    }

    default boolean isSameType(TypeMirror type1, TypeMirror type2) {
        return types().isSameType(type1, type2);
    }

    default boolean isSubTypeOf(TypeMirror type1, TypeMirror type2) {
        return types().isSubtype(type1, type2);
    }

    default boolean isContains(TypeMirror type1, TypeMirror type2) {
        return types().contains(type1, type2);
    }


    default boolean isSubSignatureOf(ExecutableType type1, ExecutableType type2) {
        return types().isSubsignature(type1, type2);
    }

    default List<? extends TypeMirror> directSupertypes(TypeMirror type) {
        return types().directSupertypes(type);
    }

    default TypeMirror erasure(TypeMirror type) {
        return types().erasure(type);
    }

    default TypeElement boxedClass(PrimitiveType type) {
        return types().boxedClass(type);
    }

    default PrimitiveType unboxedType(TypeMirror type) {
        return types().unboxedType(type);
    }

    default TypeMirror capture(TypeMirror type) {
        return types().capture(type);
    }

    default PrimitiveType primitiveType(TypeKind kind) {
        return types().getPrimitiveType(kind);
    }

    default NullType nullType() {
        return types().getNullType();
    }

    default NoType noType(TypeKind kind) {
        return types().getNoType(kind);
    }

    default ArrayType arrayType(TypeMirror component) {
        return types().getArrayType(component);
    }

    default WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        return types().getWildcardType(extendsBound, superBound);
    }

    default WildcardType wildcardExtendsOf(TypeMirror extendsBound) {
        return types().getWildcardType(extendsBound, null);
    }

    default WildcardType wildcardSuperOf(TypeMirror superBound) {
        return types().getWildcardType(null, superBound);
    }

    default DeclaredType declaredTypeOf(TypeElement typeElem, TypeMirror... typeArgs) {
        return types().getDeclaredType(typeElem, typeArgs);
    }

    default DeclaredType declaredTypeOf(DeclaredType containing, TypeElement typeElem, TypeMirror... typeArgs) {
        return types().getDeclaredType(containing, typeElem, typeArgs);
    }

    default TypeMirror asMemberOf(DeclaredType containing, Element element) {
        return types().asMemberOf(containing, element);
    }

    default boolean isRawAssignableTo(TypeMirror from, TypeMirror to) {
        return isAssignableTo(erasure(from), erasure(to));
    }

    default boolean isRawAssignableTo(TypeMirror from, Class<?> to) {
        return isAssignableTo(erasure(from), erasure(typeOf(to)));
    }
    //endregion

    //region Elements
    default PackageElement packageOf(Element e) {
        return elements().getPackageOf(e);
    }

    default boolean isDeprecated(Element e) {
        return elements().isDeprecated(e);
    }

    default Name binaryName(TypeElement e) {
        return elements().getBinaryName(e);
    }

    default List<? extends Element> allMembers(TypeElement e) {
        return elements().getAllMembers(e);
    }

    default List<? extends AnnotationMirror> allAnnotationMirrors(TypeElement e) {
        return elements().getAllAnnotationMirrors(e);
    }

    default boolean isHides(Element hider, Element hidden) {
        return elements().hides(hider, hidden);
    }

    default boolean isOverrides(ExecutableElement overrider, ExecutableElement overridden,
                                TypeElement type) {
        return elements().overrides(overrider, overridden, type);
    }

    default Name name(CharSequence name) {
        return elements().getName(name);
    }

    default boolean isFunctionalInterface(TypeElement type) {
        return elements().isFunctionalInterface(type);
    }

    default void print(Writer w, Element... elements) {
        elements().printElements(w, elements);
    }

    default Map<? extends ExecutableElement, ? extends AnnotationValue> valuesWithDefaults(AnnotationMirror e) {
        return elements().getElementValuesWithDefaults(e);
    }
    //endregion

    //region Names
    default @Nullable String getterToField(CharSequence name, boolean onlyBean) {
        if (name.length() > 3 && name.subSequence(0, 3).toString().equals("get")) {
            return Character.toLowerCase(name.charAt(3)) + (name.length() > 4 ? name.subSequence(4, name.length()).toString() : "");
        } else if (name.length() > 2 && name.subSequence(0, 2).toString().equals("is")) {
            return Character.toLowerCase(name.charAt(2)) + (name.length() > 3 ? name.subSequence(3, name.length()).toString() : "");
        } else if (!onlyBean) {
            return name.toString();
        }
        return null;
    }

    default @Nullable String getterToSetter(CharSequence name, boolean onlyBean) {
        if (name.length() > 3 && name.subSequence(0, 3).toString().equals("get")) {
            return "set" + name.subSequence(3, name.length());
        } else if (name.length() > 2 && name.subSequence(0, 2).toString().equals("is")) {
            return "set" + name.subSequence(2, name.length());
        } else if (!onlyBean) {
            return name.toString();
        }
        return null;
    }

    default @Nullable String setterToField(CharSequence name, boolean onlyBean) {
        if (name.length() > 3 && name.subSequence(0, 3).toString().equals("set")) {
            return Character.toLowerCase(name.charAt(3)) + (name.length() > 4 ? name.subSequence(4, name.length()).toString() : "");
        } else if (!onlyBean) {
            return name.toString();
        }
        return null;
    }

    default TypeName toTypeName(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR) {
            return TypeName.get(Object.class);
        } else {
            var t = typeElementOf(type);
            if (t == null) {
                return TypeName.get(Object.class);
            }
            return TypeName.get(type);
        }
    }

    default String singularToPlural(String name) {
        if (name.endsWith("y")) {
            return name.substring(0, name.length() - 1) + "ies";
        } else if (name.endsWith("s")) {
            return name + "es";
        } else {
            return name + "s";
        }
    }

    default String pluralToSingular(String name) {
        if (name.endsWith("ies")) {
            return name.substring(0, name.length() - 3) + "y";
        } else if (name.endsWith("ses")) {
            return name.substring(0, name.length() - 2);
        } else {
            return name.endsWith("s") ? name.substring(0, name.length() - 1) : name;
        }
    }

    //endregion

    //region Trees

    /**
     * @apiNote only javac supported
     */
    default JavacTask task() {
        return JavacTask.instance(env());
    }

    /**
     * @apiNote only javac supported
     */
    default Trees trees() {
        return Trees.instance(env());
    }

    /**
     * @param element target element
     * @return URI found in javac
     * @apiNote only javac supported
     */
    default URI sourcePath(Element element) {
        return Trees.instance(env()).getPath(element).getCompilationUnit().getSourceFile().toUri();
    }
    //endregion

    //region Other
    @SuppressWarnings("RedundantLengthCheck")
    default boolean isGeneratedBy(Element element, String name) {
        var generates = element.getAnnotationsByType(Generated.class);
        if (generates.length == 0) return false;
        for (var g : generates) {
            if (g.processor().equals(name)) {
                return true;
            }
        }
        return false;
    }

    default boolean isDirectlyInherit(TypeElement element, Class<?> type) {
        for (var face : element.getInterfaces()) {
            if (isSameType(face, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param root  the root Type
     * @param name  method name to check
     * @param param param type names to check (order important)
     */
    default boolean declaredMethod(TypeMirror root, String name, List<TypeName> param) {
        for (var e : allMembers(typeElementOf(root))) {
            if (e instanceof ExecutableElement ex && ex.getSimpleName().toString().equals(name)) {
                if ((param != null && !param.isEmpty()) && (ex.getParameters().size() == param.size())) {
                    var i = 0;
                    var same = true;
                    for (var x : ex.getParameters()) {
                        if (!TypeName.get(x.asType()).equals(param.get(i))) {
                            same = false;
                            break;
                        }
                        i++;
                    }
                    if (same) return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    default boolean isStatic(Element e) {
        return e.getModifiers().contains(Modifier.STATIC);
    }

    default boolean isDefault(Element e) {
        return e.getModifiers().contains(Modifier.DEFAULT);
    }

    default boolean isGetter(ExecutableElement e, boolean onlyBeanStyle) {
        return (e.getParameters().size() == 0 && e.getReturnType().getKind() != TypeKind.VOID)
                && (!onlyBeanStyle
                || e.getSimpleName().toString().startsWith("get")
                || e.getSimpleName().toString().startsWith("is"));
    }

    default boolean hasDeclared(String methodName, ExecutableElement e, TypeMirror declaring) {
        return declaredMethod(declaring, methodName, List.of(TypeName.get(e.getReturnType())));
    }

    default boolean isDeclaredBy(Element e, Class<?> cls) {
        return isSameType(e.getEnclosingElement().asType(), cls);
    }

    default boolean isDeclaredBy(Element e, TypeMirror type) {
        return isSameType(e.getEnclosingElement().asType(), type);
    }

    default Map<String, TypeMirror> resolveTypeParameters(TypeElement type) {
        var m = new HashMap<String, TypeMirror>();
        for (var t : type.getTypeParameters()) {
            m.put(t.getSimpleName().toString(), t.asType());
        }
        return m;
    }

    @Contract(mutates = "param1")
    default void mergeTypeVariables(Map<String, TypeMirror> modelVariables,
                                    Map<String, TypeMirror> variables,
                                    Map<String, TypeMirror> parameters) {
        modelVariables.forEach((k, v) -> {
            var var = variables.get(k);
            if (var != null) {
                modelVariables.put(k, var);
                return;
            }
            var = parameters.get(k);
            if (var != null) modelVariables.put(k, var);
        });

    }

    default TypeName[] orderedParameterType(Map<String, TypeMirror> parameters, TypeElement element, Function<TypeMirror, TypeName> conv) {
        return element.getTypeParameters().stream()
                .map(x -> {
                    var name = x.getSimpleName().toString();
                    var var = parameters.get(name);
                    if (var != null) {
                        var v = conv.apply(var);
                        if (v != null) return v;
                        return TypeName.get(var);
                    }
                    var v = conv.apply(x.asType());
                    if (v != null) return v;
                    return TypeName.get(x.asType());
                })
                .toArray(TypeName[]::new);
    }

    /**
     * @param v       the type variable
     * @param method  optional method
     * @param element the element
     * @return most possible typename of the variable
     */
    default TypeName resolveTypeArgument(TypeVariable v, @Nullable ExecutableElement method, TypeElement element) {
        var name = v.asElement().getSimpleName();
        if (method != null && method.getTypeParameters().stream().anyMatch(x -> x.getSimpleName().equals(name)))
            return TypeName.get(v);
        if (element.getTypeParameters().stream().anyMatch(x -> x.getSimpleName().equals(name))) return TypeName.get(v);
        for (var face : element.getInterfaces()) {
            if (face instanceof DeclaredType d) {
                var names = ((TypeElement) d.asElement()).getTypeParameters();
                for (int i = 0; i < names.size(); i++) {
                    var n = names.get(i).getSimpleName();
                    if (n.equals(name)) {
                        var types = d.getTypeArguments();
                        return TypeName.get(types.get(i));
                    }
                }
            }
        }
        return TypeName.get(v);
    }

    /**
     * @param t       the type
     * @param method  optional method
     * @param element element
     * @return most possible type name
     */
    default TypeName resolveTypeName(TypeMirror t, @Nullable ExecutableElement method, TypeElement element) {
        if (t.getKind() == TypeKind.TYPEVAR) {
            var v = ((TypeVariable) t);
            return resolveTypeArgument(v, method, element);
        } else if (t.getKind() == TypeKind.ERROR) {
            //should not happen.
            return TypeName.get(t);
        } else if (t.getKind() == TypeKind.DECLARED) {
            var de = (DeclaredType) t;
            if (de.getTypeArguments().size() == 0) return TypeName.get(t);
            else {
                var vs = de.getTypeArguments().stream()
                        .map(v -> {
                            if (v instanceof TypeVariable tv) return resolveTypeArgument(tv, method, element);
                            return TypeName.get(v);
                        }).toList();
                var cn = ClassName.get((TypeElement) de.asElement());
                return ParameterizedTypeName.get(cn, vs.toArray(TypeName[]::new));
            }
        } else {
            return TypeName.get(t);
        }
    }

    /**
     * override a method, parameters are named as v0 ... v#.
     *
     * @param e method
     * @return builder
     */
    default MethodSpec.Builder overrides(ExecutableElement e) {
        return overrides(e, false);
    }

    default MethodSpec.Builder overrides(ExecutableElement e, boolean noTypeParameter) {
        var name = e.getSimpleName().toString();
        var typeVar = noTypeParameter ? List.<TypeVariableName>of() : e.getTypeParameters().stream().map(TypeVariableName::get).toList();
        var cnt = new AtomicInteger(0);
        var parameters = e.getParameters().stream()
                .map(x -> ParameterSpec.builder(TypeName.get(x.asType()), "v" + cnt.getAndIncrement(), x.getModifiers().toArray(Modifier[]::new))
                        .build())
                .toList();
        var returns = TypeName.get(e.getReturnType());
        var modifiers = new HashSet<>(e.getModifiers());
        modifiers.remove(Modifier.ABSTRACT);
        modifiers.remove(Modifier.DEFAULT);

        return MethodSpec.methodBuilder(name)
                .addModifiers(modifiers)
                .addAnnotation(Override.class)
                .addParameters(parameters)
                .returns(returns)
                .addTypeVariables(typeVar);
    }

    /**
     * compare two method, check if subj can project from obj, means that obj method can be an implement of subj.<br/>
     * <b>note:</b> parameter must have some ordered.
     *
     * @param sub        subject
     * @param obj        project
     * @param allowChain allow subject have a chained method that return self, not care obj returns
     * @param mappable   can two type be mapping from first to second.
     * @return same or not
     */
    default boolean isProjectMethod(ExecutableElement sub, ExecutableElement obj, boolean allowChain, BiPredicate<TypeMirror, TypeMirror> mappable) {
        if (!sub.getSimpleName().equals(obj.getSimpleName())) return false;
        if (sub.getParameters().size() != obj.getParameters().size()) return false;
        if (sub.getModifiers().contains(Modifier.STATIC) != obj.getModifiers().contains(Modifier.STATIC)) return false;
        var subjRet = sub.getReturnType();
        var objRet = obj.getReturnType();
        var subChained = isSameType(subjRet, sub.getEnclosingElement().asType());
        var retMappable = isAssignableTo(objRet, subjRet) || mappable.test(subjRet, objRet);
        if (!allowChain && !retMappable) return false;
        if (!retMappable && !subChained) return false;
        for (int i = 0; i < sub.getParameters().size(); i++) {
            var subjParam = sub.getParameters().get(i).asType();
            var objParam = obj.getParameters().get(i).asType();
            if (!isAssignableTo(subjParam, objParam) && !mappable.test(subjRet, objParam)) {
                return false;
            }
        }
        return true;

    }

    /**
     * @param src               source method (that have real logic)
     * @param tar               target method (that can be impl)
     * @param chain             dose target method use chain return and src not.
     * @param parameterMappings parameter value mapping objects in same ordered (null if not need).
     * @param returnTypeMapping return value mapping object (null if not need).
     * @param <T>               type mapping object
     */
    record Projection<T>(
            ExecutableElement src,
            ExecutableElement tar,
            boolean chain,
            List<T> parameterMappings,
            T returnTypeMapping
    ) {
    }

    default <T> @Nullable Projection<T> project(ExecutableElement sub, ExecutableElement obj,
                                                boolean allowChain,
                                                BiFunction<TypeMirror, TypeMirror, T> mappable) {
        if (!sub.getSimpleName().equals(obj.getSimpleName())) return null;
        var pSub = sub.getParameters();
        var pObj = obj.getParameters();
        if (pSub.size() != pObj.size()) return null;
        if (sub.getModifiers().contains(Modifier.STATIC) != obj.getModifiers().contains(Modifier.STATIC)) return null;
        var subjRet = sub.getReturnType();
        var objRet = obj.getReturnType();
        var subChained = isSameType(subjRet, sub.getEnclosingElement().asType());
        var retAssignable = isAssignableTo(objRet, subjRet);
        var retMapping = retAssignable ? null : mappable.apply(subjRet, objRet);
        if (!allowChain && !retAssignable && retMapping == null) return null;
        if (!retAssignable && !subChained && retMapping == null) return null;
        var projects = new ArrayList<T>(pSub.size());
        for (int i = 0; i < pSub.size(); i++) {
            var subjParam = pSub.get(i).asType();
            var objParam = pObj.get(i).asType();
            var assignable = isAssignableTo(subjParam, objParam);
            var mapping = mappable.apply(subjRet, objParam);
            if (!assignable && mapping == null) {
                return null;
            } else {
                projects.add(mapping);
            }
        }
        return new Projection<>(obj, sub, allowChain && subChained, projects, retMapping);
    }


    default MethodSpec mixin(ExecutableElement method, boolean setDefault) {
        var su = ClassName.get(method.getEnclosingElement().asType());
        var name = method.getSimpleName().toString();
        var code = CodeBlock.builder();
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            code.add("$T.super.$L(", su, name);
        } else {
            code.add("return $T.super.$L(", su, name);
        }
        var cnt = new AtomicInteger(0);
        method.getParameters().forEach(p -> {
            if (cnt.get() > 0) code.add(",");
            code.add("v$L", cnt.getAndIncrement());
        });
        code.add(");");

        return (setDefault ? overrides(method).addModifiers(Modifier.DEFAULT) : overrides(method))
                .addCode(code.build())
                .build();
    }

    /**
     * only returns by {@link ProcUtil#project(ExecutableElement, ExecutableElement, boolean, BiFunction)} and its mappable parameter.
     */
    final class SpecialMapping<C extends Enum<C>> implements ExecutableElement {
        final Object payload;

        public Object payload() {
            return payload;
        }

        @SuppressWarnings("unchecked")
        public <T> T param() {
            return (T) payload;
        }

        public C code() {
            return code;
        }

        final C code;

        public static <C extends Enum<C>> SpecialMapping<C> of(Object v, C code) {
            return new SpecialMapping<>(v, code);
        }

        private SpecialMapping(Object payload, C code) {

            this.payload = payload;
            this.code = code;
        }

        @Override
        public TypeMirror asType() {
            return null;
        }

        @Override
        public ElementKind getKind() {
            return null;
        }

        @Override
        public Set<Modifier> getModifiers() {
            return null;
        }

        @Override
        public List<? extends TypeParameterElement> getTypeParameters() {
            return null;
        }

        @Override
        public TypeMirror getReturnType() {
            return null;
        }

        @Override
        public List<? extends VariableElement> getParameters() {
            return null;
        }

        @Override
        public TypeMirror getReceiverType() {
            return null;
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public boolean isDefault() {
            return false;
        }

        @Override
        public List<? extends TypeMirror> getThrownTypes() {
            return null;
        }

        @Override
        public AnnotationValue getDefaultValue() {
            return null;
        }

        @Override
        public Name getSimpleName() {
            return null;
        }

        @Override
        public Element getEnclosingElement() {
            return null;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return null;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return null;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            return null;
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return null;
        }
    }

    //endregion

    //region Annotation

    default <T extends Annotation> @Nullable Annotated annotationByType(Element e, Class<T> annoType) {
        for (var mirror : e.getAnnotationMirrors()) {
            if (isSameType(mirror.getAnnotationType(), annoType)) {
                return () -> mirror;
            }
        }
        return null;
    }

    default <T extends Annotation> List<Annotated> findAll(Element e, Class<T> annoType) {
        var l = new ArrayList<Annotated>();
        for (var mirror : e.getAnnotationMirrors()) {
            if (isSameType(mirror.getAnnotationType(), annoType)) {
                l.add(() -> mirror);
            }
        }
        return l;
    }

    interface Annotated {
        AnnotationMirror mirror();

        default <T> @Nullable T read(String name, Function<AnnotationValue, @Nullable T> fn) {
            for (var entry : mirror().getElementValues().entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue();
                if (k.getSimpleName().toString().equals(name)) {
                    return Objects.requireNonNull(fn.apply(v), "Annotation value not match");
                }
            }
            return null;
        }

        default <T> List<T> readList(String name, Function<AnnotationValue, @Nullable T> fn) {
            var vals = new ArrayList<T>();
            for (var entry : mirror().getElementValues().entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue();
                if (k.getSimpleName().toString().equals(name)) {
                    var val = v.getValue();
                    if (val instanceof List<?> lst) {
                        for (var vx : lst) {
                            if (vx instanceof AnnotationValue value) {
                                vals.add(Objects.requireNonNull(fn.apply(value), "Annotation value not match"));
                            } else {
                                throw new IllegalStateException("Annotation value not inside list,should never happen:" + vx);
                            }
                        }
                    } else {
                        return vals;
                    }

                }
            }
            return vals;
        }

        static TypeMirror mayClass(AnnotationValue v) {
            return v.getValue() instanceof TypeMirror t ? t : null;
        }

        default TypeMirror readClass(String name) {
            return read(name, Annotated::mayClass);
        }

        default List<TypeMirror> readClasses(String name) {
            return readList(name, Annotated::mayClass);
        }

        static TypeElement mayClassElement(AnnotationValue v) {
            return v.getValue() instanceof TypeMirror t ? t instanceof DeclaredType d ? (TypeElement) d.asElement() : null : null;
        }

        default TypeElement readClassElement(String name) {
            return read(name, Annotated::mayClassElement);
        }

        default List<TypeElement> readClassElements(String name) {
            return readList(name, Annotated::mayClassElement);
        }

        static String mayString(AnnotationValue v) {
            return v.getValue() instanceof String t ? t : null;
        }

        default String readString(String name) {
            return read(name, Annotated::mayString);
        }

        default List<String> readStrings(String name) {
            return readList(name, Annotated::mayString);
        }

        static Integer mayInteger(AnnotationValue v) {
            return v.getValue() instanceof Integer t ? t : null;
        }

        default Integer readInteger(String name) {
            return read(name, Annotated::mayInteger);
        }

        default List<Integer> readIntegers(String name) {
            return readList(name, Annotated::mayInteger);
        }

        static Boolean mayBoolean(AnnotationValue v) {
            return v.getValue() instanceof Boolean t ? t : null;
        }

        default Boolean readBoolean(String name) {
            return read(name, Annotated::mayBoolean);
        }

        default List<Boolean> readBooleans(String name) {
            return readList(name, Annotated::mayBoolean);
        }

        static Short mayShort(AnnotationValue v) {
            return v.getValue() instanceof Short t ? t : null;
        }

        default Short readShort(String name) {
            return read(name, Annotated::mayShort);
        }

        default List<Short> readShorts(String name) {
            return readList(name, Annotated::mayShort);
        }

        static Byte mayByte(AnnotationValue v) {
            return v.getValue() instanceof Byte t ? t : null;
        }

        default Byte readByte(String name) {
            return read(name, Annotated::mayByte);
        }

        default List<Byte> readBytes(String name) {
            return readList(name, Annotated::mayByte);
        }

        static Long mayLong(AnnotationValue v) {
            return v.getValue() instanceof Long t ? t : null;
        }

        default Long readLong(String name) {
            return read(name, Annotated::mayLong);
        }

        default List<Long> readLongs(String name) {
            return readList(name, Annotated::mayLong);
        }

        static VariableElement mayVariableElement(AnnotationValue v) {
            return v.getValue() instanceof VariableElement t ? t : null;
        }

        default VariableElement readEnum(String name) {
            return read(name, Annotated::mayVariableElement);
        }

        default List<VariableElement> readEnums(String name) {
            return readList(name, Annotated::mayVariableElement);
        }

        static AnnotationMirror mayAnnotationMirror(AnnotationValue v) {
            return v.getValue() instanceof AnnotationMirror t ? t : null;
        }

        default AnnotationMirror readAnnotation(String name) {
            return read(name, Annotated::mayAnnotationMirror);
        }

        default List<AnnotationMirror> readAnnotations(String name) {
            return readList(name, Annotated::mayAnnotationMirror);
        }

        default <K, V> Map<K, V> readMap(String keyName, String valueName,
                                         Function<AnnotationValue, K> keyFn,
                                         Function<AnnotationValue, V> valueFn) {
            var m = new HashMap<K, V>();
            var keys = readList(keyName, keyFn);
            var values = readList(valueName, valueFn);
            if (keys.size() != values.size())
                throw new IllegalStateException(mirror().getAnnotationType().asElement().getSimpleName() + " requires " + keyName + " and " + valueName + " have same size");
            for (int i = 0; i < keys.size(); i++) {
                m.put(keys.get(i), values.get(i));
            }
            return m;
        }

    }
    //endregion

}
