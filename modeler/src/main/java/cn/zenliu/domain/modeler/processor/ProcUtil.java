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
import com.squareup.javapoet.TypeName;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
public interface ProcUtil {
    ProcessingEnvironment env();

    Elements elements();

    Types types();

    Filer filer();

    boolean isDebug();

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

    default void note(String msg) {
        log(Diagnostic.Kind.NOTE, msg);
    }

    default void note(MessageInfo msg) {
        log(Diagnostic.Kind.NOTE, msg);
    }

    default void note(String pattern, Object... args) {
        note(formatter(pattern, args));
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

    default void mandatoryWarn(String msg) {
        log(Diagnostic.Kind.MANDATORY_WARNING, msg);
    }

    default void mandatoryWarn(MessageInfo msg) {
        log(Diagnostic.Kind.MANDATORY_WARNING, msg);
    }

    default void mandatoryWarn(String pattern, Object... args) {
        mandatoryWarn(formatter(pattern, args));
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

    default MessageInfo formatter(String pattern, Object... args) {
        return MessageInfo.format(pattern, args);
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

    default TypeMirror type(String fqn) {
        return elements().getTypeElement(fqn).asType();
    }

    default TypeMirror type(Class<?> type) {
        return elements().getTypeElement(type.getCanonicalName()).asType();
    }

    default TypeElement typeElement(TypeMirror type) {
        return ((TypeElement) types().asElement(type));
    }

    default TypeElement typeElement(String qualifiedName) {
        return typeElement(type(qualifiedName));
    }

    default boolean isAssignable(TypeMirror type1, TypeMirror type2) {
        if (type1.getKind() == TypeKind.ERROR || type2.getKind() == TypeKind.ERROR) {
            env().getMessager().printMessage(Diagnostic.Kind.OTHER, "one of " + type1 + " and " + type2 + " not resolved");
            var t1 = typeElement(type1);
            var t2 = typeElement(type2);
            if (t1 == null || t2 == null) return false;
            return t1.getQualifiedName().equals(t2.getQualifiedName());
        }
        return types().isAssignable(type1, type2);
    }

    default boolean isAssignable(TypeMirror type1, Class<?> type2) {
        if (type1.getKind() == TypeKind.ERROR) {
            env().getMessager().printMessage(Diagnostic.Kind.OTHER, "one of " + type1 + " and " + type2 + " not resolved");
            var t1 = typeElement(type1);
            if (t1 == null) return false;
            return t1.getQualifiedName().toString().equals(type2.getCanonicalName());
        }
        return types().isAssignable(type1, typeElement(type2.getCanonicalName()).asType());
    }

    default boolean isSameType(TypeMirror type1, Class<?> type2) {
        return types().isSameType(type1, type(type2));
    }

    default boolean isSameType(TypeMirror type1, TypeMirror type2) {
        return types().isSameType(type1, type2);
    }

    default boolean subtypeOf(TypeMirror type1, TypeMirror type2) {
        return types().isSubtype(type1, type2);
    }

    default boolean contains(TypeMirror type1, TypeMirror type2) {
        return types().contains(type1, type2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    default boolean subsignatureOf(ExecutableType type1, ExecutableType type2) {
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

    //endregion

    //region Elements
    default PackageElement packageOf(Element e) {
        return elements().getPackageOf(e);
    }

    default boolean deprecated(Element e) {
        return elements().isDeprecated(e);
    }

    default Name deprecated(TypeElement e) {
        return elements().getBinaryName(e);
    }

    default List<? extends Element> allMembers(TypeElement e) {
        return elements().getAllMembers(e);
    }

    default List<? extends AnnotationMirror> allAnnotationMirrors(TypeElement e) {
        return elements().getAllAnnotationMirrors(e);
    }

    default boolean hides(Element hider, Element hidden) {
        return elements().hides(hider, hidden);
    }

    default boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
                              TypeElement type) {
        return elements().overrides(overrider, overridden, type);
    }

    default Name name(CharSequence name) {
        return elements().getName(name);
    }

    default boolean functionalInterface(TypeElement type) {
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

    default String toClassName(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR) {
            return TypeName.get(Object.class).toString();
        } else if (!typeElement(type).getTypeParameters().isEmpty()) {
            return typeElement(type).getQualifiedName().toString();
        } else {
            return TypeName.get(type).toString();
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
        for (var e : allMembers(typeElement(root))) {
            if (e instanceof ExecutableElement ex && ex.getSimpleName().toString().equals(name)) {
                if (param != null && !param.isEmpty()) {
                    var i = new AtomicInteger();
                    if (
                            ex.getParameters().size() == param.size() &&
                            ex.getParameters().stream().allMatch(x -> TypeName.get(x.asType()).equals(param.get(i.getAndIncrement())))
                    )
                        return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    //endregion
}
