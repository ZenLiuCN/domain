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
import cn.zenliu.domain.modeler.annotation.Info;
import cn.zenliu.domain.modeler.prototype.Meta;
import cn.zenliu.domain.modeler.util.TypeInfo;
import com.google.common.io.ByteStreams;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Array;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Desc("generator for Gene.Fields")
@ApiStatus.AvailableSince("0.1.2")
public class GeneFields extends BaseFileProcessor {


    public GeneFields() {
        super(Gene.Fields.class, Gene.Entity.class);
    }


    private final Set<String> processed = new HashSet<>();
    protected final static String TARGET = "@Gene.Fields";

    @Nullable
    @Override
    protected JavaFile processElement(Element ele, RoundEnvironment roundEnv, ProcUtil u) {
        var c = this.preCheck(ele, u);
        if (c == null) return null;

        if (ele instanceof TypeElement t) {
            if (notInterface(u, TARGET, t)) return null;
            if (notInherit(u, TARGET, t, Meta.Object.class)) return null;
            var name = String.join(".", u.elements().getPackageOf(ele).getQualifiedName().toString(), t.getSimpleName() + "Fields");
            if (!processed.add(name)) return null;
            return JavaFile.builder(
                            u.elements().getPackageOf(ele).getQualifiedName().toString(),
                            t.accept(new Visitor(u),
                                            TypeSpec
                                                    .interfaceBuilder(t.getSimpleName() + "Fields")
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addSuperinterface(Meta.Fields.class)
                                                    .addAnnotation(generated())
                                    )
                                    .build())
                    .build();
        }
        u.warn("{} not a valid target of {}", ele.toString(), TARGET);
        return null;
    }

    @Override
    protected AbstractProcessor self() {
        return this;
    }

    static class Visitor extends BaseGetterVisitor {
        Visitor(ProcUtil u) {
            super(u);
        }

        @Override
        public TypeSpec.Builder visitExecutable(ExecutableElement e, TypeSpec.Builder builder) {
            //not getter
            if (
                    notInstanceMethod(e) ||
                            isObjectMethod(e) ||
                            notGetterLikeMethod(e)

            ) {
                u.other("Ignore Object method: {}", e);
                return builder;
            }
            final String n = toSetterName(e);
            if (n == null) {
                u.other("Ignore none bean style getter for Object method: {}", e);
                return builder;
            }
            u.other("Generate Field from Object method: {}", e);
            var ret = e.getReturnType();
            var typeName = u.toTypeName(ret);
            TypeInfo info=null;
            String cn;
            if (typeName instanceof ClassName c) {
                cn = c.toString();
            } else if (typeName instanceof ParameterizedTypeName p) {
                cn = p.rawType.toString();
                info=TypeInfo.from(ret, u.env());
            } else if (typeName instanceof ArrayTypeName p) {
                cn = p.toString();
            } else {
                cn = typeName.toString();
            }
            if(info!=null){
                var b=new StringJoiner(",","{","}");
                for(var by:TypeInfo.serialize(info)) b.add("0x"+Integer.toHexString(by));
                return builder
                        .addField(declareStaticFinalField(String.class, n.toUpperCase()).initializer("$S", n).build())
                        .addField(declareStaticFinalField(Class.class, n.toUpperCase() + Meta.Fields.TYPE_SUFFIX)
                                .addAnnotation(AnnotationSpec.builder(Info.Type.class)
                                        .addMember("value","$L", b)
                                        .build())
                                .initializer("$L.class", cn).build());
            }
            return builder
                    .addField(declareStaticFinalField(String.class, n.toUpperCase()).initializer("$S", n).build())
                    .addField(declareStaticFinalField(Class.class, n.toUpperCase() + Meta.Fields.TYPE_SUFFIX)
                            .initializer("$L.class", cn).build());
        }


    }
}
