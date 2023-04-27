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

package cn.zenliu.domain.modeler.annotation;


import cn.zenliu.domain.modeler.util.TypeInfo;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Zen.Liu
 * @since 2023-04-27
 */
class InfoTest {
    @Some
    static class T extends HashMap<String, Object> {

    }

    static class T2<K> extends HashMap<K, Object> {
        @Some
        static class T3 extends T2<String> {

        }
    }

    @SupportedAnnotationTypes({"cn.zenliu.domain.modeler.annotation.Some"})
    @SupportedSourceVersion(SourceVersion.RELEASE_17)
    class P extends javax.annotation.processing.AbstractProcessor {
        final BiConsumer<ProcessingEnvironment,Element> fn;

        P(BiConsumer<ProcessingEnvironment, Element> fn) {
            this.fn = fn;
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            var ele = roundEnv.getElementsAnnotatedWith(Some.class);
            for (Element e : ele) {
               fn.accept(processingEnv,e);
            }
            return false;
        }
    }

    @Test
    void typeInfoFromClassTest() {
        var info = TypeInfo.from(T.class.getGenericSuperclass());
        System.out.println(info);
        var bytes = TypeInfo.serialize(info);
        var i2 = TypeInfo.deserialize(bytes);
        assertEquals(info, i2);
    }

    @Test
    void typeInfoFromProcessorTest() {
        var compilation = javac()
                .withProcessors(new P((env,e)->{
                    var su = env.getTypeUtils().directSupertypes(e.asType()).get(0);
                    var info = TypeInfo.from(su, env);
                    System.out.println(info);
                    var bytes = TypeInfo.serialize(info);
                    var i2 = TypeInfo.deserialize(bytes);
                    assertEquals(info, i2);
                    var infClass = TypeInfo.from(T.class.getGenericSuperclass());
                    assertEquals(infClass, info);
                }))
                .compile(JavaFileObjects.forSourceString("some.pack.T", """
                         package some.pack;
                          @cn.zenliu.domain.modeler.annotation.Some
                          class T extends java.util.HashMap<String, Object> {
                           
                           }
                        """));
        assertThat(compilation).succeededWithoutWarnings();

    }
    @Test
    void typeInfoFromProcessorNestTest() {
       var compilation = javac()
                .withProcessors(new P((env,e)->{
                    var su = env.getTypeUtils().directSupertypes(e.asType()).get(0);
                    var info = TypeInfo.from(su, env);
                    System.out.println(info);
                    var bytes = TypeInfo.serialize(info);
                    var i2 = TypeInfo.deserialize(bytes);
                    assertEquals(info, i2);
                    var infClass = TypeInfo.from(T2.T3.class.getGenericSuperclass());
                    assertEquals(infClass, info);
                }))
                .compile(JavaFileObjects.forSourceString("cn.zenliu.domain.modeler.annotation.InfoTest", """
                         package cn.zenliu.domain.modeler.annotation;
                         import java.util.HashMap;
                         class InfoTest{
                         static class T2<K> extends HashMap<K, Object> {
                             @cn.zenliu.domain.modeler.annotation.Some
                             static class T3 extends T2<String> {
                         
                             }
                         }
                         }
                      
                        """));
        assertThat(compilation).succeededWithoutWarnings();
    }
}
