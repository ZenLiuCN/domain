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
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */

@ApiStatus.AvailableSince("0.1.2")
public interface Configurer {
    String FILE_NAME = "modeler.properties";

    interface Parser {
        static Parser create(String name, String example, Function<String, Object> parse) {
            return new Parser() {
                @Override
                public String desc() {
                    return name;
                }

                @Override
                public String example() {
                    return example;
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> T parse(String val) {
                    return (T) parse.apply(val);
                }
            };
        }

        static Parser enumParser(Class<? extends Enum<?>> type) {
            return new Parser() {
                @Override
                public String desc() {
                    return "enum (" + type.getName() + ")";
                }

                @Override
                public String example() {
                    return Arrays.toString(type.getEnumConstants()).replace(",", " |");
                }

                @SuppressWarnings({"unchecked", "rawtypes"})
                @Override
                public <T> T parse(String val) {
                    return (T) Enum.valueOf((Class<? extends Enum>) type, val);
                }
            };
        }


        String desc();

        String example();

        <T> T parse(String val);

        Map<Class<?>, Parser> SIMPLE_TYPE = Map.of(
                String.class, create("string", "<text>", x -> x)
                , Integer.class, create("int", "<int>", Integer::parseInt)
                , Long.class, create("long", "<long>", Long::parseLong)
                , Double.class, create("double", "<double>", Double::parseDouble)
                , Boolean.class, create("boolean", "[true|false]", Boolean::parseBoolean)
        );
    }


    Map<Path, Path> founded = new ConcurrentHashMap<>();

    /**
     * found outermost folder with {@link #FILE_NAME}
     *
     * @param current current path
     * @param deep    max level to find
     */
    static Path outer(Path current, int deep, boolean debug) {
        current = current.toAbsolutePath();
        if (debug) System.out.println("lookup config from :" + current + ", max level:" + deep);
        Path last = null;
        var n = 0;
        while (n < deep) {
            if (current.resolve(FILE_NAME).toFile().exists()) {
                if (debug) System.out.println("found config at :" + current);
                last = current;
                founded.put(current, current.resolve(FILE_NAME));
            } else if (debug) {
                System.out.println("missing config at :" + current);
            }
            if (current.getParent() == null || current.getParent().equals(current)) break;//file system root
            current = current.getParent();
            n++;
        }
        return last;
    }

    static List<Path> all(Path current, Path root) {
        current = current.toAbsolutePath();
        var found = new ArrayList<Path>();
        while (!current.equals(root)) {
            if (founded.containsKey(current)) {
                found.add(founded.get(current));
            } else if (current.resolve(FILE_NAME).toFile().exists()) {
                found.add(current.resolve(FILE_NAME));
            }
            if (current.getParent() == null || current.getParent().equals(current)) break;//file system root
            current = current.getParent();
        }
        return found;
    }

    Path userDir = Paths.get(System.getProperty("user.dir"));
    /**
     * the root contains meta.properties
     */
    Path root = outer(userDir, userDir.toAbsolutePath().toString().split(Pattern.quote(File.separator)).length - 1, System.getProperty("modeler.config.debug") != null);

    @Data
    class Setting {
        boolean debug;
        boolean enabled;
    }

    record Config(Properties properties, Path path) {
        public <T> Optional<T> read(String key, Parser parser) {
            if (properties.containsKey(key)) {
                return Optional.of(parser.parse(properties.getProperty(key)));
            }
            return Optional.empty();
        }

        public Optional<Boolean> readBoolean(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Boolean.class));
        }

        public Optional<String> readString(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(String.class));
        }

        public Optional<Integer> readInteger(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Integer.class));
        }

        public Optional<Long> readLong(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Long.class));
        }

        public Optional<Double> readDouble(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Double.class));
        }

        public List<String> keys() {
            return properties.keySet().stream().map(x -> (String) x).toList();
        }

        public boolean enabled(@Nullable String prefix) {
            return readBoolean((prefix == null ? "" : prefix) + "enabled").orElse(true);
        }

        public boolean debug() {
            return readBoolean("debug").orElse(true);
        }

        public Optional<List<String>> accept(String prefix) {
            return readString(prefix + "accept").map(x -> Arrays.asList(x.split(",")));
        }

        @SneakyThrows
        public static Config parse(Path file) {
            var p = new Properties();
            p.load(file.toUri().toURL().openStream());
            return new Config(p, file);
        }
    }

    record Configs(SortedSet<Config> configs) {

        /**
         * globally enabled
         */
        public boolean isDebug() {
            return configs.stream().anyMatch(Config::debug);
        }

        /**
         * specific enabled
         */
        public boolean isEnabled(String prefix) {
            return configs.stream().filter(x -> !x.enabled(prefix)).findFirst().isEmpty();
        }

        /**
         * globally enabled
         */
        public boolean isEnabled() {
            return configs.stream().filter(x -> !x.enabled(null)).findFirst().isEmpty();
        }

        public <T> Optional<T> read(String key, Parser parser) {
            for (var c : configs) {
                if (c.properties.containsKey(key)) {
                    return Optional.of(parser.parse(c.properties.getProperty(key)));
                }
            }
            return Optional.empty();
        }

        public Optional<Boolean> readBoolean(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Boolean.class));
        }

        public Optional<String> readString(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(String.class));
        }

        public Optional<Integer> readInteger(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Integer.class));
        }

        public Optional<Long> readLong(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Long.class));
        }

        public Optional<Double> readDouble(String key) {
            return read(key, Parser.SIMPLE_TYPE.get(Double.class));
        }
    }

    AtomicReference<Config> rootCfg = new AtomicReference<>();

    @SneakyThrows
    static Pair<Setting, Map<String, Set<AbstractProcessor>>> parse() {
        if (root != null) {
            rootCfg.set(Config.parse(root.resolve(FILE_NAME)));
        } else {
            return null;
        }
        var s = new Setting();
        var rootConf = rootCfg.get();
        s.setDebug(rootConf.debug());
        s.setEnabled(rootConf.enabled(null));
        var keys = rootConf.keys();
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
            if (rootConf.enabled(pre)) {
                var cls = rootConf.readString(pre + "processor").orElseThrow(() -> new IllegalStateException("missing config of " + pre + "processor"));
                var i = Loader.loadClass(AbstractProcessor.class, cls);
                var accept = rootConf.accept(pre).orElseGet(i::acceptTypes);
                for (var k : accept) {
                    map.computeIfAbsent(k, x -> new HashSet<>()).add(i);
                }
                i.setPrefix(pre);
            }
        }
        return new Pair<>(s, map);
    }

    Map<Path, Configs> resolved = new ConcurrentHashMap<>();
    Comparator<Config> COMPARATOR = Comparator.<Config, Path>comparing(x -> x.path).reversed();

    static Configs resolve(ProcUtil u, Element element) {
        var uri = u.sourcePath(element);
        if (uri.getScheme() == null) {
            return resolved.computeIfAbsent(root, r -> {
                var t = new TreeSet<>(COMPARATOR);
                t.add(rootCfg.get());
                return new Configs(t);
            });
        }
        var path = Paths.get(uri).getParent();
        if (resolved.containsKey(path)) {
            return resolved.get(path);
        }
        var all = all(path, root);
        var s = new TreeSet<>(COMPARATOR);
        s.add(rootCfg.get());
        for (var p : all) {
            s.add(Config.parse(p));
            resolved.computeIfAbsent(p, x -> {
                var t = new TreeSet<>(COMPARATOR);
                t.addAll(s);
                return new Configs(t);
            }); //eager put
        }
        var cs = new Configs(s);
        resolved.put(path, cs);
        return cs;
    }

}
