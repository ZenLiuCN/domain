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

import org.immutables.value.Value;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.*;

/**
 * Just a container of generation modifier annotations
 *
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Mode {
    /**
     * Mark a Type is Prototype that should be processed by generators
     */
    @Target({ElementType.TYPE})
    @Documented
    @ApiStatus.AvailableSince("0.1.3")
    @interface Prototype {

    }

    /**
     * marker a field with no Setter generated for a XXXEntity type. effect with {@link Gene.Entity}
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Documented
    @ApiStatus.AvailableSince("0.1.0")
    @Inherited
    @interface ReadOnly {

    }

    /**
     * mark on a none java bean style getter method as a Field.<br/> when mark on a Domain Object to define all getter
     * like methods (returns not void,accept nothing) are Field.<br/> effect with {@link Gene.Entity} and
     * {@link  Gene.Fields}
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Documented
    @ApiStatus.AvailableSince("0.1.0")
    @Inherited
    @interface Field {

    }

    /**
     * suffix style of Immutables: generate immutable as *Val and Mutable as *Var
     */
    @Target({ElementType.TYPE})
    @Documented
    @Value.Style(
            typeImmutable = "*Val",
            typeModifiable = "*Var",
            defaults = @Value.Immutable(lazyhash = true)
    )
    @ApiStatus.AvailableSince("0.1.0")
    @interface Values {
    }

}
