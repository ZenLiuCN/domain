package cn.zenliu.domain.modeler.prototype;

import cn.zenliu.domain.modeler.processor.Configurer;
import cn.zenliu.domain.modeler.processor.ModelerProcessor;
import com.google.testing.compile.JavaFileObjects;
import lombok.SneakyThrows;

import javax.tools.JavaFileObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ProcessorTest {
    @SneakyThrows
    public static void main(String[] args) {
        entityGenericProcess();
    }

    @SneakyThrows
    static void entityGenericProcess() {
        Files.writeString(Paths.get(Configurer.FILE_NAME), """
                 debug=true
                 proc.entity.processor=cn.zenliu.domain.modeler.processor.EntityProcessor
                 proc.entity.chain=true
                 proc.fields.enabled=false
                 proc.fields.processor=cn.zenliu.domain.modeler.processor.FieldsProcessor
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                        package some.pack;
                        import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                        import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                        import cn.zenliu.domain.modeler.annotation.Generated;
                        import cn.zenliu.domain.modeler.prototype.Meta;
                        @Entity @Fields
                        public interface MetaTest<T> extends Meta.Object {
                              T getId();
                              @Entity
                              @Generated(processor = "EntityProcessor",version = "123",timestamp = 123L)
                              @Generated(processor = "FieldsProcessor",version = "123",timestamp = 123L)
                              interface MetaEntity<T> extends MetaTest<T>,Meta.Entity{
                                void setId(T v);
                              }
                              @Entity
                              interface MetaEntity3<T> extends MetaEntity<T>{
                              }
                        }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }

    @SneakyThrows
    static void fieldsGenericProcess() {
        Files.writeString(Paths.get(Configurer.FILE_NAME), """
                 debug=true
                 proc.entity.processor=cn.zenliu.domain.modeler.processor.EntityProcessor
                 proc.entity.chain=true
                 proc.entity.enabled=false
                 proc.fields.processor=cn.zenliu.domain.modeler.processor.FieldsProcessor
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                         @Entity @Fields
                          public interface MetaTest<T> extends Meta.Object {
                                                         T getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }

    @SneakyThrows
    static void entityProcessFile() {
        Files.writeString(Paths.get(Configurer.FILE_NAME), """
                 debug=true
                 proc.entity.processor=cn.zenliu.domain.modeler.processor.EntityProcessor
                 proc.entity.chain=true
                 proc.fields.enabled=false
                 proc.fields.processor=cn.zenliu.domain.modeler.processor.FieldsProcessor
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forResource("Some.java"));
        assertThat(compilation).succeededWithoutWarnings();
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }

    @SneakyThrows
    static void entityProcess() {
        Files.writeString(Paths.get(Configurer.FILE_NAME), """
                 debug=true
                 proc.entity.processor=cn.zenliu.domain.modeler.processor.EntityProcessor
                 proc.entity.chain=true
                 proc.fields.enabled=false
                 proc.fields.processor=cn.zenliu.domain.modeler.processor.FieldsProcessor
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                        package some.pack;
                        import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                        import cn.zenliu.domain.modeler.prototype.Meta;
                        @Entity
                        public interface MetaTest extends Meta.Object {
                                String getId();
                        }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }

    @SneakyThrows
    static void fieldsProcess() {
        Files.writeString(Paths.get(Configurer.FILE_NAME), """
                 debug=true
                 proc.entity.processor=cn.zenliu.domain.modeler.processor.EntityProcessor
                 proc.entity.chain=true
                 proc.entity.enabled=false
                 proc.fields.processor=cn.zenliu.domain.modeler.processor.FieldsProcessor
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                         @Entity @Fields
                          public interface MetaTest extends Meta.Object {
                                                         String getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }

    @SneakyThrows
    static void fieldsAndEntityProcess() {
        Files.writeString(Paths.get(Configurer.FILE_NAME), """
                 debug=true
                 proc.entity.processor=cn.zenliu.domain.modeler.processor.EntityProcessor
                 proc.entity.chain=true
                 proc.fields.processor=cn.zenliu.domain.modeler.processor.FieldsProcessor
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var compilation = javac()
                .withProcessors(new ModelerProcessor())
                .compile(JavaFileObjects.forSourceString("MetaTest", """
                         package some.pack;
                         import cn.zenliu.domain.modeler.annotation.Gene.Fields;
                         import cn.zenliu.domain.modeler.annotation.Gene.Entity;
                         import cn.zenliu.domain.modeler.prototype.Meta;
                         @Entity @Fields
                          public interface MetaTest extends Meta.Object {
                                                         String getId();
                          }
                        """));
        assertThat(compilation).succeededWithoutWarnings();
        for (var f : compilation.generatedFiles()) {
            if (f.getKind() != JavaFileObject.Kind.SOURCE) continue;
            System.out.println(f);
            try (var r = f.openInputStream()) {
                r.transferTo(System.out);
            }
        }
    }
}