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

import cn.zenliu.domain.modeler.annotation.Gene;
import cn.zenliu.domain.modeler.prototype.Meta;
import com.squareup.javapoet.*;
import lombok.SneakyThrows;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Desc("generator for Gene.Entity")
public class GeneEntity extends BaseFileProcessor {
    public final String SUFFIX = "Entity";

    public GeneEntity() {
        super(Gene.Entity.class);
    }

    protected TypeSpec.Builder makeType(boolean isGeneric, boolean isInheritedEntity, TypeElement t) {
        var type = (
                isGeneric
                        ? TypeSpec
                        .interfaceBuilder(t.getSimpleName() + "Entity")
                        .addTypeVariables(t.getTypeParameters().stream()
                                .map(TypeVariableName::get)
                                .toList())
                        : TypeSpec
                        .interfaceBuilder(t.getSimpleName() + "Entity")

        )
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(t.asType());
        type = isInheritedEntity ? type : type.addSuperinterface(Meta.Entity.class);
        return type.addAnnotation(generated());
    }

    protected TypeSpec.Builder makeInheritMutateType(boolean isGeneric, boolean isInheritedEntity, TypeElement t) {
        final TypeName mut;
        if (isGeneric) {
            var cls = (ParameterizedTypeName) ParameterizedTypeName.get(t.asType());
            var pkg = cls.rawType.packageName();
            mut = ParameterizedTypeName.get(ClassName.get(pkg, t.getSimpleName() + GeneMutate.SUFFIX)
                    , cls.typeArguments.toArray(TypeName[]::new));

        } else {
            var cls = ClassName.get(t);
            var pkg = cls.packageName();
            mut = ClassName.get(pkg, t.getSimpleName() + GeneMutate.SUFFIX);
        }
        var type = (
                isGeneric
                        ? TypeSpec
                        .interfaceBuilder(t.getSimpleName() + SUFFIX)
                        .addTypeVariables(t.getTypeParameters().stream()
                                .map(TypeVariableName::get)
                                .toList())
                        : TypeSpec
                        .interfaceBuilder(t.getSimpleName() + SUFFIX)

        )
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(t.asType())
                .addSuperinterface(mut);
        type = isInheritedEntity ? type : type.addSuperinterface(Meta.Entity.class);
        return type.addAnnotation(generated());
    }

    protected static final String TARGET = "@Gene.Entity";

    @SneakyThrows
    protected JavaFile processElement(Element ele, RoundEnvironment roundEnv, ProcUtil u) {
        var c = this.preCheck(ele, u);
        if (c == null) return null;
        if (ele instanceof TypeElement t) {
            if (!mustInterface(u, TARGET, t)) return null;
            if (!mustNotDirectInherit(u, TARGET, t, Meta.Entity.class)) return null;
            if (!mustInherit(u, TARGET, t, Meta.Object.class)) return null;
            var isInheritedEntity = u.isAssignable(t.asType(), Meta.Entity.class);
            var isGeneric = !t.getTypeParameters().isEmpty();
            return JavaFile.builder(
                            u.elements().getPackageOf(ele).getQualifiedName().toString(),
                            isAnnotated(t, Gene.Mutate.class) ?
                                    makeInheritMutateType(isGeneric, isInheritedEntity, t).build() :
                                    t.accept(new SetterGeneVisitor(u, c.readBoolean(prefix + "chain").orElse(false)), makeType(isGeneric, isInheritedEntity, t)).build())
                    .build();
        }
        u.warn(this, "{} not a valid target of {}", ele, TARGET);
        return null;
    }


    @Override
    protected AbstractProcessor self() {
        return this;
    }


}
