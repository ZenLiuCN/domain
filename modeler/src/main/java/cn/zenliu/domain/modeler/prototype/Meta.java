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

import cn.zenliu.domain.modeler.annotation.Gene;
import cn.zenliu.domain.modeler.annotation.Mode;
import cn.zenliu.domain.modeler.processor.GeneEntity;
import cn.zenliu.domain.modeler.processor.GeneFields;
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
     *
     * @see Gene.Entity
     * @see Gene.Fields
     * @see Gene.Mutate
     * @see Mode.ReadOnly
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Object extends Meta {

    }

    /**
     * when extended by interface: current interface is a Generated Domain Entity, which must have a inherited Domain Object.
     *
     * @see GeneEntity
     * @see Gene.Entity
     * @see Mode.ReadOnly
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Entity extends Meta {
        String SUFFIX="Entity";
    }

    /**
     * when extended by interface: current interface is Domain Value
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Value extends Meta {


    }

    /**
     * when extended by interface: current interface is Domain UseCase
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface UseCase extends Meta {

    }

    /**
     * when extended by interface: current interface is Domain Port
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Port extends Meta {

    }

    /**
     * when extended by interface: current interface is Domain Event
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Event extends Meta {

    }

    /**
     * An interface Meta Information as constant
     *
     * @see GeneFields
     * @see Gene.Fields
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Fields extends Meta {
        String SUFFIX="Fields";
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
     * when use with interface: current interface is Domain Port Adaptor Provider
     */
    @ApiStatus.AvailableSince("0.1.0")
    interface Provider extends Meta {
        /**
         * @return the Initializer info or empty
         */
        default Optional<Initializer> initializer() {
            return Optional.empty();
        }
    }

    /**
     * An interface with predefined properties and actions.
     *
     * @see Gene.Mutate
     */
    @ApiStatus.AvailableSince("0.1.2")
    interface Trait extends Meta {
        String SUFFIX="Trait";
        String MUTABLE_SUFFIX="Mutate";
    }

    /**
     * Object Style Entity is an entity with save and delete method.<br/>
     * This will only affect annotated with both of  {@link Gene.Mutate} and  {@link Gene.Entity}, And not already inherited from {@link Entity}.
     */
    @ApiStatus.AvailableSince("0.1.5")
    interface ObjectStyleEntity extends Entity {
        boolean delete();

        void save();

        /**
         * transaction save
         *
         * @param transaction transaction object
         */
        default void save(java.lang.Object transaction) {

        }

        /**
         * transaction delete
         *
         * @param transaction transaction object
         * @return whether succeed
         */
        default boolean delete(java.lang.Object transaction) {
            return false;
        }
    }

    /**
     * Adaptor is an optional middle layer between Definition and Implement, which may generated by {@link Gene.Adapt}.
     */
    interface Adaptor extends Meta {
        String SUFFIX="Adaptor";
        String CLASS_SUFFIX="Adapter";
    }

}
