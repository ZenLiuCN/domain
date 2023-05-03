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

package cn.zenliu.domain.modeler.util;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarFile;

import static cn.zenliu.domain.modeler.processor.safer.Configurer.debugf;

/**
 * @author Zen.Liu
 * @since 2023-04-20
 */
public interface Loader {
    static boolean classExists(String name, @Nullable ClassLoader loader) {
        loader = loader == null ? Thread.currentThread().getContextClassLoader() : loader;
        try {
            loader.loadClass(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * load a MapStruct mapper.
     */
    static <T> T mapper(Class<T> cls) {
        return load(cls, "Impl");
    }

    /**
     * load a implement of cls with suffix.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> T load(Class<T> cls, String suffix) {
        return (T) cls.getClassLoader().loadClass(cls.getName() + suffix).getConstructor().newInstance();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> T loadClass(Class<T> cls, String clsName) {
        return (T) cls.getClassLoader().loadClass(clsName).getConstructor().newInstance();
    }

    String POM_VERSION_UNKNOWN = "unknown";

    /**
     * @param type type class in a Jar
     * @return POM_VERSION_UNKNOWN if failed
     */
    static String loadPomVersion(Class<?> type) {
        try {
            var file = type.getProtectionDomain().getCodeSource().getLocation().getFile();
            debugf("Locate Jar file {}", file);
            if (file.endsWith("jar")) {
                try (var jar = new JarFile(file)) {
                    var it = jar.entries().asIterator();
                    while (it.hasNext()) {
                        var e = it.next();
                        debugf("Lookup Jar entry {}", e);
                        if (e.getName().endsWith("pom.properties")) {
                            var prop = new Properties();
                            prop.load(type.getClassLoader().getResourceAsStream(e.getRealName()));
                            debugf("Load Jar pom.properties {}", prop);
                            return prop.getProperty("version");
                        }
                    }
                }
            } else { //from path
                var prop = Files.find(Paths.get(file.indexOf('/') == 0 && file.indexOf(':') > 0 ? file.substring(1) : file),
                                20,
                                (p, a) -> a.isRegularFile() && p.endsWith("pom.properties"), FileVisitOption.FOLLOW_LINKS)
                        .findFirst();
                if (prop.isPresent()) {
                    var p = new Properties();
                    p.load(prop.get().toUri().toURL().openStream());
                    return p.getProperty("version");
                }
            }
            return POM_VERSION_UNKNOWN;
        } catch (IOException e) {
            return POM_VERSION_UNKNOWN;
        }
    }
}
