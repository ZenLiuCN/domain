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

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class GeneTest {
    static final boolean print =
            System.getProperties().containsKey("sun.java.command") &&
                    System.getProperties().getProperty("sun.java.command").contains("com.intellij");

    @SneakyThrows
    static void config(String content) {
        Files.writeString(Paths.get(Configurer.FILE_NAME), ("debug=%s\n%s\n").formatted(print, content), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SneakyThrows
    @Test
    void geneFields() {
        config("proc.fields.processor=cn.zenliu.domain.modeler.processor.GeneFields");
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Fields
                          public interface MetaTest<T> extends Meta.Object {
                               T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestFields.java")
                .contentsAsUtf8String()
                .isNotEmpty();

    }

    @SneakyThrows
    @Test
    void geneFieldsComplex() {
        config("proc.fields.processor=cn.zenliu.domain.modeler.processor.GeneFields");
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                         import java.util.Map;
                          @Fields
                          public interface MetaTest<T> extends Meta.Object {
                               Map<T,String> getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        save(compilation,x->x.isNameCompatible("MetaTestFields", JavaFileObject.Kind.CLASS),Paths.get("MetaTestFields.class"));
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestFields.java")
                .contentsAsUtf8String()
                .isNotEmpty();

    }

    @SneakyThrows
    @Test
    void geneFieldsOfEntity() {
        config("proc.fields.processor=cn.zenliu.domain.modeler.processor.GeneFields");
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Entity
                          public interface MetaTest<T> extends Meta.Object {
                                                         T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestFields.java")
                .contentsAsUtf8String()
                .isNotEmpty();
    }

    @SneakyThrows
    @Test
    void geneEntity() {
        config("proc.fields.processor=cn.zenliu.domain.modeler.processor.GeneEntity");

        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Entity
                          public interface MetaTest<T> extends Meta.Object {
                                                         T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestEntity.java")
                .contentsAsUtf8String()
                .isNotEmpty();
    }

    @SneakyThrows
    @Test
    void geneEntityChain() {
        config("""
                proc.entity.chain=true
                proc.entity.processor=cn.zenliu.domain.modeler.processor.GeneEntity
                """);

        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Entity
                          public interface MetaTest<T> extends Meta.Object {
                                                         T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestEntity.java")
                .contentsAsUtf8String()
                .isNotEmpty();
    }

    @SneakyThrows
    @Test
    void geneMutate() {
        config("""
                proc.mutate.chain=true
                proc.mutate.processor=cn.zenliu.domain.modeler.processor.GeneMutate
                """);

        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Mutate;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Mutate
                          public interface MetaTest<T> extends Meta.Object {
                                                         T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestMutate.java")
                .contentsAsUtf8String()
                .isNotEmpty();
    }

    @SneakyThrows
    @Test
    void geneMutateTrait() {
        config("""
                proc.mutate.chain=true
                proc.mutate.processor=cn.zenliu.domain.modeler.processor.GeneMutate
                """);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Mutate;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Mutate
                          public interface MetaTest<T> extends Meta.Trait {
                                T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestMutate.java")
                .contentsAsUtf8String()
                .isNotEmpty();
    }

    @SneakyThrows
    @Test
    void geneMutateWithEntity() {
        config("""
                proc.mutate.chain=true
                proc.mutate.processor=cn.zenliu.domain.modeler.processor.GeneMutate
                proc.entity.chain=true
                proc.entity.processor=cn.zenliu.domain.modeler.processor.GeneEntity
                """);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Mutate;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                          @Mutate @Entity
                          public interface MetaTest<T> extends Meta.Object {
                                                         T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        print(compilation);
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestMutate.java")
                .contentsAsUtf8String()
                .isNotEmpty();
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "some/pack/MetaTestEntity.java")
                .contentsAsUtf8String()
                .isNotEmpty();
    }

    @SneakyThrows
    static void print(Compilation compilation) {
        if (!print) return;
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }

    @SneakyThrows
    static void save(Compilation compilation, Predicate<JavaFileObject> filter, Path out) {
        for (var f : compilation.generatedFiles()) {
            if (filter.test(f))
                try (var r = f.openInputStream()) {
                    r.transferTo(Files.newOutputStream(out, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE));
                }
        }
    }
}

