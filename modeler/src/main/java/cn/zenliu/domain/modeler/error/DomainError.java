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

package cn.zenliu.domain.modeler.error;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nullable;

/**
 * Domain Exception.<br/>
 * <b>Note:</b> user message not write to super exceptions.
 *
 * @author Zen.Liu
 * @since 2023-04-20
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@ApiStatus.AvailableSince("0.1.0")
public class DomainError extends ErrorCodeException {
    @Getter
    protected final String userMessage;

    public DomainError(int code, @Nullable String userMessage) {
        this(code, userMessage, null, null, false, true);
    }

    public DomainError(@Nullable String userMessage) {
        this(500, userMessage, null, null, false, true);
    }

    public DomainError() {
        this(500, null, null, null, false, true);
    }

    public DomainError(int code, @Nullable String userMessage, @Nullable String message) {
        this(code, userMessage, message, null, false, true);
    }

    public DomainError(int code, @Nullable String userMessage, @Nullable String message, Throwable cause) {
        this(code, userMessage, message, cause, false, true);
    }

    public DomainError(int code, Throwable cause) {
        this(code, null, null, cause, false, true);
    }

    public DomainError(int code, @Nullable String userMessage, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(code, message, cause, enableSuppression, writableStackTrace);
        this.userMessage = userMessage;
    }

    /**
     * Construct a {@link GoneError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static GoneError gone(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new GoneError(null, message, null);
        if (args == null || args.length == 0) return new GoneError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new GoneError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link TimeoutError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static TimeoutError timeout(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new TimeoutError(null, message, null);
        if (args == null || args.length == 0) return new TimeoutError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new TimeoutError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link UnauthorizedError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static UnauthorizedError unauthorized(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new UnauthorizedError(null, message, null);
        if (args == null || args.length == 0) return new UnauthorizedError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new UnauthorizedError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link  BadRequestError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static BadRequestError badRequest(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new BadRequestError(null, message, null);
        if (args == null || args.length == 0) return new BadRequestError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new BadRequestError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link  NotFoundError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static NotFoundError notFound(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new NotFoundError(null, message, null);
        if (args == null || args.length == 0) return new NotFoundError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new NotFoundError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link  InternalServerError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static InternalServerError internal(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new InternalServerError(null, message, null);
        if (args == null || args.length == 0) return new InternalServerError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new InternalServerError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link  ConflictError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static ConflictError conflict(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new ConflictError(null, message, null);
        if (args == null || args.length == 0) return new ConflictError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new ConflictError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link  ServiceUnavailableError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static ServiceUnavailableError serviceUnavailable(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new ServiceUnavailableError(null, message, null);
        if (args == null || args.length == 0) return new ServiceUnavailableError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new ServiceUnavailableError(u.getMessage(), message, u.getThrowable());
    }

    /**
     * Construct a {@link  ForbiddenError}
     *
     * @param message system message for exception
     * @param pattern user message pattern (SL4FJ format), null for no use message.
     * @param args    args with user message.
     */
    public static ForbiddenError forbidden(@Nullable String message, @Nullable String pattern, Object... args) {
        if (pattern == null) return new ForbiddenError(null, message, null);
        if (args == null || args.length == 0) return new ForbiddenError(pattern, message, null);
        var u = MessageFormatter.arrayFormat(pattern, args);
        return new ForbiddenError(u.getMessage(), message, u.getThrowable());
    }
}
