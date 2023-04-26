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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasesTest {
    @Test
    void testToPascal() {
        assertEquals("P", Cases.toPascal("p").toString());
        assertEquals("PascalCase", Cases.toPascal("pascalCase").toString());
        assertEquals("PascalCase", Cases.toPascal("pascal_case").toString());
        assertEquals("PascalCase", Cases.toPascal("pascal-case").toString());
        assertEquals("PascalCase", Cases.toPascal("pascal-Case").toString());
        assertEquals("PASCase", Cases.toPascal("PAS-Case").toString());
        assertEquals("PASCase", Cases.toPascal("PAS_Case").toString());
        assertEquals("PASCase", Cases.toPascal("PASCase").toString());
        assertEquals("PASCaseC", Cases.toPascal("PASCaseC").toString());
    }

    @Test
    void testToCamel() {
        assertEquals("p", Cases.toCamel("P").toString());
        assertEquals("pascalCase", Cases.toCamel("pascalCase").toString());
        assertEquals("pascalCase", Cases.toCamel("Pascal_case").toString());
        assertEquals("pascalCase", Cases.toCamel("pascal-case").toString());
        assertEquals("pascalCase", Cases.toCamel("pascal-Case").toString());
        assertEquals("pascalCase", Cases.toCamel("PASCAL-Case").toString());
        assertEquals("pasCase", Cases.toCamel("PAS-Case").toString());
        assertEquals("pasCase", Cases.toCamel("PAS_Case").toString());
        assertEquals("pasCase", Cases.toCamel("PASCase").toString());
        assertEquals("pasCaseC", Cases.toCamel("PASCaseC").toString());
    }

    @Test
    void testToSnake() {
        assertEquals("p", Cases.toSnake("P").toString());
        assertEquals("pascal_case", Cases.toSnake("pascalCase").toString());
        assertEquals("pascal_case", Cases.toSnake("pascal_case").toString());
        assertEquals("pascal_case", Cases.toSnake("Pascal-case").toString());
        assertEquals("pascal_case", Cases.toSnake("pascal-Case").toString());
        assertEquals("pascal_case", Cases.toSnake("PASCAL-Case").toString());
        assertEquals("pas_case", Cases.toSnake("PAS-Case").toString());
        assertEquals("pas_case", Cases.toSnake("PAS_Case").toString());
        assertEquals("pas_case", Cases.toSnake("PASCase").toString());
        assertEquals("pas_case_c", Cases.toSnake("PASCaseC").toString());
    }

    @Test
    void testToScreaming() {
        assertEquals("P", Cases.toScreaming("p").toString());
        assertEquals("PASCAL_CASE", Cases.toScreaming("pascalCase").toString());
        assertEquals("PASCAL_CASE", Cases.toScreaming("pascal_case").toString());
        assertEquals("PASCAL_CASE", Cases.toScreaming("Pascal-case").toString());
        assertEquals("PASCAL_CASE", Cases.toScreaming("pascal-Case").toString());
        assertEquals("PASCAL_CASE", Cases.toScreaming("PASCAL-Case").toString());
        assertEquals("PAS_CASE", Cases.toScreaming("PAS-Case").toString());
        assertEquals("PAS_CASE", Cases.toScreaming("PAS_Case").toString());
        assertEquals("PAS_CASE", Cases.toScreaming("PASCase").toString());
        assertEquals("PAS_CASE_C", Cases.toScreaming("PASCaseC").toString());
    }

    @Test
    void testToWords() {
        assertEquals(List.of("P"), Cases.toWords("p"));
        assertEquals(List.of("pascal", "Case"), Cases.toWords("pascalCase"));
        assertEquals(List.of("pascal", "case"), Cases.toWords("pascal_case"));
        assertEquals(List.of("Pascal", "case"), Cases.toWords("Pascal-case"));
        assertEquals(List.of("pascal", "Case"), Cases.toWords("pascal-Case"));
        assertEquals(List.of("PASCAL", "Case"), Cases.toWords("PASCAL-Case"));
        assertEquals(List.of("PAS", "Case"), Cases.toWords("PAS-Case"));
        assertEquals(List.of("PAS", "Case"), Cases.toWords("PAS_Case"));
        assertEquals(List.of("PAS", "Case"), Cases.toWords("PASCase"));
        assertEquals(List.of("PAS", "Case", "C"), Cases.toWords("PASCaseC"));
    }
}