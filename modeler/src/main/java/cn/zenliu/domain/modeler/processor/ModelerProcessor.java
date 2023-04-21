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

import cn.zenliu.domain.modeler.util.Loader;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
@com.google.auto.service.AutoService(Processor.class)
public class ModelerProcessor implements Processor, ProcUtil {
    private final Map<Pattern, Set<AbstractProcessor>> processors;
    private final Set<String> supportedTypes;

    //region Pattern Compile
    public static final Pattern noMatches = Pattern.compile("(\\P{all})+");
    private static final String allMatchesString = ".*";
    private static final Pattern allMatches = Pattern.compile(allMatchesString);

    private static String validImportStringToPatternString(String s) {
        if (s.equals("*")) {
            return allMatchesString;
        } else {
            var s_prime = s.replace(".", "\\.");
            if (s_prime.endsWith("*")) {
                s_prime = s_prime.substring(0, s_prime.length() - 1) + ".+";
            }
            return s_prime;
        }
    }

    private static boolean isValidImportString(String s) {
        if (s.equals("*"))
            return true;
        var valid = true;
        var t = s;
        var index = t.indexOf('*');
        if (index != -1) {
            // '*' must be last character...
            if (index == t.length() - 1) {
                // ... any and preceding character must be '.'
                if (index - 1 >= 0) {
                    valid = t.charAt(index - 1) == '.';
                    // Strip off ".*$" for identifier checks
                    t = t.substring(0, t.length() - 2);
                }
            } else
                return false;
        }

        // Verify string is off the form (javaId \.)+ or javaId
        if (valid) {
            String[] javaIds = t.split("\\.", t.length() + 2);
            for (String javaId : javaIds)
                valid &= SourceVersion.isIdentifier(javaId);
        }
        return valid;
    }

    private static Pattern validImportStringToPattern(String s) {
        var pattern = validImportStringToPatternString(s);
        if (pattern == allMatchesString) {
            return allMatches;
        } else {
            return Pattern.compile(pattern);
        }
    }

    private static Pattern importStringToPattern(String pattern) {
        String module;
        String pkg;
        var slash = pattern.indexOf('/');
        if (slash == (-1)) {
            if (pattern.equals("*")) {
                return validImportStringToPattern(pattern);
            }
            module = "";
            pkg = pattern;
        } else {
            var moduleName = pattern.substring(0, slash);
            if (!SourceVersion.isName(moduleName)) {
                System.err.println("[modeler processor] invalid annotation type pattern:" + pattern);
                return noMatches;
            }
            module = Pattern.quote(moduleName + "/");
            pkg = pattern.substring(slash + 1);
        }
        if (isValidImportString(pkg)) {
            return Pattern.compile(module + validImportStringToPatternString(pkg));
        } else {
            System.err.println("[modeler processor] invalid annotation type pattern:" + pattern);
            return noMatches;
        }
    }

    //endregion
    static void printf(String pattern, Object... args) {
        System.out.println("[modeler processor]" + MessageFormatter.arrayFormat(pattern, args).getMessage());
    }

    static void errorf(String pattern, Object... args) {
        System.err.println("[modeler processor]" + MessageFormatter.arrayFormat(pattern, args).getMessage());
    }


    public ModelerProcessor() {
        if (!Loader.classExists("com.squareup.javapoet.TypeSpec", this.getClass().getClassLoader())) {
            errorf("missing dependency com.squareup:javapoet:1.13.0 , meta processors will disabled");
            this.supportedTypes = Collections.emptySet();
            this.processors = Collections.emptyMap();
        }else if(Configurer.root==null){
            printf("user.dir: {}",Configurer.userDir);
            Configurer.outer(Configurer.userDir, Configurer.userDir.toAbsolutePath().toString().split(Pattern.quote(File.separator)).length - 1, true);
            errorf("missing or invalid configure file '{}' , meta processors will disabled", Configurer.FILE_NAME);
            this.supportedTypes = Collections.emptySet();
            this.processors = Collections.emptyMap();
        }else{
            var conf=Configurer.parse();
            this.debug = conf.v0().debug;
            var processors = conf.v1();
            if (!conf.v0().isEnabled()) {
                this.processors = Collections.emptyMap();
                this.supportedTypes = Collections.emptySet();
                errorf("meta processors disabled");
                return;
            }
            if (processors.isEmpty()) {
                this.processors = Collections.emptyMap();
                this.supportedTypes = Collections.emptySet();
                errorf("empty config found, meta processors will disabled");
                return;
            }
            this.supportedTypes = processors.keySet();
            if (debug)
                printf("supported annotations: {}", this.supportedTypes);
            this.processors = processors.entrySet().stream().map(x -> new Pair<>(importStringToPattern(x.getKey()), x.getValue())).collect(Collectors.toMap(Pair::v0, Pair::v1, (s0, s1) -> {
                s0.addAll(s1);
                return s0;
            }));
            if (debug)
                printf("enabled processors: {}", this.processors);
        }

    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return supportedTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Getter
    @Accessors(fluent = true)
    private ProcessingEnvironment env;
    @Getter
    @Accessors(fluent = true)
    private Elements elements;
    @Getter
    @Accessors(fluent = true)
    private Types types;

    @Getter
    @Accessors(fluent = true)
    private Filer filer;
    private boolean initialized = false;
    @Getter
    private boolean debug = false;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        if (initialized) throw new IllegalStateException("Cannot call init more than once.");
        Objects.requireNonNull(processingEnv, "Tool provided null ProcessingEnvironment");
        this.env = processingEnv;
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        initialized = true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (processors.isEmpty()) return false;
        for (var entry : processors.entrySet()) {
            var p = entry.getKey();
            for (var proc : entry.getValue()) {
                if (annotations.isEmpty() && Objects.equals(p.pattern(), "*")) {
                    if (proc.process(null, annotations, roundEnv, this)) return true;
                } else {
                    var pred = p.asPredicate();
                    for (var annotation : annotations) {
                        if (!pred.test(annotation.asType().toString())) continue;
                        for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            if (proc.process(element, annotations, roundEnv, this)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return List.of(); //TODO
    }
}
