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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Result declared with three state:<br/>
 * 1. Success: value may be null, also no error present.<br/>
 * 2. Failure: value is null, maybe have error present.<br/>
 * 3. Warning: value maybe null, maybe have error present, but the error not critical.<br/>
 *
 * @author Zen.Liu
 * @since 2023-04-13
 */

public interface Result<T> {
    /**
     * get data or throw error
     */
    T get();

    /**
     * peek the data without check error
     */
    T peek();

    /**
     * fetch the error or null.
     */
    Throwable error();

    default <R> Result<R> map(Function<T, R> fn) {
        var err = error();
        if (err != null) {
            return fail(err);
        }
        return run(() -> fn.apply(peek()));
    }

    default T orElse(T v) {
        var err = error();
        if (err != null) {
            return v;
        }
        return peek();
    }

    @SneakyThrows
    default void then(Consumer<T> fn) {
        var err = error();
        if (err != null) {
            throw err;
        }
        fn.accept(peek());
    }

    /**
     *  this method will eat the error
     * @param fn the mapping function
     * @return value or empty
     */
    default <R> Optional<R> then(Function<T, R> fn) {
        var err = error();
        if (err != null) {
            return Optional.empty();
        } else
            return Optional.ofNullable(fn.apply(peek()));
    }
    default void then(Consumer<T> fn, Consumer<Throwable> errFn) {
        var err = error();
        if (err != null) {
            errFn.accept(err);
        } else
            fn.accept(peek());
    }

    default <R> Optional<R> then(Function<T, R> fn, Consumer<Throwable> errFn) {
        var err = error();
        if (err != null) {
            errFn.accept(err);
            return Optional.empty();
        } else
            return Optional.ofNullable(fn.apply(peek()));
    }

    static <T> Result<T> ok(T v) {
        return new Success<>(v);
    }

    static <T> Result<T> warn(T v, Throwable err) {
        return new Warning<>(v, err);
    }

    static <T> Result<T> fail(Throwable v) {
        return new Failure<>(v);
    }

    static <T> Result<T> run(Supplier<T> v) {
        try {
            return ok(v.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    static <T> Result<T> run(Runnable v) {
        try {
            v.run();
            return ok(null);
        } catch (Exception e) {
            return new Warning<>(null, e);
        }
    }

    class Success<T> implements Result<T> {
        private final T v;

        public Success(T v) {
            this.v = v;
        }

        @Override
        public T get() {
            return v;
        }

        @Override
        public T peek() {
            return v;
        }

        @Override
        public Throwable error() {
            return null;
        }
    }

    class Failure<T> implements Result<T> {
        private final Throwable e;

        public Failure(Throwable e) {
            this.e = e;
        }

        @SneakyThrows
        @Override
        public T get() {
            if (e != null) throw e;
            return null;
        }

        @Override
        public T peek() {
            return null;
        }

        @Override
        public Throwable error() {
            return e;
        }
    }

    class Warning<T> implements Result<T> {
        private final T v;
        private final Throwable err;

        public Warning(T v, Throwable err) {
            this.v = v;
            this.err = err;
        }

        @Override
        public T get() {
            return v;
        }

        @Override
        public T peek() {
            return v;
        }

        @Override
        public Throwable error() {
            return err;
        }
    }

}
