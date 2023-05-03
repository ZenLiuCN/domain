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
import cn.zenliu.domain.modeler.processor.safer.Pair;
import cn.zenliu.domain.modeler.prototype.Meta;
import com.squareup.javapoet.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Desc("generator for Gene.Entity")
@ApiStatus.AvailableSince("0.1.2")
public class GeneEntity extends BaseFileProcessor {

    public GeneEntity() {
        super(Gene.Entity.class);
    }

    protected Pair<TypeSpec.Builder, TypeName> makeType(boolean isGeneric,
                                                        boolean isInheritedEntity,
                                                        boolean useObjectStyle,
                                                        TypeElement t) {
        var name = t.getSimpleName().toString();
        if (name.endsWith(Meta.Trait.SUFFIX)) name = name.replace(Meta.Trait.SUFFIX, "");
        if (name.endsWith(Meta.Trait.MUTABLE_SUFFIX)) name = name.replace(Meta.Trait.MUTABLE_SUFFIX, "");
        name += Meta.Entity.SUFFIX;
        var type = (
                isGeneric
                        ? TypeSpec
                        .interfaceBuilder(name)
                        .addTypeVariables(t.getTypeParameters().stream()
                                .map(TypeVariableName::get)
                                .toList())
                        : TypeSpec
                        .interfaceBuilder(name)

        )
                .addAnnotation(generated())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(t.asType());
        type = isInheritedEntity ? type : type.addSuperinterface(useObjectStyle ? Meta.ObjectStyleEntity.class : Meta.Entity.class);

        return new Pair<>(type, lookupTypeName(t, name, isGeneric));
    }

    protected Pair<TypeSpec.Builder, TypeName> makeInheritMutateType(boolean isGeneric,
                                                                     boolean isInheritedEntity,
                                                                     boolean useObjectStyle,
                                                                     TypeElement t) {
        var name = t.getSimpleName().toString();
        if (name.endsWith(Meta.Trait.SUFFIX)) name = name.replace(Meta.Trait.SUFFIX, "");
        if (name.endsWith(Meta.Trait.MUTABLE_SUFFIX)) name = name.replace(Meta.Trait.MUTABLE_SUFFIX, "");
        final TypeName mut;
        if (isGeneric) {
            var cls = (ParameterizedTypeName) ParameterizedTypeName.get(t.asType());
            var pkg = cls.rawType.packageName();
            mut = ParameterizedTypeName.get(ClassName.get(pkg, name + Meta.Trait.MUTABLE_SUFFIX)
                    , cls.typeArguments.toArray(TypeName[]::new));

        } else {
            var cls = ClassName.get(t);
            var pkg = cls.packageName();
            mut = ClassName.get(pkg, name + Meta.Trait.MUTABLE_SUFFIX);
        }
        name += Meta.Entity.SUFFIX;
        var type = (
                isGeneric
                        ? TypeSpec
                        .interfaceBuilder(name)
                        .addTypeVariables(t.getTypeParameters().stream()
                                .map(TypeVariableName::get)
                                .toList())
                        : TypeSpec
                        .interfaceBuilder(name)

        )
                .addAnnotation(generated())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(t.asType())
                .addSuperinterface(mut);
        type = isInheritedEntity ? type : type.addSuperinterface(useObjectStyle ? Meta.ObjectStyleEntity.class : Meta.Entity.class);
        return new Pair<>(type, lookupTypeName(t, name, isGeneric));
    }

    protected static final String TARGET = "@Gene.Entity";

    @SneakyThrows
    protected List<JavaFile> processElement(Element ele, RoundEnvironment roundEnv, ProcUtil u) {
        var c = this.preCheck(ele, u);
        if (c == null) return null;
        if (ele instanceof TypeElement t) {
            if (notInterface(u, TARGET, t)) return null;
            if (notDirectInherit(u, TARGET, t, Meta.Entity.class)) return null;
            if (notInherit(u, TARGET, t, Meta.Object.class)) return null;
            var isInheritedEntity = u.isAssignableTo(t.asType(), Meta.Entity.class);
            var isGeneric = !t.getTypeParameters().isEmpty();
            var isMutate = isAnnotated(t, Gene.Mutate.class);
            var pair = isMutate ?
                    makeInheritMutateType(isGeneric, isInheritedEntity, c.readBoolean(prefix + "object").orElse(false), t)
                    : makeType(isGeneric, isInheritedEntity, c.readBoolean(prefix + "object").orElse(false), t);
            return List.of(JavaFile.builder(
                            u.elements().getPackageOf(ele).getQualifiedName().toString(),
                            t.accept(new SetterGeneVisitor(u,
                                            c.readBoolean(prefix + "chain").orElse(false),
                                            c.readBoolean(prefix + "bean").orElse(true),
                                            pair.v1()),
                                    pair.v0()).build())
                    .build());
        }
        u.warn(this, "{} not a valid target of {}", ele, TARGET);
        return null;
    }


    @Override
    protected AbstractProcessor self() {
        return this;
    }


}
