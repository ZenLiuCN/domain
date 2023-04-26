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

package cn.zenliu.domain.modeler.annotation;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.*;

/**
 * Just a container of Markers,Marker are just annotation of principles, without any other magics.
 *
 * @author Zen.Liu
 * @since 2023-04-21
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Mark {

    /**
     * Unique field marker
     */

    @Target({ElementType.METHOD, ElementType.FIELD})
    @Documented
    @Inherited
    @ApiStatus.AvailableSince("0.1.0")
    @interface Unique {
    }

    /**
     * Container not empty, or String not blank
     */

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Documented
    @Inherited
    @ApiStatus.AvailableSince("0.1.0")
    @interface NotEmpty {
    }

    /**
     * field link to another object or objects
     */
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Documented
    @Inherited
    @ApiStatus.AvailableSince("0.1.0")
    @interface Ref {

        /**
         * another direction of references. Value's format should use java document link style (entity#field).
         */
        String value() default "";

    }

    /**
     * Lookup field marker
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Documented
    @Inherited
    @ApiStatus.AvailableSince("0.1.0")
    @interface Lookup {
        /**
         * composited with fields
         */
        String[] value() default {};
    }

    /**
     * same as JPA
     */
    @Target({ElementType.TYPE})
    @Documented
    @Inherited
    @ApiStatus.AvailableSince("0.1.0")
    @interface Embeddable {

    }

    /**
     * same as JPA
     */
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Documented
    @Inherited
    @ApiStatus.AvailableSince("0.1.0")
    @interface Embedded {

    }

}
