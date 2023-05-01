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

import cn.zenliu.domain.modeler.annotation.Generated;
import cn.zenliu.domain.modeler.annotation.Mode;
import cn.zenliu.domain.modeler.util.Loader;
import com.squareup.javapoet.AnnotationSpec;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nullable;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@ApiStatus.AvailableSince("0.1.0")
public abstract class AbstractProcessor {


    /**
     * current config prefix of this processor
     */
    @Getter
    @Setter
    protected @Nullable String prefix;

    /**
     * print info to {@link System#out}, nothrow.<br/>
     * <b>Note:</b> use {@link ProcUtil#note(String)} when processing.
     *
     * @param pattern SLF4J pattern
     * @param args    values
     */
    protected void info(String pattern, Object... args) {
        System.out.println(MessageFormatter.arrayFormat("[" + name() + "] " + pattern, args).getMessage());
    }

    /**
     * print error to {@link System#err}, nothrow.<br/>
     * <b>Note:</b> use {@link ProcUtil#error(String)} when processing.
     *
     * @param pattern SLF4J pattern
     * @param args    values
     */
    protected void errorf(String pattern, Object... args) {
        var m = MessageFormatter.arrayFormat("[" + name() + "] " + pattern, args);
        System.err.println(m.getMessage());
        if (m.getThrowable() != null)
            m.getThrowable().printStackTrace(System.err);
    }

    /**
     * print into {@link System#err} and throw error.<br/>
     * <b>Note:</b> use {@link ProcUtil#fatal(String)} when processing.
     *
     * @param pattern SLF4J pattern
     * @param args    values
     */
    @SneakyThrows
    protected void fatalf(String pattern, Object... args) {
        var m = MessageFormatter.arrayFormat("[" + name() + "] " + pattern, args);
        System.err.println(m.getMessage());
        if (m.getThrowable() != null) {
            m.getThrowable().printStackTrace(System.err);
            throw m.getThrowable();
        } else {
            throw new IllegalStateException(m.getMessage());
        }
    }

    /**
     * called when {@link #prefix} is set, overrides this when need some eager configurations.
     */
    protected void config() {

    }


    /**
     * @param element     current element, null if {@link #acceptTypes()} contains a "*" pattern.
     * @param annotations {@link Processor#process(Set, RoundEnvironment)}
     * @param roundEnv    {@link Processor#process(Set, RoundEnvironment)}
     * @param util        {@link ProcUtil}
     * @return skip next processor or not
     */
    public abstract boolean process(@Nullable Element element, Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcUtil util);

    protected String name() {
        return this.getClass().getSimpleName();
    }

    private String desc;

    /**
     * default to read {@link Desc} value, or just a 'Modeler Generator class.getSimpleName()'.
     */
    protected String desc() {
        if (this.desc != null) {
            return this.desc;
        }
        var desc = this.getClass().getAnnotation(Desc.class);
        if (desc != null) {
            return this.desc = desc.value();
        }
        return this.desc = "Modeler Generator " + this.getClass().getSimpleName();
    }

    /**
     * @see Processor#getSupportedAnnotationTypes()
     */
    public abstract List<String> acceptTypes();

    private String pomVersion;

    /**
     * auto read version from a pom.properties
     */
    protected String pomVersion() {
        if (pomVersion == null) pomVersion = Loader.loadPomVersion(this.getClass());
        return pomVersion;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add("prefix=" + prefix)
                .add("version=" + pomVersion())
                .add("desc=" + desc())
                .toString();
    }

    /**
     * @return a Generated annotation
     */
    protected AnnotationSpec generated() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("processor", "$S", name())
                .addMember("version", "$S", pomVersion())
                .addMember("timestamp", "$LL", System.currentTimeMillis())
                .build();
    }

    /**
     * @param ele current process element
     * @param u   util
     * @return null if already generated by current processor or config is disabled
     */
    protected @org.jetbrains.annotations.Nullable Configurer.Configs preCheck(Element ele, ProcUtil u) {
        if (u.isGeneratedBy(ele, name())) {
            return null;
        }
        var c = Configurer.resolve(u, ele);
        if (!(c.isEnabled() && c.isEnabled(self().getPrefix()))) {
            if (c.isDebug()) u.mandatoryWarn(self(), "disabled when process {}", ele);
            return null;
        }
        if (c.isDebug()) info("Configs {} for target '{}'", c, ele);
        return c;
    }

    protected abstract AbstractProcessor self();

    protected boolean notInterface(ProcUtil u, String anno, TypeElement t) {
        if (!t.getKind().isInterface()) {
            u.mandatoryWarn(self(), "type {} not a valid target of {}, only interface supported", anno, t);
            return true;
        }
        return false;
    }


    protected boolean notInherit(ProcUtil u, String anno, TypeElement t, Class<?> type) {
        if (!u.isAssignable(t.asType(), type)) {
            u.mandatoryWarn(self(), "type {} not a valid target of {}, not inherit {}", t, anno, type.getCanonicalName());
            return true;
        }
        return false;
    }
    protected boolean inherit(ProcUtil u, TypeElement t, Class<?> type) {
        //u.mandatoryWarn(self(), "type {} not a valid target of {}, not inherit {}", t, anno, type.getCanonicalName());
        return u.isAssignable(t.asType(), type);
    }
    protected boolean mustInheritOneOf(ProcUtil u, String anno, TypeElement t, Class<?>... types) {
        for (var c : types) {
            if (u.isAssignable(t.asType(), c)) return true;
        }
        u.mandatoryWarn(self(), "type {} not a valid target of {}, not inherit one of {}", t, anno, types);
        return false;
    }

    protected boolean notDirectInherit(ProcUtil u, String anno, TypeElement t, Class<?> type) {
        if (u.isDirectlyInherit(t, type)) {
            u.mandatoryWarn(self(), "type {} not a valid target of {}, should not direct inherit {}", t, anno, type.getCanonicalName());
            return true;
        }
        return false;
    }

    protected boolean isAnnotated(TypeElement t, Class<? extends Annotation> annoType) {
        return t.getAnnotation(annoType) != null;
    }

    /**
     * @param t the element to check
     * @return true for annotated with Mode.Prototype
     */
    protected boolean isPrototype(Element t) {
        if (t == null) return false;
        return t.getAnnotationsByType(Mode.Prototype.class).length > 0;
    }
}
