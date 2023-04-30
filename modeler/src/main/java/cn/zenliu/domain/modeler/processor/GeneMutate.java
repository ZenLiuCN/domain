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
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Desc("generator for Gene.Mutate")
@ApiStatus.AvailableSince("0.1.2")
public class GeneMutate extends BaseFileProcessor {
    public static final String SUFFIX = "Mutate";

    public GeneMutate() {
        super(Gene.Mutate.class);
    }

    protected Pair<TypeSpec.Builder, TypeName> makeType(boolean isGeneric, boolean isTrait, TypeElement t) {
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
                .addModifiers(Modifier.PUBLIC);
        type = isTrait ? type.addSuperinterface(Meta.Trait.class) : type.addSuperinterface(t.asType());
        type.addAnnotation(generated());
        var pkg=ClassName.get(t).packageName();
        TypeName typeName = ClassName.get(pkg, t.getSimpleName() + SUFFIX);
        if (isGeneric) {
            typeName = ParameterizedTypeName.get((ClassName) typeName, t.getTypeParameters().stream()
                    .map(TypeVariableName::get)
                    .toArray(TypeVariableName[]::new));
        }
        return new Pair<>(type, typeName);
    }

    protected static final String TARGET = "@Gene.Mutate";

    @SneakyThrows
    protected JavaFile processElement(Element ele, RoundEnvironment roundEnv, ProcUtil u) {
        var c = this.preCheck(ele, u);
        if (c == null) return null;
        if (ele instanceof TypeElement t) {
            if (notInterface(u, TARGET, t)) return null;
            if (notDirectInherit(u, TARGET, t, Meta.Entity.class)) return null;
            if (!mustInheritOneOf(u, TARGET, t, Meta.Object.class, Meta.Trait.class)) return null;
            var pair = makeType(!t.getTypeParameters().isEmpty(), u.isAssignable(t.asType(), Meta.Trait.class), t);
            return JavaFile.builder(
                            u.elements().getPackageOf(ele).getQualifiedName().toString(),
                            t.accept(new SetterGeneVisitor(
                                                    u,
                                                    c.readBoolean(prefix + "chain").orElse(false),
                                                    pair.v1()),
                                            pair.v0())
                                    .build())
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
