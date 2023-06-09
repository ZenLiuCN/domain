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

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Exception with ErrorCode
 *
 * @author Zen.Liu
 * @since 2023-04-20
 */
@ApiStatus.AvailableSince("0.1.0")
public class ErrorCodeException extends RuntimeException {
    @Getter
    protected final int errorCode;

    public ErrorCodeException(int code) {
        this(code, (String) null);
    }

    public ErrorCodeException() {
        this(500);
    }

    public ErrorCodeException(@Nullable String message) {
        this(500, message);
    }

    public ErrorCodeException(int code, @Nullable String message) {
        this(code, message, null);
    }

    public ErrorCodeException(int code, @Nullable String message, @Nullable Throwable cause) {
        this(code, message, cause, false, true);
    }

    public ErrorCodeException(int code, @Nullable Throwable cause) {
        this(code, null, cause, false, true);
    }

    protected ErrorCodeException(int code, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message == null
                        ? cause == null ? "status %d".formatted(code) : "status %d,%s".formatted(code, cause)
                        : cause == null ? "status %d,%s".formatted(code, message) : "status %d,%s,cause of %s".formatted(code, message, cause),
                cause, enableSuppression, writableStackTrace);
        this.errorCode = code;
    }
}
