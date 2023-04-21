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
import cn.zenliu.domain.modeler.annotation.Generated;
import cn.zenliu.domain.modeler.annotation.Mode;
import cn.zenliu.domain.modeler.prototype.Meta;
import com.squareup.javapoet.*;
import lombok.SneakyThrows;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;
import java.util.List;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
public class EntityProcessor extends BaseFileProcessor {

    @Override
    protected void config() {

    }

    @Override
    protected String name() {
        return "EntityProcessor";
    }

    @Override
    protected String desc() {
        return "Generator for cn.zenliu.domains.annotation.Generated.Entity";
    }

    public EntityProcessor() {
        super(Gene.Entity.class);
    }

    static TypeMirror superEntity(TypeElement type, ProcUtil u) {
        for (var face : type.getInterfaces()) {
            if (u.isAssignable(face, Meta.Entity.class)) {
                return face;
            }
        }
        return null;
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
        return type.addAnnotation(AnnotationSpec.builder(Generated.class)
                .addMember("processor", "$S", name())
                .addMember("version", "$S", pomVersion())
                .addMember("timestamp", "$LL", System.currentTimeMillis())
                .build());
    }

    @SneakyThrows
    protected JavaFile processElement(Element ele, RoundEnvironment roundEnv, ProcUtil u) {
        if (u.isGeneratedBy(ele, name())) {
            return null;
        }
        var c = Configurer.resolve(u, ele);
        if (!c.isEnabled()) {
            if (c.isDebug()) u.warn("disabled when process ", ele);
            return null;
        }
        if (ele instanceof TypeElement t) {

            var isEntity = u.isDirectlyInherit(t, Meta.Entity.class);
            var isInheritedEntity = u.isAssignable(t.asType(), Meta.Entity.class);
            var isObject = u.isAssignable(t.asType(), Meta.Object.class);
            var isGeneric = !t.getTypeParameters().isEmpty();
            var isInterface = t.getKind().isInterface();
            if (!isEntity && isInterface && isObject) {
                return JavaFile.builder(
                                u.elements().getPackageOf(ele).getQualifiedName().toString(),
                                t.accept(new Visitor(u, c.readBoolean(prefix + "chain").orElse(false)),
                                                makeType(isGeneric, isInheritedEntity, t))
                                        .build())
                        .build();
            } else if (!isEntity) {
                if (!isInterface) {
                    u.warn("type {} not a valid target of @Generated.Entity, only interface supported", ele);
                } else {
                    u.warn("type {} not a valid target of @Generated.Entity, not inherited from Meta.Object", ele);
                }
                return null;
            }
        }
        u.warn("{} not a valid target of @Generated.Entity", ele.toString());
        return null;
    }

    static class Visitor extends ElementScanner14<TypeSpec.Builder, TypeSpec.Builder> {
        private final ProcUtil u;
        private final boolean chain;
        private boolean haveTypeField = false;
        private TypeMirror root;

        Visitor(ProcUtil u, boolean chain) {
            this.u = u;
            this.chain = chain;
        }


        @Override
        public TypeSpec.Builder visitType(TypeElement e, TypeSpec.Builder builder) {
            if (root == null) {
                haveTypeField = e.getAnnotation(Mode.Field.class) != null;
                root = e.asType();
                for (Element ex : u.elements().getAllMembers(e)) {
                    if (ex.getKind() == ElementKind.METHOD) {
                        builder = ex.accept(this, builder);
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
        public TypeSpec.Builder visitExecutable(ExecutableElement e, TypeSpec.Builder builder) {
            //not getter
            if (
                    e.getModifiers().contains(Modifier.STATIC) ||
                    e.getModifiers().contains(Modifier.DEFAULT) ||
                    e.getParameters().size() != 0 ||
                    e.getReturnType().getKind() == TypeKind.VOID ||
                    u.isSameType(e.getEnclosingElement().asType(), Object.class) ||
                    e.getAnnotation(Mode.ReadOnly.class) != null

            ) {
                u.other("Ignore Object method: {}", e);
                return builder;
            }
            final String n;
            //no name check
            if (haveTypeField || e.getAnnotation(Mode.Field.class) != null) {
                n = u.getterToSetter(e.getSimpleName(), false);
                assert n != null : "not getter, should never happened " + e.getSimpleName();
            } else {
                n = u.getterToSetter(e.getSimpleName(), true);
                if (n == null) {
                    u.other("Ignore none bean style getter for Object method: {}", e);
                    return builder;
                }
            }
            if (u.declaredMethod(root, n, List.of(TypeName.get(e.getReturnType())))) {
                u.other("Ignore already declared setter for Object method: {}", e);
                return builder;
            }
            u.other("Generate Setter from Object method: {}", e);
            return builder.addMethod(
                    chain ?
                            MethodSpec.methodBuilder(n)
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .returns(TypeName.get(root))
                                    .addParameter(ParameterSpec.builder(TypeName.get(e.getReturnType()), "v").build())
                                    .build()
                            : MethodSpec.methodBuilder(n)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(ParameterSpec.builder(TypeName.get(e.getReturnType()), "v").build())
                            .build()
            );
        }

        @Override
        public TypeSpec.Builder visitTypeParameter(TypeParameterElement e, TypeSpec.Builder builder) {
            return builder;
        }
    }

}
