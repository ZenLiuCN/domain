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

package cn.zenliu.domain.modeler.prototype;

import cn.zenliu.domain.modeler.error.DomainError;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Injector is a simple DI abstraction.
 *
 * @author Zen.Liu
 * @since 2023-04-20
 */
public interface Injector {
    /**
     * register a Bean Provider, each time request this bean will generate new one from factory.
     *
     * @param type      bean exactly type
     * @param qualifier bean qualified name, if null it will use {@link Class#getName()}
     * @param factory   supplier
     */
    <T> void provider(Class<T> type, @Nullable String qualifier, Supplier<T> factory);

    /**
     * register a Singleton Bean with Qualifier.
     *
     * @param type      bean exactly type
     * @param qualifier bean qualified name, if null it will use {@link Class#getName()}
     * @param factory   supplier
     */
    <T> void singleton(Class<T> type, @Nullable String qualifier, Supplier<T> factory);

    /**
     * register a Singleton Instance Bean with Qualifier.
     *
     * @param type      bean exactly type
     * @param qualifier bean qualified name, if null it will use {@link Class#getName()}
     * @param value     the Bean instance
     */

    <T> void singleton(Class<T> type, @Nullable String qualifier, T value);

    /**
     * same as {@link #one(Class, String)} with null name.
     *
     * @param type bean exactly type.
     * @see #one(Class, String)
     */
    default <T> Optional<Supplier<T>> one(Class<T> type) {
        return one(type, null);
    }

    /**
     * fetch one bean defined as a type and an optional qualified name.
     *
     * @param type the bean exactly type
     * @param name the bean qualified name, null for use {@link Class#getName()}
     * @return all defined bean of this type
     */
    <T> Optional<Supplier<T>> one(Class<T> type, @Nullable String name);

    /**
     * fetch all bean defined as a type.
     *
     * @param type the bean exactly type
     * @return all defined bean of this type
     */
    <T> Map<String, Supplier<T>> all(Class<T> type);

    class Factories extends ConcurrentHashMap<String, Supplier<Object>> {
        public Factories() {
            super();

        }

        public Factories(int initialSize) {
            super(initialSize);
        }
    }

    class FactorySupplier implements Supplier<Object> {
        protected final Supplier<Object> fac;


        public FactorySupplier(Supplier<Object> fac) {
            this.fac = fac;
        }

        @Override
        public Object get() {
            return fac.get();
        }
    }

    class LazyFactory implements Supplier<Object> {
        protected final Supplier<Object> fac;
        protected Object val;

        public LazyFactory(Supplier<Object> fac) {
            this.fac = fac;
        }

        @Override
        public Object get() {
            if (val == null) synchronized (fac) {
                val = fac.get();
            }
            return val;
        }
    }

    class InstanceFactory implements Supplier<Object> {
        protected final Object val;

        public InstanceFactory(Object val) {
            this.val = val;
        }


        @Override
        public Object get() {
            return val;
        }
    }

    @SuppressWarnings("unchecked")
    class MapInjector implements Injector {
        protected final Map<Class<?>, Factories> container;

        public MapInjector() {
            this.container = new ConcurrentHashMap<>();
        }

        public MapInjector(int initSize) {
            this.container = new ConcurrentHashMap<>(initSize);
        }


        @Override
        public <T> void provider(Class<T> type, @Nullable String qualifier, Supplier<T> factory) {
            qualifier = qualifier == null ? type.getName() : qualifier;
            final Factories fac;
            if (container.containsKey(type)) {
                fac = container.get(type);
                if (fac.containsKey(qualifier)) {
                    throw DomainError.conflict("bean '" + qualifier + "':" + type + " conflict already defined.", null);
                }
            } else {
                synchronized (container) {
                    fac = new Factories();
                    container.put(type, fac);
                }
            }
            fac.put(qualifier, new FactorySupplier((Supplier<Object>) factory));
        }

        @Override
        public <T> void singleton(Class<T> type, @Nullable String qualifier, Supplier<T> factory) {
            qualifier = qualifier == null ? type.getName() : qualifier;
            final Factories fac;
            if (container.containsKey(type)) {
                fac = container.get(type);
                if (fac.containsKey(qualifier)) {
                    throw DomainError.conflict("bean '" + qualifier + "':" + type + " conflict already defined.", null);
                }
            } else {
                synchronized (container) {
                    fac = new Factories();
                    container.put(type, fac);
                }
            }
            fac.put(qualifier, new LazyFactory((Supplier<Object>) factory));
        }

        @Override
        public <T> void singleton(Class<T> type, @Nullable String qualifier, T value) {
            qualifier = qualifier == null ? type.getName() : qualifier;
            final Factories fac;
            if (container.containsKey(type)) {
                fac = container.get(type);
                if (fac.containsKey(qualifier)) {
                    throw DomainError.conflict("bean '" + qualifier + "':" + type + " conflict already defined.", null);
                }
            } else {
                synchronized (container) {
                    fac = new Factories();
                    container.put(type, fac);
                }
            }
            fac.put(qualifier, new InstanceFactory(value));
        }

        @Override
        public <T> Optional<Supplier<T>> one(Class<T> type, String name) {
            name = name == null ? type.getName() : name;
            if (!container.containsKey(type)) {
                return Optional.empty();
            } else {
                return Optional.ofNullable((Supplier<T>) container.get(type).get(name));
            }
        }

        @Override
        public <T> Map<String, Supplier<T>> all(Class<T> type) {
            if (!container.containsKey(type)) {
                return Collections.emptyMap();
            } else {
                return (Map<String, Supplier<T>>) (Map<?, ?>) container.get(type);
            }
        }
    }

    static Injector map(Integer initialSize) {
        return initialSize == null ? new MapInjector() : new MapInjector(initialSize);
    }
}
