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

package cn.zenliu.domain.modeler.processor;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.ApiStatus;

import javax.lang.model.element.ExecutableElement;

/**
 * @author Zen.Liu
 * @since 2023-04-22
 */
@ApiStatus.AvailableSince("0.1.2")
public class SetterGeneVisitor extends BaseGetterVisitor {
    private final boolean chain;
    private final TypeName self;

    SetterGeneVisitor(ProcUtil u, boolean chain, boolean beanStyle,TypeName self) {
        super(beanStyle, u);
        this.chain = chain;
        this.self = self;
    }

    @Override
    public TypeSpec.Builder visitExecutable(ExecutableElement e, TypeSpec.Builder builder) {
        if (notGetter(e, true)) return builder;
        final String n = toSetterName(e);
        if (n == null || hasDeclaredSetter(n, e)) return builder;
        u.other("Generate Setter from method: {}", e);
        var m = declareSetter(n, e);
        return builder.addMethod(chain ? m.returns(self == null ? TypeName.get(root) : self).build() : m.build());
    }

}
