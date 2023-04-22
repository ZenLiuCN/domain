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

import cn.zenliu.domain.modeler.prototype.Meta;

import java.lang.annotation.*;

/**
 * Just a Container of Generate Annotations
 *
 * @author Zen.Liu
 * @since 2023-04-20
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Gene {
    /**
     * Generate a full XXXEntity interface (with Setters) from {@link Meta.Object}.<br/>
     * if already have a  {@link Mutate}, will just directly inherit generated {@link Meta.Trait}.<br/>
     * <b>Note:</b> also effected as {@link Fields}
     */
    @Target({ElementType.TYPE})
    @Documented
    @Fields
    @interface Entity {
    }

    /**
     * Generate a Constant XXXFields interface from a {@link Meta.Object}.<br/>
     * <pre>
     * {@code @Generated(...)
     * public interface UserFields extends User, Meta.Fields{
     * String ID="id";
     * Class  ID_TYPE=String.class;
     * }
     * }
     * </pre>
     */
    @Target({ElementType.TYPE})
    @Documented
    @interface Fields {
    }

    /**
     * Generate a Mutate Trait (Object) for Annotated Type of {@link Meta.Object} or {@link Meta.Trait}.
     * The result will inherit {@link Meta.Trait}.
     */
    @Target({ElementType.TYPE})
    @Documented
    @interface Mutate {

    }
}
