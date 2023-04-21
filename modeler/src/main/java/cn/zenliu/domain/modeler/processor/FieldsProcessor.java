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
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
public class FieldsProcessor extends BaseFileProcessor {


    public FieldsProcessor() {
        super(Gene.Fields.class, Gene.Entity.class);
    }

    @Override
    protected void config() {

    }

    @Override
    protected String name() {
        return "FilesProcessor";
    }

    @Override
    protected String desc() {
        return "Generator for cn.zenliu.domains.annotation.Generated.Fields";
    }

    private final Set<String> processed = new HashSet<>();

    @Nullable
    @Override
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
            var isEntity = u.isAssignable(t.asType(), Meta.Entity.class);
            var isObject = u.isAssignable(t.asType(), Meta.Object.class);
            var isInterface = t.getKind().isInterface();
            if (!isEntity && isInterface && isObject) {
                var name = String.join(".", u.elements().getPackageOf(ele).getQualifiedName().toString(), t.getSimpleName() + "Fields");
                if (!processed.add(name)) return null;
                return JavaFile.builder(
                                u.elements().getPackageOf(ele).getQualifiedName().toString(),
                                t.accept(new Visitor(u),
                                                TypeSpec
                                                        .interfaceBuilder(t.getSimpleName() + "Fields")
                                                        .addModifiers(Modifier.PUBLIC)
                                                        .addSuperinterface(Meta.Fields.class)
                                                        .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                                .addMember("processor", "$S", name())
                                                                .addMember("version", "$S", pomVersion())
                                                                .addMember("timestamp", "$LL", System.currentTimeMillis())
                                                                .build())
                                        )
                                        .build())
                        .build();
            } else if (!isEntity) {
                if (!isInterface) {
                    u.warn("type {} not a valid target of @Generated.Fields, only interface supported", ele);
                } else {
                    u.warn("type {} not a valid target of @Generated.Fields, not inherited from Meta.Object", ele);
                }
                return null;
            }
        }
        u.warn("{} not a valid target of @Generated.Fields", ele.toString());
        return null;
    }

    static class Visitor extends ElementScanner14<TypeSpec.Builder, TypeSpec.Builder> {
        private final ProcUtil u;
        private boolean haveTypeField = false;
        private TypeMirror root;

        Visitor(ProcUtil u) {
            this.u = u;
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
                    u.isSameType(e.getEnclosingElement().asType(), Object.class)

            ) {
                u.other("Ignore Object method: {}", e);
                return builder;
            }
            final String n;

            //no name check
            if (haveTypeField || e.getAnnotation(Mode.Field.class) != null) {
                n = u.getterToField(e.getSimpleName(), false);
                assert n != null : "not getter, should never happened " + e.getSimpleName();
            } else {
                n = u.getterToField(e.getSimpleName(), true);
                if (n == null) {
                    u.other("Ignore none bean style getter for Object method: {}", e);
                    return builder;
                }
            }

            u.other("Generate Field from Object method: {}", e);
            return builder
                    .addField(FieldSpec.builder(String.class, n.toUpperCase(),
                                    Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$S", n).build())
                    .addField(FieldSpec.builder(Class.class, n.toUpperCase() + Meta.Fields.TYPE_SUFFIX,
                                    Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$L.class", u.toClassName(e.getReturnType())).build());
        }

        @Override
        public TypeSpec.Builder visitTypeParameter(TypeParameterElement e, TypeSpec.Builder builder) {
            return builder;
        }
    }
}
