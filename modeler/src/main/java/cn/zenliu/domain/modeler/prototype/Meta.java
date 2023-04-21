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

import lombok.SneakyThrows;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Meta markers.
 *
 * @author Zen.Liu
 * @since 2023-04-20
 */
@ApiStatus.AvailableSince("0.1.0")
public interface Meta {
    /**
     * when extended by interface: current interface is Domain Object
     */
    @ApiStatus.AvailableSince("0.1.0")  interface Object {

    }

    /**
     * when extended by interface: current interface is a Generated Domain Entity, which must have a inherited Domain Object
     */
    @ApiStatus.AvailableSince("0.1.0") interface Entity {

    }

    /**
     * when extended by interface: current interface is Domain Value
     */
    @ApiStatus.AvailableSince("0.1.0") interface Value {

    }

    /**
     * when extended by interface: current interface is Domain UseCase
     */
    @ApiStatus.AvailableSince("0.1.0") interface UseCase {

    }

    /**
     * when extended by interface: current interface is Domain Port
     */
    @ApiStatus.AvailableSince("0.1.0") interface Port {

    }

    /**
     * when extended by interface: current interface is Domain Event
     */
    @ApiStatus.AvailableSince("0.1.0") interface Event {

    }

    /**
     * An interface Meta Information as constant
     */
    @ApiStatus.AvailableSince("0.1.0") interface Fields {
        /**
         * static final field name suffix for hold the type class.
         */
        String TYPE_SUFFIX = "_TYPE";

        /**
         * use reflect to dynamic read fields from class,the result should be cached.
         */
        @SneakyThrows
        default Map<String, @Nullable Class<?>> fields() {
            var out = new HashMap<String, Class<?>>();
            for (var field : this.getClass().getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (field.getDeclaringClass() == Fields.class) continue;

                    var name = field.getName();
                    if (!name.toUpperCase().equals(name)) continue;

                    if (name.endsWith(TYPE_SUFFIX)) {
                        name = name.substring(0, name.indexOf(TYPE_SUFFIX));
                        out.put(name, (Class<?>) field.get(null));
                    } else if (field.getType() == String.class) {
                        out.put(name, (Class<?>) null);
                    }
                }
            }
            return out;
        }
    }

    /**
     * when use with interface: current interface is Domain Port Adapter Provider
     */
    @ApiStatus.AvailableSince("0.1.0") interface Provider {
        /**
         * @return the Initializer info or empty
         */
        default Optional<Initializer> initializer() {
            return Optional.empty();
        }
    }

}
