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
import cn.zenliu.domain.modeler.processor.safer.Configurer;
import cn.zenliu.domain.modeler.processor.safer.Pair;
import cn.zenliu.domain.modeler.prototype.Meta;
import cn.zenliu.domain.modeler.util.Projections;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementScanner14;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.zenliu.domain.modeler.processor.GeneAdaptor.GeneContext.ModelMethodVisitor.MethodBuildContext.CODES.*;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Desc("generator for Gene.Adapt")
@ApiStatus.AvailableSince("0.2.2")
public class GeneAdaptor extends BaseFileProcessor {


    public GeneAdaptor() {
        super(Gene.Adapt.class);
    }

    protected final static String TARGET = "@Gene.Adapt";

    static class Mappings {
        final Map<Pair<TypeMirror, TypeMirror>, ExecutableElement> mappings;

        Mappings(List<TypeElement> m, ProcUtil u) {
            if (m == null || m.isEmpty()) {
                mappings = Map.of();
            } else
                mappings = m.stream()
                        .flatMap(x -> u.allMembers(x).stream())
                        .filter(ExecutableElement.class::isInstance)
                        .map(ExecutableElement.class::cast)
                        .filter(x -> u.isDefault(x)
                                && x.getParameters().size() == 1
                                && x.getReturnType().getKind() != TypeKind.VOID
                        ).collect(Collectors.toMap(
                                x -> new Pair<>(x.getParameters().get(0).asType(), x.getReturnType()),
                                Function.identity()));

        }

        boolean isEmpty() {
            return mappings.isEmpty();
        }

        Optional<ExecutableElement> get(TypeMirror from, TypeMirror to) {
            return Optional.ofNullable(mappings.get(new Pair<>(from, to)));
        }
    }

    @Nullable
    @Override
    protected List<JavaFile> processElement(Element ele, RoundEnvironment roundEnv, ProcUtil u) {
        var c = this.preCheck(ele, u);
        if (c == null) return null;

        if (ele instanceof TypeElement t) {
            var genClass = c.readBoolean(prefix + "class").orElse(false);
            var genInterface = c.readBoolean(prefix + "interface").orElse(true);
            if (!genClass && !genInterface) {
                u.error("generate {} for class adapter and interface adaptor both are disabled", t);
                return null;
            }
            if (isInterface(u, TARGET, t)) return null;
            var a = u.annotationByType(t, Gene.Adapt.class);
            if (a == null) return null;
            var tar = a.readClassElement("value");
            if (tar == null) return null;
            if (!tar.getKind().isInterface()) {
                u.error("{} annotated value {} is not a interface,adaptor can only generate for interface", t, tar);
                return null;
            }
            return new GeneContext(u, t, tar, a, c).build();

        }
        u.warn("{} not a valid target of {}", ele.toString(), TARGET);
        return null;
    }

    @Override
    protected AbstractProcessor self() {
        return this;
    }

    class GeneContext {
        final ProcUtil u;
        final TypeElement entity;
        final TypeElement model;
        final ProcUtil.Annotated anno;
        final Configurer.Config conf;


        GeneContext(ProcUtil u, TypeElement entity, TypeElement model, ProcUtil.Annotated anno, Configurer.Config conf) {
            this.u = u;
            this.entity = entity;
            this.model = model;
            this.anno = anno;
            this.conf = conf;
        }

        private Mappings mappings;

        private boolean isGeneric;
        private boolean checkInstance;
        private boolean genInterface;
        private boolean genClass;
        private String pkg;

        private List<TypeMirror> mixins;
        private Map<String, TypeMirror> modelTypeVariables;
        private List<TypeVariableName> entityTypeVariableNames;

        private ClassName genClassName;
        private ParameterizedTypeName genParamClassName;
        private ClassName genInterfaceName;
        private ParameterizedTypeName genParamInterfaceName;

        private boolean config() {
            mappings = new Mappings(anno.readClassElements("mapper"), u);
            mixins = anno.readClasses("mixins");
            var entityTypeParameters = entity.getTypeParameters().stream().map(TypeVariableName::get).toArray(TypeVariableName[]::new);
            Map<String, TypeMirror> entityTypeVariables = u.resolveTypeParameters(entity);
            modelTypeVariables = u.resolveTypeParameters(model);
            isGeneric = !entityTypeVariables.isEmpty();
            Map<String, TypeMirror> declaredTypeParameters = anno.readMap("names", "types",
                    ProcUtil.Annotated::mayString,
                    ProcUtil.Annotated::mayClass);
            checkInstance = conf.readBoolean(prefix + "instance").orElse(true);
            genInterface = conf.readBoolean(prefix + "interface").orElse(true);
            genClass = conf.readBoolean(prefix + "class").orElse(false);
            u.mergeTypeVariables(modelTypeVariables, entityTypeVariables, declaredTypeParameters);
            entityTypeVariableNames = isGeneric ? entity.getTypeParameters().stream()
                    .map(TypeVariableName::get)
                    .toList() : List.of();
            pkg = u.elements().getPackageOf(entity).getQualifiedName().toString();
            genClassName = !genClass ? null : ClassName.get(pkg, model.getSimpleName() + Meta.Adaptor.CLASS_SUFFIX);
            genInterfaceName = !genInterface ? null : ClassName.get(pkg, model.getSimpleName() + Meta.Adaptor.SUFFIX);
            genParamClassName = !genClass || !isGeneric ? null : ParameterizedTypeName.get(genClassName, entityTypeParameters);
            genParamInterfaceName = !genInterface || !isGeneric ? null : ParameterizedTypeName.get(genInterfaceName, entityTypeParameters);
            return true;
        }

        private Function<TypeMirror, TypeName> parameterConv(TypeName self, TypeElement selfType) {
            return x -> typeParameterConv(x, self, selfType);
        }

        private TypeName typeParameterConv(TypeMirror x, TypeName self, TypeElement selfType) {
            if (x instanceof TypeVariable ex) {
                var var = modelTypeVariables.get(ex.asElement().getSimpleName().toString());
                if (var != null) {
                    if (u.isSameType(var, Gene.Self.class) || u.isSameType(var, selfType.asType())) {
                        return self;
                    }
                    return TypeName.get(var);
                }
            }
            if (u.isSameType(x, Gene.Self.class) || u.isSameType(x, selfType.asType())) {
                return self;
            }
            if (x instanceof DeclaredType ex) {
                if (!ex.getTypeArguments().isEmpty()) {
                    var param = ex.getTypeArguments().stream()
                            .map(v -> {
                                if (v instanceof TypeVariable e0) {
                                    var var = modelTypeVariables.get(e0.asElement().getSimpleName().toString());
                                    if (var != null) {
                                        if (u.isSameType(var, Gene.Self.class) || u.isSameType(var, selfType.asType())) {
                                            return self;
                                        }
                                        return TypeName.get(var);
                                    }
                                }
                                return TypeName.get(v);
                            })
                            .toArray(TypeName[]::new);
                    return ParameterizedTypeName.get(ClassName.get((TypeElement) ex.asElement()), param);
                }
            }

            return TypeName.get(x);
        }

        private TypeSpec.Builder cls;

        private boolean buildClassType() {
            if (!genClass) return true;
            var parameterized = model.getTypeParameters().isEmpty()
                    ? TypeName.get(model.asType())
                    : ParameterizedTypeName.get(ClassName.get(model),
                    u.orderedParameterType(modelTypeVariables, model, parameterConv(isGeneric ? genParamClassName : genClassName, entity)));

            if (isGeneric) cls = TypeSpec.classBuilder(model.getSimpleName() + Meta.Adaptor.CLASS_SUFFIX)
                    .addAnnotation(generated())
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(parameterized)
                    .addSuperinterface(Meta.Adaptor.class)
                    .addTypeVariables(entityTypeVariableNames)
                    .addField(FieldSpec.builder(TypeName.get(entity.asType()), "entity", Modifier.PUBLIC, Modifier.FINAL).build())
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .addParameter(ParameterSpec.builder(TypeName.get(entity.asType()), "entity").build())
                            .addCode("this.entity=$T.requireNonNull(entity);", Objects.class)
                            .build())
                    .addMethod(MethodSpec.methodBuilder("of")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(isGeneric ? genParamClassName : genClassName)
                            .addParameter(TypeName.get(entity.asType()), "entity")
                            .addTypeVariables(entityTypeVariableNames)
                            .addCode("return new $T<>(entity);", genClassName)
                            .build());
            else
                cls = TypeSpec.classBuilder(model.getSimpleName() + Meta.Adaptor.CLASS_SUFFIX)
                        .addAnnotation(generated())
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(parameterized)
                        .addSuperinterface(Meta.Adaptor.class)
                        .addField(FieldSpec.builder(TypeName.get(entity.asType()), "entity", Modifier.PUBLIC, Modifier.FINAL).build())
                        .addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addParameter(ParameterSpec.builder(TypeName.get(entity.asType()), "entity").build())
                                .addCode("this.entity=$T.requireNonNull(entity);", Objects.class)
                                .build())
                        .addMethod(MethodSpec.methodBuilder("of")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .addParameter(TypeName.get(entity.asType()), "entity")
                                .addCode("return new $T(entity);", genClassName)
                                .build());
            if (mixins != null && !mixins.isEmpty())
                mixins.forEach(f -> cls.addSuperinterface(f));
            return true;
        }

        private TypeSpec.Builder face;


        private boolean buildInterfaceType() {
            if (!genInterface) return true;
            var parameterized = model.getTypeParameters().isEmpty() ?
                    TypeName.get(model.asType()) :
                    ParameterizedTypeName.get(ClassName.get(model),
                            u.orderedParameterType(modelTypeVariables, model, parameterConv(isGeneric ? genParamInterfaceName : genInterfaceName, entity)));
            if (isGeneric) face = TypeSpec.interfaceBuilder(model.getSimpleName() + Meta.Adaptor.SUFFIX)
                    .addAnnotation(generated())
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(parameterized)
                    .addSuperinterface(Meta.Adaptor.class)
                    .addTypeVariables(entityTypeVariableNames)
                    .addMethod(MethodSpec.methodBuilder("entity")
                            .returns(TypeName.get(entity.asType()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .build());
            else face = TypeSpec.interfaceBuilder(model.getSimpleName() + Meta.Adaptor.SUFFIX)
                    .addAnnotation(generated())
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(parameterized)
                    .addSuperinterface(Meta.Adaptor.class)
                    .addMethod(MethodSpec.methodBuilder("entity")
                            .returns(TypeName.get(entity.asType()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .build());
            if (mixins != null && !mixins.isEmpty())
                mixins.forEach(f -> face.addSuperinterface(f));
            return true;
        }

        protected boolean mixin() {
            if (mixins != null)
                for (var mixin : mixins) {
                    if (!((DeclaredType) mixin).asElement().getKind().isInterface()) {
                        u.error(GeneAdaptor.this, "mixin {} not a interface ", mixin);
                        return false;
                    }
                    ((DeclaredType) mixin).asElement().accept(new BaseMethodVisitor<Void>(false, u) {
                        @Override
                        public Void visitExecutable(ExecutableElement e, Void unused) {
                            if (u.isStatic(e) || isObjectMethod(e)) {
                                u.other("Ignore Object method: {}", e);
                                return unused;
                            }
                            if (cls != null) cls.addMethod(u.mixin(e, false));
                            if (face != null) face.addMethod(u.mixin(e, true));
                            return unused;
                        }
                    }, null);
                }
            return true;

        }

        List<JavaFile> build() {
            if (!config()) return null;
            if (!buildClassType()) return null;
            if (!buildInterfaceType()) return null;
            model.accept(new ModelMethodVisitor(), null);
            if (!mixin()) return null;
            return
                    cls != null && face != null
                            ? List.of(JavaFile.builder(pkg, cls.build()).build(), JavaFile.builder(pkg, face.build()).build())
                            : cls != null ? List.of(JavaFile.builder(pkg, cls.build()).build())
                            : List.of(JavaFile.builder(pkg, face.build()).build())
                    ;
        }

        class ModelMethodVisitor extends BaseMethodVisitor<Void> {
            ModelMethodVisitor() {
                super(false, GeneContext.this.u);
            }

            @Override
            public Void visitExecutable(ExecutableElement e, Void unused) {
                if (notInstanceMethod(e) || isObjectMethod(e)) {
                    u.other("Ignore Object method: {}", e);
                    return unused;
                }
                var name = e.getSimpleName().toString();
                var typeVar = e.getTypeParameters().stream().map(TypeVariableName::get).toList();
                var parameters = e.getParameters().stream().map(ParameterSpec::get).toList();
                var ret = e.getReturnType();
                ret = modelTypeVariables.getOrDefault(ret.toString(), ret);

                new MethodBuildContext(cls, genClassName, e, name, typeVar, parameters,
                        typeParameterConv(ret, isGeneric ? genParamClassName : genClassName, model), false).build();
                new MethodBuildContext(face, genInterfaceName, e, name, typeVar, parameters,
                        typeParameterConv(ret, isGeneric ? genParamInterfaceName : genInterfaceName, model), true).build();
                //means less return
                return unused;
            }


            class MethodBuildContext {
                final TypeSpec.Builder type;
                final ClassName typeName;
                final ExecutableElement modelMethod;
                final String name;
                final List<TypeVariableName> typeVar;
                final List<ParameterSpec> parameters;
                final TypeName returnType;
                final boolean isInterfaceType;

                MethodBuildContext(TypeSpec.Builder type, ClassName typeName, ExecutableElement modelMethod,
                                   String name, List<TypeVariableName> typeVar, List<ParameterSpec> parameters, TypeName returnType,
                                   boolean isInterfaceType) {
                    this.type = type;
                    this.typeName = typeName;
                    this.modelMethod = modelMethod;
                    this.name = name;
                    this.typeVar = typeVar;
                    this.parameters = parameters;
                    this.returnType = returnType;
                    this.isInterfaceType = isInterfaceType;

                }

                void build() {
                    if (type == null) return;
                    var m = MethodSpec.methodBuilder(name)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .addParameters(parameters)
                            .returns(returnType)
                            .addTypeVariables(typeVar);
                    if (isInterfaceType) m.addModifiers(Modifier.DEFAULT);
                    var code = CodeBlock.builder();
                    var retVoid = returnType.equals(TypeName.VOID);
                    var prj = entity.accept(new MethodMatcher(modelMethod), null);
                    if (prj == null) {
                        u.error(GeneAdaptor.this, "not found suitable method for {} on {}", modelMethod, entity);
                        return;
                    }
                    var openBracket = new AtomicInteger();
                    if (!retVoid) {
                        code.add("return ");
                        if (prj.returnTypeMapping() != null) {
                            writeProject(code, prj.returnTypeMapping(), null, openBracket);
                        }
                    }
                    if (isInterfaceType) {
                        openBracket.incrementAndGet();
                        code.add("entity().$L(", name);
                    } else {
                        openBracket.incrementAndGet();
                        code.add("entity.$L(", name);
                    }
                    if (prj.parameterMappings().size() > 0) {
                        for (int i = 0; i < modelMethod.getParameters().size(); i++) {
                            var p = prj.parameterMappings().get(i);
                            var from = prj.src().getParameters().get(i).asType();
                            var self = u.isSameType(from, entity.asType());
                            if (i > 0) code.add(",");
                            if (self && p != null) {
                                if (checkInstance) {
                                    code.add("v$L instanceOf $T t?t.entity", i, typeName);
                                    if (isInterfaceType) code.add("()");
                                    code.add(":");
                                }
                                writeProject(code, p, i, openBracket).add("v$L)", i);
                                openBracket.decrementAndGet();
                            } else if (p != null) {
                                writeProject(code, p, i, openBracket).add("v$L)", i);
                                openBracket.decrementAndGet();
                            } else {
                                code.add("v$L", i);
                            }
                        }
                    }
                    while (openBracket.get() != 0) {
                        code.add(")");
                        openBracket.decrementAndGet();
                    }
                    code.add(";");
                    m.addCode(code.build());
                    type.addMethod(m.build());
                }

                @SuppressWarnings("unchecked")
                private CodeBlock.Builder writeProject(CodeBlock.Builder code, ExecutableElement ele,
                                                       Integer pIndex, AtomicInteger openBracket) {
                    if (ele instanceof ProcUtil.SpecialMapping<?> s0) {
                        var s = ((ProcUtil.SpecialMapping<CODES>) s0);
                        switch (s.code) {
                            case CODE_SELF -> {
                                openBracket.incrementAndGet();
                                return isInterfaceType
                                        ? code.add("(($T)()->", typeName)
                                        : code.add("$T.of(", typeName);
                            }
                            case CODE_TYPE_PARAM -> {
                                if (pIndex != null) {
                                    openBracket.incrementAndGet();
                                    return code.add("(");
                                }
                                return code;
                            }
                            case CODE_SET_PROJECT -> {
                                ExecutableElement p = s.param();
                                openBracket.incrementAndGet();
                                if (p == null) {
                                    return code.add("$T.set(x->x,", Projections.class);
                                } else {
                                    var be = openBracket.get();
                                    code.add("$T.set(x$L->", be, Projections.class);
                                    writeProject(code, p, null, openBracket);
                                    code.add("x$L", be);
                                    var af = openBracket.get();
                                    if (af > be) {
                                        openBracket.decrementAndGet();
                                        code.add(")");
                                    }
                                    return code.add(",");
                                }
                            }
                            case CODE_LIST_PROJECT -> {
                                ExecutableElement p = s.param();
                                openBracket.incrementAndGet();
                                if (p == null) {
                                    return code.add("$T.list(x->x,", Projections.class);
                                } else {
                                    var be = openBracket.get();
                                    code.add("$T.list(x$L->", Projections.class, be);
                                    writeProject(code, p, null, openBracket);
                                    var af = openBracket.get();
                                    code.add("x$L", be);
                                    if (af > be) {
                                        openBracket.decrementAndGet();
                                        code.add(")");
                                    }
                                    return code.add(",");
                                }
                            }
                            case CODE_LIST_SET_PROJECT -> {
                                ExecutableElement p = s.param();
                                openBracket.incrementAndGet();
                                if (p == null) {
                                    return code.add("$T.listToSet(x->x,", Projections.class);
                                } else {
                                    var be = openBracket.get();
                                    code.add("$T.listToSet(x$L->", Projections.class, be);
                                    writeProject(code, p, null, openBracket);
                                    var af = openBracket.get();
                                    code.add("x$L", be);
                                    if (af > be) {
                                        openBracket.decrementAndGet();
                                        code.add(")");
                                    }
                                    return code.add(",");
                                }
                            }
                            case CODE_SET_LIST_PROJECT -> {
                                ExecutableElement p = s.param();
                                openBracket.incrementAndGet();
                                if (p == null) {
                                    return code.add("$T.setToList(x->x,", Projections.class);
                                } else {
                                    var be = openBracket.get();
                                    code.add("$T.setToList(x$L->", Projections.class, be);
                                    writeProject(code, p, null, openBracket);
                                    code.add("x$L", be);
                                    var af = openBracket.get();
                                    if (af > be) {
                                        openBracket.decrementAndGet();
                                        code.add(")");
                                    }
                                    return code.add(",");
                                }
                            }
                            case CODE_MAP_KEY_PROJECT -> {
                                ExecutableElement p = s.param();
                                openBracket.incrementAndGet();
                                if (p == null) {
                                    return code.add("$T.mapKey(x->x,", Projections.class);
                                } else {
                                    code.add("$T.mapKey(k->", Projections.class);
                                    var be = openBracket.get();
                                    writeProject(code, p, null, openBracket);
                                    var af = openBracket.get();
                                    code.add("k");
                                    if (af > be) {
                                        openBracket.decrementAndGet();
                                        code.add(")");
                                    }
                                    return code.add(",");
                                }
                            }
                            case CODE_MAP_VALUE_PROJECT -> {
                                ExecutableElement p = s.param();
                                openBracket.incrementAndGet();
                                if (p == null) {
                                    return code.add("$T.mapValue(x->x,", Projections.class);
                                } else {
                                    code.add("$T.mapValue(v->", Projections.class);
                                    var be = openBracket.get();
                                    writeProject(code, p, null, openBracket);
                                    var af = openBracket.get();
                                    code.add("v");
                                    if (af > be) {
                                        openBracket.decrementAndGet();
                                        code.add(")");
                                    }
                                    return code.add(",");
                                }
                            }
                            case CODE_MAP_PROJECT -> {
                                Pair<ExecutableElement, ExecutableElement> p = s.param();
                                if (p == null) {
                                    openBracket.incrementAndGet();
                                    return code.add("$T.map(x->x,x->x,", Projections.class);
                                } else {
                                    var k = p.v0();
                                    var v = p.v1();
                                    openBracket.incrementAndGet();
                                    code.add("$T.map(", Projections.class);
                                    if (k != null) {
                                        code.add("k->");
                                        var be = openBracket.get();
                                        writeProject(code, k, null, openBracket);
                                        var af = openBracket.get();
                                        code.add("k");
                                        if (af > be) {
                                            openBracket.decrementAndGet();
                                            code.add(")");
                                        }
                                    } else {
                                        code.add("x->x");
                                    }
                                    code.add(",");
                                    if (v != null) {
                                        code.add("v->");
                                        var be = openBracket.get();
                                        writeProject(code, k, null, openBracket);
                                        var af = openBracket.get();
                                        code.add("v");
                                        if (af > be) {
                                            openBracket.decrementAndGet();
                                            code.add(")");
                                        }
                                    } else {
                                        code.add("x->x");
                                    }
                                    return code.add(",");
                                }
                            }

                        }
                        return code;
                    }
                    openBracket.incrementAndGet();
                    code.add("$T.$L(", ele.getEnclosingElement().asType(), ele.getSimpleName());
                    return code;
                }

                enum CODES {
                    CODE_SELF,
                    CODE_TYPE_PARAM,
                    CODE_SET_PROJECT,
                    CODE_LIST_PROJECT,
                    CODE_LIST_SET_PROJECT,
                    CODE_SET_LIST_PROJECT,
                    CODE_MAP_KEY_PROJECT,
                    CODE_MAP_VALUE_PROJECT,
                    CODE_MAP_PROJECT,
                }

                ExecutableElement typeMapping(TypeMirror a, TypeMirror b) {
                    if (a instanceof TypeVariable v) {
                        var name = v.asElement().getSimpleName().toString();
                        var var = modelTypeVariables.get(name);
                        if (var != null) {
                            if (u.isSameType(var, Gene.Self.class) || u.isSameType(var, entity.asType())) {
                                return ProcUtil.SpecialMapping.of(typeName, CODE_SELF);
                            } else {
                                return ProcUtil.SpecialMapping.of(var, CODE_TYPE_PARAM);
                            }
                        }
                    }
                    if (u.isRawAssignableTo(a, Collection.class)) {
                        var ae = ((DeclaredType) a).getTypeArguments().get(0);
                        if (u.isRawAssignableTo(b, Collection.class)) {
                            var be = ((DeclaredType) b).getTypeArguments().get(0);
                            var isSetB = u.isRawAssignableTo(b, Set.class);
                            var isSetA = u.isRawAssignableTo(a, Set.class);
                            var code = isSetA && isSetB
                                    ? CODE_SET_PROJECT
                                    : !isSetA && !isSetB ? CODE_LIST_PROJECT
                                    : isSetA ? CODE_SET_LIST_PROJECT
                                    : CODE_LIST_SET_PROJECT;
                            if (u.isAssignableTo(ae, be)) {
                                return ProcUtil.SpecialMapping.of(null, code);
                            }
                            var p = typeMapping(ae, be);
                            if (p != null) {
                                return ProcUtil.SpecialMapping.of(p, code);
                            }
                        }
                    }
                    if (u.isRawAssignableTo(a, Map.class)) {
                        var ake = ((DeclaredType) a).getTypeArguments().get(0);
                        var ave = ((DeclaredType) a).getTypeArguments().get(1);
                        if (u.isRawAssignableTo(b, Map.class)) {
                            var bke = ((DeclaredType) b).getTypeArguments().get(0);
                            var bve = ((DeclaredType) b).getTypeArguments().get(1);
                            var ka = u.isRawAssignableTo(ake, bke);
                            var va = u.isRawAssignableTo(ave, bve);
                            if (ka && va) {
                                return ProcUtil.SpecialMapping.of(null, CODE_MAP_PROJECT);
                            }
                            var pk = typeMapping(ake, bke);
                            var pv = typeMapping(ave, bve);
                            if (pk != null && pv != null) {
                                return ProcUtil.SpecialMapping.of(new Pair<>(pk, pv), CODE_MAP_PROJECT);
                            } else if (pk != null) {
                                return ProcUtil.SpecialMapping.of(new Pair<>(pk, null), CODE_MAP_KEY_PROJECT);
                            } else if (pv != null) {
                                return ProcUtil.SpecialMapping.of(new Pair<>(null, pv), CODE_MAP_VALUE_PROJECT);
                            }
                        }
                    }
                    return mappings.isEmpty() ? null : mappings.get(a, b).orElse(null);
                }


                class MethodMatcher extends ElementScanner14<ProcUtil.Projection<ExecutableElement>, Void> {
                    final ExecutableElement subject;//method on interface


                    MethodMatcher(ExecutableElement target) {
                        super();
                        this.subject = target;
                    }

                    private ProcUtil.Projection<ExecutableElement> match;

                    @Override
                    public ProcUtil.Projection<ExecutableElement> visitExecutable(ExecutableElement project, Void unused) {
                        if (match != null) return match;
                        match = u.project(subject, project, true, MethodBuildContext.this::typeMapping);
                        return match;
                    }
                }
            }


        }
    }


}
