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
import lombok.*;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.helpers.MessageFormatter;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */

@ApiStatus.AvailableSince("0.1.2")
public interface Configurer {
    String FILE_NAME = "modeler.properties";

    boolean DEBUG = System.getProperty("modeler.config.debug") != null;

    static void debugf(String pattern, Object... args) {
        System.out.println("[Modeler\t] [DEBUG] " + MessageFormatter.arrayFormat(pattern, args).getMessage());
    }

    static void infof(String pattern, Object... args) {
        System.out.println("[Modeler\t] [INFO] " + MessageFormatter.arrayFormat(pattern, args).getMessage());
    }

    static void errorf(String pattern, Object... args) {
        System.err.println("[Modeler\t] [ERROR] " + MessageFormatter.arrayFormat(pattern, args).getMessage());
    }

    static void debugCfg(String pattern, Object... args) {
        if (DEBUG) {
            System.out.println("[Modeler\t] [DEBUG] " + MessageFormatter.arrayFormat(pattern, args).getMessage());
        }
    }

    Path USER_DIR = Paths.get(System.getProperty("user.dir"));
    /**
     * config group by module
     */
    Map<Path, SortedSet<Path>> CONFIGS = new ConcurrentHashMap<>();
    SortedSet<Path> MODULES = new TreeSet<>(Comparator.comparing(Path::getNameCount));

    enum BuiltType {
        MAVEN("pom.xml"),
        GRADLE("build.gradle"),
        ANT("build.xml"),
        ;
        final String conf;

        BuiltType(String conf) {
            this.conf = conf;
        }

        public boolean match(Path p) {
            return p.getFileName().toString().equalsIgnoreCase(conf);
        }

        public static @org.jetbrains.annotations.Nullable BuiltType matches(Path p) {
            for (BuiltType value : values()) {
                if (value.match(p)) return value;
            }
            return null;
        }
    }

    /**
     * the most short path config file
     */
    AtomicReference<Path> ROOT = new AtomicReference<>();


    static Set<Path> locator(int deep, int up, Predicate<Path> match) {
        var found = new HashSet<Path>();
        into(deep, match).forEach(found::add);
        lookup(up, match).findFirst().ifPresent(found::add);
        return found;
    }

    @SneakyThrows
    static Stream<Path> into(int deep, Predicate<Path> match) {
        return Files.walk(USER_DIR, deep)
                .filter(x -> x.getFileName().toString().charAt(0) != '.' && x.toFile().isFile())
                .filter(match);
    }

    static boolean buildFile(Path path) {
        return BuiltType.matches(path) != null;
    }

    static boolean configFile(Path path) {
        var fn = path.getFileName().toString();
        return fn.equalsIgnoreCase(Configurer.FILE_NAME);
    }

    static Stream<Path> lookup(int deep, Predicate<Path> match) {
        var it = new DirUpIter(deep, USER_DIR);
        var sp = Spliterators.spliteratorUnknownSize(it, Spliterator.DISTINCT);
        return StreamSupport.stream(sp, false)
                .flatMap(x -> {
                    try {
                        return Files.list(x);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .filter(x -> x.getFileName().toString().charAt(0) != '.' && x.toFile().isFile())
                .filter(match);
    }

    class DirUpIter implements Iterator<Path> {
        volatile int deep;
        volatile Path current;
        private final Object lock = new Object();

        public DirUpIter(int deep, Path root) {
            this.deep = deep;
            this.current = root;
        }

        @Override
        public boolean hasNext() {
            return deep > 0 && current.getParent() != null;
        }

        @Override
        public Path next() {
            synchronized (lock) {
                current = current.getParent();
                deep--;
            }
            return current;
        }
    }

    interface Configuration {
        boolean containsKey(String key);

        String getProperty(String key);

        Set<String> keySet();
    }

    @EqualsAndHashCode
    @ToString
    class PropertiesConf implements Configuration {
        final Properties p;

        public PropertiesConf(Properties p) {
            this.p = p;
        }

        @Override
        public boolean containsKey(String key) {
            return p.containsKey(key);
        }

        @Override
        public String getProperty(String key) {
            return p.getProperty(key);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Set<String> keySet() {
            return (Set<String>) (Set) p.keySet();
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    class DeepFirstConf extends PropertiesConf {
        @Getter
        final int deep;

        DeepFirstConf(Properties p, Path file) {
            super(p);
            deep = file.getNameCount();
        }
    }

    /**
     * @param properties SortedSet of properties
     * @param cache      cache map (may concurrent)
     */
    record Config(Set<Configuration> properties, Map<String, Object> cache) {
        @SuppressWarnings("unchecked")
        public <T> Optional<T> read(String key, Function<String, T> parser) {
            if (cache.containsKey(key)) return Optional.of((T) cache.get(key));
            for (var property : properties) {
                if (property.containsKey(key)) {
                    var v = parser.apply(property.getProperty(key));
                    cache.put(key, v);
                    return Optional.of(v);
                }
            }
            return Optional.empty();
        }

        public Optional<Boolean> readBoolean(String key) {
            return read(key, Boolean::parseBoolean);
        }

        public Optional<String> readString(String key) {
            return read(key, Function.identity());
        }

        public Optional<Integer> readInteger(String key) {
            return read(key, Integer::parseInt);
        }

        public Optional<Long> readLong(String key) {
            return read(key, Long::parseLong);
        }

        public Optional<Double> readDouble(String key) {
            return read(key, Double::parseDouble);
        }

        public List<String> keys() {
            return properties.stream().flatMap(x -> x.keySet().stream()).map(x -> (String) x).distinct().toList();
        }

        public boolean enabled(@javax.annotation.Nullable String prefix) {
            var root = readBoolean("enabled").orElse(true);
            if (prefix == null) return root;
            return readBoolean(prefix + "enabled").orElse(true);
        }

        public boolean debug() {
            return readBoolean("debug").orElse(true);
        }

        public Optional<List<String>> accept(String prefix) {
            return readString(prefix + "accept").map(x -> Arrays.asList(x.split(",")));
        }
    }

    static Config parse(Path file) {
        var p = new Properties();
        try {
            p.load(file.toUri().toURL().openStream());
        } catch (IOException e) {
            return null;
        }
        return new Config(Collections.singleton(new PropertiesConf(p)), new ConcurrentHashMap<>());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Config parse(Set<Path> file) {
        var set = new TreeSet<>(Comparator.comparing(DeepFirstConf::getDeep).reversed());
        for (var path : file) {
            try {
                var p = new Properties();
                p.load(path.toUri().toURL().openStream());
                set.add(new DeepFirstConf(p, path));
            } catch (IOException e) {
                return null;
            }
        }
        return new Config((Set<Configuration>) (Set) set, new ConcurrentHashMap<>());
    }

    @Data
    class Setting {
        boolean debug;
        boolean enabled;
    }

    AtomicReference<Config> ROOT_CONF = new AtomicReference<>();

    /**
     * parse the founded outermost config file
     */
    @SneakyThrows
    static Pair<Setting, Map<String, Set<AbstractProcessor>>> parse() {
        if (ROOT.get() != null) {
            ROOT_CONF.set(parse(ROOT.get()));
        } else {
            return null;
        }
        var s = new Setting();
        var rc = ROOT_CONF.get();
        s.setDebug(rc.debug());
        s.setEnabled(rc.enabled(null));
        var keys = rc.keys();
        var processors = new HashMap<String, Set<String>>();
        for (var key : keys) {
            if (key.startsWith("proc.")) {
                var lastDot = key.substring(6).indexOf('.') + 6;
                var name = key.substring(0, lastDot + 1);
                var map = processors.computeIfAbsent(name, x -> new HashSet<>());
                var value = key.substring(lastDot + 1);
                map.add(value);
            }
        }
        var map = new HashMap<String, Set<AbstractProcessor>>();
        for (var pre : processors.keySet()) {
            // var keySet = entry.getValue();
            if (rc.enabled(pre)) {
                var cls = rc.readString(pre + "processor").orElseThrow(() -> new IllegalStateException("missing config of " + pre + "processor"));
                var i = Loader.loadClass(AbstractProcessor.class, cls);
                var accept = rc.accept(pre).orElseGet(i::acceptTypes);
                for (var k : accept) {
                    map.computeIfAbsent(k, x -> new HashSet<>()).add(i);
                }
                i.setPrefix(pre);
            }
        }
        return new Pair<>(s, map);
    }


    /**
     * @param u       util
     * @param element the element
     * @return element relative config chain.
     */
    static Config resolve(ProcUtil u, Element element) {
        var uri = u.sourcePath(element);
        if (uri.getScheme() == null) {
            debugCfg("Can't found element {} path from URI {}", element, uri.toASCIIString());
            return ROOT_CONF.get();
        }
        //the source parent
        var path = Paths.get(uri).getParent().toAbsolutePath().toString();
        Set<Path> match = null;
        Path last = null;
        for (var entry : CONFIGS.entrySet()) {
            var key = entry.getKey();
            var val = entry.getValue();
            if (path.startsWith(key.toString()) && !val.isEmpty()) {
                if (last == null || last.getNameCount() < key.getNameCount()) {
                    match = val;
                    last = key;
                }

            }
        }
        if (match == null) {
            debugCfg("Found no element {} relative config ", element);
            return ROOT_CONF.get();
        }
        var p = new HashSet<>(match);
        var rc = ROOT.get();
        if (last.toString().startsWith(rc.toString()))
            p.add(rc);
        debugCfg("Found element {} relative configs {} ", element, p);
        return parse(p);
    }

    static boolean init() {
        CONFIGS.clear();
        MODULES.clear();
        var modules = locator(15, 4, Configurer::buildFile)
                .stream()
                .map(Path::getParent)
                .collect(Collectors.toSet());
        var settings = locator(15, 4, Configurer::configFile);
        var root = (Path) null;
        for (var module : modules) {
            module = module.toAbsolutePath();
            var m = module.toString();
            for (var setting : settings) {
                setting = setting.toAbsolutePath();
                if (setting.toString().startsWith(m)) {
                    if (root == null) root = setting;
                    else if (root.getNameCount() > setting.getNameCount()) root = setting;
                    var set = CONFIGS.computeIfAbsent(module, i -> new TreeSet<>(Comparator.comparing(Path::getNameCount)));
                    set.add(setting);
                    break;
                }
            }
        }
        ROOT.set(root);
        MODULES.addAll(modules);
        if (root != null) {
            var prj = MODULES.first();
            debugf("Try check project config under {}", prj);
            var rs = CONFIGS.get(MODULES.first());
            if (rs != null && !rs.first().getParent().equals(prj)) {
                rs.clear(); //NO root configuration
            }
        }
        debugCfg("Found root config: {} ", root);
        debugCfg("Found configs: {} ", CONFIGS);
        debugCfg("Found modules: {} ", MODULES);
        return ROOT.get() == null;
    }

    /**
     * calculate the real root configuration path
     */
    static boolean calcRoot(Path root) {
        root = root.toAbsolutePath();
        debugCfg("Try resolve module root from {}", root);
        var name = root.toString();
        if (name.endsWith("classes")) {
            root = root.getParent();
            name = root.toString();
            if (name.endsWith("target")) {
                root = root.getParent();//maven
            }
        }
        var val = CONFIGS.get(root);
        if (val == null || val.isEmpty()) {
            debugCfg("Can't found module {} config in {}", root, CONFIGS);
            if (ROOT.get() == null) return false;
            return root.toString().startsWith(ROOT.get().toString());//simple check if a submodule of located config
        }
        var roots = val.first();
        debugCfg("Found module {} root config at {} ", root, roots);
        ROOT.set(roots);
        return true;
    }


}
