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

import cn.zenliu.domain.modeler.annotation.Mode;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;
import java.util.List;

/**
 * @author Zen.Liu
 * @since 2023-04-22
 */
public abstract class BaseGetterVisitor extends ElementScanner14<TypeSpec.Builder, TypeSpec.Builder> {
    protected boolean haveTypeField = false;
    protected TypeMirror root;
    protected final ProcUtil u;

    protected BaseGetterVisitor(ProcUtil u) {
        this.u = u;
    }

    @Override
    public TypeSpec.Builder visitType(TypeElement e, TypeSpec.Builder builder) {
        if (root == null) {
            haveTypeField = e.getAnnotation(Mode.Field.class) != null;
            root = e.asType();
            for (var ex : u.elements().getAllMembers(e)) {
                if (ex.getKind() == ElementKind.METHOD) {
                    var b = ex.accept(this, builder);
                    if (b != null) builder = b;
                }
            }
            return builder;
        }
        return builder;
    }

    @Override
    public TypeSpec.Builder visitVariable(VariableElement e, TypeSpec.Builder builder) {
        return builder;
    }

    @Override
    public TypeSpec.Builder visitTypeParameter(TypeParameterElement e, TypeSpec.Builder builder) {
        return builder;
    }

    protected boolean notInstanceMethod(ExecutableElement e) {
        return (e.getModifiers().contains(Modifier.STATIC) || e.getModifiers().contains(Modifier.DEFAULT));
    }

    protected boolean notGetterLikeMethod(ExecutableElement e) {
        return (e.getParameters().size() != 0 || e.getReturnType().getKind() == TypeKind.VOID);
    }

    protected boolean isObjectMethod(ExecutableElement e) {
        return u.isSameType(e.getEnclosingElement().asType(), Object.class);
    }

    protected boolean isReadyOnly(ExecutableElement e) {
        return e.getAnnotation(Mode.ReadOnly.class) != null;
    }

    /**
     * not check if Getter or Setter
     */
    protected boolean isFluentStyle(ExecutableElement e) {
        return haveTypeField || e.getAnnotation(Mode.Field.class) != null;
    }

    /**
     * @return getter name to setter name
     */
    protected @Nullable String toSetterName(ExecutableElement e) {
        if (isFluentStyle(e)) {
            var n = u.getterToSetter(e.getSimpleName(), false);
            assert n != null : "not getter, should never happened " + e.getSimpleName();
            return n;
        }
        var n = u.getterToSetter(e.getSimpleName(), true);
        if (n == null) u.other("Ignore none bean style getter for method: {}", e);
        return n;
    }

    protected boolean hasDeclaredSetter(String methodName, ExecutableElement e) {
        var s = u.declaredMethod(root, methodName, List.of(TypeName.get(e.getReturnType())));
        if (s) u.other("Ignore already declared setter for method: {}", e);
        return s;
    }

    protected MethodSpec.Builder declareSetter(String methodName, ExecutableElement e) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterSpec.builder(TypeName.get(e.getReturnType()), "v").build());
    }

    protected FieldSpec.Builder declareStaticFinalField(Class<?> type, String name) {
        return FieldSpec.builder(type, name, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
    }

    /**
     * @param e executable
     * @param ignoreReadyOnly not allow {@link Mode.ReadOnly} as a Getter
     * @return true:  not a getter
     */
    protected boolean notGetter(ExecutableElement e, boolean ignoreReadyOnly) {
        if (
                notInstanceMethod(e) ||
                isObjectMethod(e) ||
                notGetterLikeMethod(e) ||
                (ignoreReadyOnly && isReadyOnly(e))

        ) {
            u.other("ignore method: {}", e);
            return true;
        }
        return false;
    }
}
