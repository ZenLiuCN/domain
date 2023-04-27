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

package cn.zenliu.domain.modeler.util;

import cn.zenliu.domain.modeler.annotation.Info;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.*;
import java.util.*;

@ApiStatus.AvailableSince("0.1.4")
@Builder
@ToString
@EqualsAndHashCode
public class TypeInfo {
    @Getter
    @Accessors(fluent = true)
    public static class LazyClass {
        /**
         * The class name, same as {@link Class#getName()}
         */
        final String name;


        Class<?> cls;

        @SneakyThrows
        public Class<?> cls() {
            if (cls == null) {
                cls = Class.forName(name);
            }
            return cls;
        }

        public LazyClass(Class<?> cls) {
            this.cls = cls;
            this.name = cls.getName();
        }

        public static LazyClass of(Class<?> cls) {
            return new LazyClass(cls);
        }

        public LazyClass(String name) {
            this.name = name;
            this.cls = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LazyClass lazyClass = (LazyClass) o;
            return name.equals(lazyClass.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return name;
        }

        public static LazyClass of(String clsName) {
            return new LazyClass(clsName);
        }

        public static LazyClass OBJECT = of(Object.class);
    }

    @Getter
    final String name;

    public Class<?> getTypeClass() {
        return typeClass.cls();
    }

    final LazyClass typeClass;
    public LazyClass typeClass() {
        return typeClass;
    }
    @Getter
    final TypeInfo type;

    @Getter
    final boolean parameterized;
    @Getter
    final List<TypeInfo> typeArguments;

    @Getter
    final boolean array;

    @Getter
    final boolean boundary;
    @Getter
    final List<TypeInfo> upper;
    @Getter
    final List<TypeInfo> lower;

    /**
     * check one of {@link #typeClass} or {@link #type} should exist;
     */
    public boolean empty() {
        return typeClass == null && type == null;
    }

    /**
     * Current info not contains any things but a {@link #type}
     */
    public boolean isHolder() {
        return type != null &&
                (name == null || name.isEmpty()) &&
                !parameterized &&
                !boundary &&
                !array &&
                typeClass == null;
    }

    /**
     * reduce useless level
     */
    public TypeInfo flatten() {
        if (empty()) return this;
        if (isHolder()) {
            return type.flatten();
        }
        return this;
    }

    @SneakyThrows
    static TypeInfo deserialize(Bytes buf) {
        var b = TypeInfo.builder()
                .name(buf.readString());
        //type class
        if (buf.readBoolean()) {
            b.typeClass(new LazyClass(buf.readString()));
        }
        //type
        if (buf.readBoolean()) {
            b.type(deserialize(buf));
        }
        //parameterized
        {
            var n = buf.readInt();
            if (n > 0) {
                var l = new ArrayList<TypeInfo>();
                for (int i = 0; i < n; i++) {
                    l.add(deserialize(buf));
                }
                b.parameterized(true)
                        .typeArguments(l);
            }
        }
        //array
        b.array(buf.readBoolean());
        //boundary
        var boundary = buf.readBoolean();
        b.boundary(boundary);
        if (boundary) {
            //boundary upper
            if (buf.readBoolean()) {
                var n = buf.readInt();
                var lst = new ArrayList<TypeInfo>();
                for (int i = 0; i < n; i++) {
                    lst.add(deserialize(buf));
                }
                b.upper(lst);
            }
            //boundary lower
            if (buf.readBoolean()) {
                var n = buf.readInt();
                var lst = new ArrayList<TypeInfo>();
                for (int i = 0; i < n; i++) {
                    lst.add(deserialize(buf));
                }
                b.lower(lst);
            }
        }

        return b.build();
    }

    static void serialize(TypeInfo info, Bytes buf) {
        buf.put(info.name);
        //type class
        if (info.typeClass != null) {
            buf.put(true).put(info.typeClass.name());
        } else
            buf.put(false);
        //type
        if (info.type != null) {
            buf.put(true);
            serialize(info.type, buf);
        } else
            buf.put(false);
        //parameterized
        if (info.parameterized && info.typeArguments != null && !info.typeArguments.isEmpty()) {
            buf.put(info.typeArguments.size());
            for (var a : info.typeArguments) serialize(a, buf);
        } else {
            buf.put(0);
        }
        //array
        buf.put(info.array);
        //boundary
        buf.put(info.boundary);
        if (info.boundary) {
            if (info.upper != null && !info.upper.isEmpty()) {
                buf.put(true);
                buf.put(info.upper.size());
                for (var i : info.upper) {
                    serialize(i, buf);
                }
            } else {
                buf.put(false);
            }
            if (info.lower != null) {
                buf.put(true);
                buf.put(info.lower.size());
                for (var i : info.lower) {
                    serialize(i, buf);
                }
            } else {
                buf.put(false);
            }
        }
    }

    /**
     * Write to binary present.
     */
    public static byte[] serialize(TypeInfo info) {
        var buf = Bytes.write(new byte[1024], 0, 256);
        serialize(info, buf);
        return Arrays.copyOf(buf.buf(), buf.index());
    }

    /**
     * From a byte array.
     */
    public static TypeInfo deserialize(byte[] buf) {
        return deserialize(Bytes.read(buf));
    }

    /**
     * From a Base64 binary.
     *
     * @param base64 base64 std encoded string
     */
    public static TypeInfo deserialize(String base64) {
        return deserialize(Bytes.read(Base64.getDecoder().decode(base64)));
    }

    public static TypeInfo from(java.lang.reflect.Type info) {
        var b = TypeInfo.builder().name("");
        if (info instanceof Class<?> cls) {
            b.array(cls.isArray())
                    .typeClass(new LazyClass(cls));
        } else if (info instanceof ParameterizedType p) {
            var raw = p.getRawType();
            b.parameterized(true);
            b.type(from(raw));
            var lst = new ArrayList<TypeInfo>();
            for (var argument : p.getActualTypeArguments()) {
                lst.add(from(argument));
            }
            b.typeArguments(lst);
        } else if (info instanceof GenericArrayType p) {
            var raw = p.getGenericComponentType();
            b.array(true);
            b.type(from(raw));
        } else if (info instanceof TypeVariable<?> p) {
            b.name(p.getName());
            var boundary = p.getBounds().length > 0;
            b.boundary(boundary);
            if (boundary) {
                var lst = new ArrayList<TypeInfo>();
                for (var bound : p.getBounds()) {
                    lst.add(from(bound));
                }
                b.upper(lst);
            } else {
                b.typeClass(new LazyClass(Object.class));
            }
        } else if (info instanceof WildcardType p) {
            b.name("*");
            var boundary = p.getLowerBounds().length > 0 || p.getUpperBounds().length > 0;
            b.boundary(boundary);
            if (boundary) {
                {
                    var lst = new ArrayList<TypeInfo>();
                    for (var bound : p.getUpperBounds()) {
                        lst.add(from(bound));
                    }
                    b.upper(lst);
                }
                {
                    var lst = new ArrayList<TypeInfo>();
                    for (var bound : p.getLowerBounds()) {
                        lst.add(from(bound));
                    }
                    b.upper(lst);
                }
            } else {
                b.typeClass(LazyClass.of(Object.class));
            }
        } else {
            throw new IllegalStateException("unknown type to process: " + info);
        }
        return b.build().flatten();
    }

    /**
     * @param classTypeField the field may have a {@link Info.Type}
     * @return empty if not found
     */
    public static Optional<TypeInfo> visit(Field classTypeField) {
        var a = classTypeField.getAnnotationsByType(Info.Type.class);
        if (a.length == 0) return Optional.empty();
        return Optional.of(deserialize(a[0].value()));
    }

    /**
     * Create from an APT TypeMirror.
     *
     * @param info TypeMirror
     * @param env  processing environment.
     */
    @SneakyThrows
    public static TypeInfo from(TypeMirror info, ProcessingEnvironment env) {
        var b = TypeInfo.builder().name("");
        switch (info.getKind()) {
            case BOOLEAN -> b.typeClass(LazyClass.of(Boolean.TYPE));
            case BYTE -> b.typeClass(LazyClass.of(Byte.TYPE));
            case SHORT -> b.typeClass(LazyClass.of(Short.TYPE));
            case INT -> b.typeClass(LazyClass.of(Integer.TYPE));
            case LONG -> b.typeClass(LazyClass.of(Long.TYPE));
            case CHAR -> b.typeClass(LazyClass.of(Character.TYPE));
            case FLOAT -> b.typeClass(LazyClass.of(Float.TYPE));
            case DOUBLE -> b.typeClass(LazyClass.of(Double.TYPE));
            case VOID -> b.typeClass(LazyClass.of(Void.TYPE));
            case ARRAY -> {
                var a = (ArrayType) info;
                b.array(true).type(from(a.getComponentType(), env));
            }
            case DECLARED -> {
                var a = (DeclaredType) info;
                var raw = (TypeElement) a.asElement();
                if (!env.getTypeUtils().isSameType(raw.asType(), info)) {
                    b.type(from(raw.asType(), env));
                } else {
                    b.typeClass(LazyClass.of(typeElementToClassName(raw)));
                    return b.build();//not process nested type argument for have agreement as from(java.lang.reflect.Type)
                }
                var args = a.getTypeArguments();
                var parameterized = args != null && !args.isEmpty();
                if (parameterized) {
                    b.parameterized(true);
                    var lst = new ArrayList<TypeInfo>();
                    for (var arg : args) {
                        lst.add(from(arg, env));
                    }
                    b.typeArguments(lst);
                }
            }
            case TYPEVAR -> {
                var a = (javax.lang.model.type.TypeVariable) info;
                b.name(a.asElement().getSimpleName().toString());
                //b.type(from(a.asElement().asType(),env));
                var u = a.getUpperBound();
                var l = a.getLowerBound();
                var boundary = false;
                if (u instanceof IntersectionType i) {
                    boundary = true;
                    var lst = new ArrayList<TypeInfo>();
                    for (var bound : i.getBounds()) {
                        lst.add(from(bound, env));
                    }
                    b.upper(lst);
                }
                if (l instanceof IntersectionType i) {
                    boundary = true;
                    var lst = new ArrayList<TypeInfo>();
                    for (var bound : i.getBounds()) {
                        lst.add(from(bound, env));
                    }
                    b.lower(lst);
                }
                b.boundary(boundary);
                if (!boundary) b.typeClass(LazyClass.OBJECT);
            }
            case WILDCARD -> {
                var a = (javax.lang.model.type.WildcardType) info;
                var u = a.getExtendsBound();
                var l = a.getSuperBound();
                var boundary = u != null || l != null;
                if (boundary) {
                    if (u != null) {
                        b.upper(List.of(from(u, env)));
                    }
                    if (l != null) {
                        b.lower(List.of(from(l, env)));
                    }
                }
                b.typeClass(LazyClass.OBJECT);
            }
            default -> throw new IllegalStateException("unknown javax type:" + info);
        }
        return b.build().flatten();
    }

    /**
     * @param e type element
     * @return same format as Class#getName()
     */
    public static String typeElementToClassName(TypeElement e) {
        var s = new ArrayList<String>();
        var last = (TypeElement) e;
        var temp = (Element) null;
        while ((temp = e.getEnclosingElement()) != null && temp instanceof TypeElement t) {
            if (t == last) break;
            s.add(last.getSimpleName().toString());
            last = t;
        }
        var b = new StringBuilder(last.getQualifiedName().toString());
        s.forEach(x -> b.append('$').append(x));
        return b.toString();
    }
}