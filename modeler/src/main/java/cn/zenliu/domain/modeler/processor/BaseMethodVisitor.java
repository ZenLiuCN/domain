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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;

/**
 * @author Zen.Liu
 * @since 2023-04-22
 */
@ApiStatus.AvailableSince("0.1.2")
public abstract class BaseMethodVisitor<P> extends ElementScanner14<P, P> {
    protected boolean haveTypeField = false;
    protected final boolean beanStyle;
    protected TypeMirror root;
    protected final ProcUtil u;

    protected BaseMethodVisitor(boolean beanStyle, ProcUtil u) {
        this.beanStyle = beanStyle;
        this.u = u;
    }

    @Override
    public P visitType(TypeElement e, P builder) {
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
    public P visitVariable(VariableElement e, P builder) {
        return builder;
    }

    @Override
    public P visitTypeParameter(TypeParameterElement e, P builder) {
        return builder;
    }

    protected boolean notInstanceMethod(ExecutableElement e) {
        return u.isStatic(e) || u.isDefault(e);
    }

    protected boolean notGetterLikeMethod(ExecutableElement e) {
        return !u.isGetter(e, beanStyle);
    }

    protected boolean isObjectMethod(ExecutableElement e) {
        return u.isDeclaredBy(e, Object.class);
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
            var n = u.getterToSetter(e.getSimpleName(), beanStyle);
            assert n != null : "not getter, should never happened " + e.getSimpleName();
            return n;
        }
        var n = u.getterToSetter(e.getSimpleName(), true);
        if (n == null) u.other("Ignore none bean style getter for method: {}", e);
        return n;
    }

    protected boolean hasDeclaredSetter(String methodName, ExecutableElement e) {
        var s = u.hasDeclared(methodName, e, root);
        if (s) u.other("Ignore already declared setter for method: {}", e);
        return s;
    }


    protected MethodSpec.Builder declareSetter(String methodName, ExecutableElement e) {
        var re = u.typeElementOf(root);
        var retType = u.resolveTypeName(e.getReturnType(), e, re);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterSpec.builder(retType, "v").build());
    }

    protected FieldSpec.Builder declareStaticFinalField(Class<?> type, String name) {
        return FieldSpec.builder(type, name, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
    }

    /**
     * @param e               executable
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
