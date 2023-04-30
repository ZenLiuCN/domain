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

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A proxy HashSet delegate a collection actions as a set.<br/>
 * 1. it may change the source to remove already exists duplicated elements.<br/>
 * 2. all final result will affect on underlying collection.
 *
 * @author Zen.Liu
 * @since 2023-04-30
 */
@ApiStatus.AvailableSince("0.1.6")
public interface ProxySet<E, C extends Collection<E>> extends Set<E> {
    C unwrap();

    @Override
    default int size() {
        return unwrap().size();
    }

    @Override
    default boolean isEmpty() {
        return unwrap().isEmpty();
    }

    @Override
    default boolean contains(Object o) {
        return unwrap().contains(o);
    }

    @NotNull
    @Override
    default Iterator<E> iterator() {
        return unwrap().iterator();
    }

    @NotNull
    @Override
    default Object[] toArray() {
        return unwrap().toArray();
    }

    @NotNull
    @Override
    default <T> T[] toArray(@NotNull T[] a) {
        return unwrap().toArray(a);
    }

    @Override
    default boolean add(E e) {
        return !contains(e) && unwrap().add(e);
    }

    @Override
    default boolean remove(Object o) {
        return unwrap().remove(o);
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        return unwrap().containsAll(c);
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends E> c) {
        for (E e : c) {
            if (unwrap().contains(e)) return false;
        }
        return unwrap().addAll(c);
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
        return unwrap().retainAll(c);
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        return unwrap().removeAll(c);
    }

    @Override
    default void clear() {
        unwrap().clear();
    }

    @Override
    default boolean equals(Object o) {
        if (o instanceof ProxySet<?, ?> p) {
            return Objects.equals(p.unwrap(), unwrap());
        }
        return unwrap().equals(o);
    }

    @Override
    default int hashCode() {
        return unwrap().hashCode();
    }

    /**
     * a implement with extra hash set check.
     *
     * @param <E> element type
     * @param <C> collection type
     */
    final class Impl<E, C extends Collection<E>> implements ProxySet<E, C> {
        final Set<Integer> hash;
        final C unwrap;
        final boolean eager;

        public Impl(@NonNull C o, boolean eager) {
            this.unwrap = o;
            this.hash = ConcurrentHashMap.newKeySet(o.size() == 0 ? 16 : o.size());
            this.eager = eager;
            if (eager) {
                unwrap.removeIf(e -> !hash.add(e.hashCode()));
            }
        }

        private void initHash() {
            if (!eager && hash.isEmpty()) {
                synchronized (hash) {
                    if (hash.isEmpty()) {
                        unwrap.removeIf(e -> !hash.add(e.hashCode()));
                    }

                }
            }
        }

        @Override
        public C unwrap() {
            return unwrap;
        }

        @Override
        public boolean add(E e) {
            initHash();
            if (hash.add(e.hashCode()))
                unwrap.add(e);
            return false;
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends E> c) {
            initHash();
            if (c.isEmpty()) return false;
            var temp = new ArrayList<E>();
            for (var v : c) {
                if (hash.add(v.hashCode())) {
                    temp.add(v);
                }
            }
            if (temp.isEmpty()) return false;
            return unwrap.addAll(temp);
        }

        @Override
        public boolean contains(Object o) {
            if (o == null) return false;
            initHash();
            return hash.contains(o.hashCode());
        }

        @Override
        public boolean remove(Object o) {
            if (o == null) return false;
            initHash();
            if (!hash.contains(o.hashCode())) return false;
            return unwrap.remove(o);
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            initHash();
            for (var o : c) {
                if (o == null || !hash.contains(o.hashCode())) return false;

            }
            return true;
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            initHash();
            var modified = false;
            var it = unwrap.iterator();
            while (it.hasNext()) {
                var e = it.next();
                if (!c.contains(e)) {
                    hash.remove(e.hashCode());
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            initHash();
            var modified = false;
            var it = unwrap.iterator();
            while (it.hasNext()) {
                var e = it.next();
                if (c.contains(e)) {
                    hash.remove(e.hashCode());
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            var removed = false;
            var each = iterator();
            while (each.hasNext()) {
                var e = each.next();
                if (filter.test(e)) {
                    hash.remove(e.hashCode());
                    each.remove();
                    removed = true;
                }
            }
            return removed;
        }
    }

    static <E, C extends Collection<E>> ProxySet<E, C> of(C collection, boolean eager) {
        return new Impl<>(collection, eager);
    }
}
