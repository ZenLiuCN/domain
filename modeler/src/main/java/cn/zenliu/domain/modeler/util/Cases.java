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

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Character.*;

/**
 * @author Zen.Liu
 * @since 2023-04-26
 */
public interface Cases {
    enum Type {
        /**
         * Not known case type.
         */
        UNKNOWN,
        /**
         * Also known as small Pascal, Pascal Style with first letter lowercase.
         */
        CAMEL,
        /**
         * Each word is capitalized and without any delimiter. Usually use for ClassName or FieldName in JavaBean style
         * getter setter.
         */
        PASCAL,
        /**
         * Full lower-cased letters, word delimited with underscore .
         */
        SNAKE,
        /**
         * Full upper-cased letters, word delimited with underscore.
         */
        SCREAMING,
    }

    /**
     * @param src source
     * @return simple change first char to lower-case
     */
   @ApiStatus.AvailableSince("0.1.3") static CharSequence toCamelSimple(CharSequence src) {
        if (src == null || src.isEmpty()) return src;
        if (src.length() == 1) return String.valueOf(toLowerCase(src.charAt(0)));
        return new StringBuilder().append(toLowerCase(src.charAt(0))).append(src.subSequence(1, src.length()));
    }

    /**
     * @param src source
     * @return simple change first char to upper-case
     */
   @ApiStatus.AvailableSince("0.1.3") static CharSequence toPascalSimple(CharSequence src) {
        if (src == null || src.isEmpty()) return src;
        if (src.length() == 1) return String.valueOf(toUpperCase(src.charAt(0)));
        return new StringBuilder().append(toUpperCase(src.charAt(0))).append(src.subSequence(1, src.length()));
    }

    /**
     * loop to convert into Pascal Case , word split by none-letter character or upper-case character.<br/>
     * <b>note:</b> Abbreviations are keep in upper-case.
     *
     * @param src source
     * @return result into PascalCase with abbreviation keep in upper-case
     */
   @ApiStatus.AvailableSince("0.1.3") static CharSequence toPascal(CharSequence src) {
        if (src == null || src.isEmpty()) return src;
        if (src.length() == 1) return String.valueOf(toUpperCase(src.charAt(0)));
        var b = new StringBuilder();
        var w = new StringBuilder();
        for (var i = 0; i < src.length(); i++) {
            var c = src.charAt(i);
            if (isAlphabetic(c)) {
                if (isUpperCase(c)) {
                    if (!w.isEmpty()) {
                        b.append(toUpperCase(w.charAt(0))).append(w.subSequence(1, w.length()));
                        w.setLength(0);
                    }
                }
                w.append(c);
            } else {
                if (!w.isEmpty()) {
                    b.append(toUpperCase(w.charAt(0))).append(w.subSequence(1, w.length()));
                    w.setLength(0);
                }
            }
        }
        //final word
        if (!w.isEmpty()) {
            b.append(toUpperCase(w.charAt(0))).append(w.subSequence(1, w.length()));
        }
        return b;
    }

    /**
     * loop to convert into Camel Case , word split by none-letter character or upper-case character.<br/>
     * <b>note:</b> Abbreviations are keep in upper-case, but if it's a start word will keep in full lower-case form.
     *
     * @param src source
     * @return result
     */
  @ApiStatus.AvailableSince("0.1.3")  static CharSequence toCamel(CharSequence src) {
        if (src == null || src.isEmpty()) return src;
        if (src.length() == 1) return String.valueOf(toLowerCase(src.charAt(0)));
        var b = new StringBuilder();
        var w = new StringBuilder();
        var pp = 0;
        var pc = false;
        for (var i = 0; i < src.length(); i++) {
            var c = src.charAt(i);
            if (isAlphabetic(c)) {
                if (isUpperCase(c)) {
                    if (i == 0) {//first initial
                        pp++;
                        pc = true;
                        w.append(c);
                        continue;
                    }
                    if (pc && pp > 0) {//continue upper-case
                        w.append(c);
                        pp++;
                        continue;
                    }
                    addCamelWord(b, w);
                    pp += pc ? 1 : 0;
                    pc = true;
                } else {
                    if (!w.isEmpty()) {
                        var lc = w.charAt(w.length() - 1);
                        if (isUpperCase(lc)) {
                            w.deleteCharAt(w.length() - 1);
                            addCamelWord(b, w);
                            w.append(lc);
                        }
                    }
                    pp = 0;
                    pc = false;
                }
                w.append(c);
            } else {
                pp = 0;
                pc = false;
                addCamelWord(b, w);
            }
        }
        //final word
        if (!w.isEmpty()) {
            b.append(toUpperCase(w.charAt(0))).append(w.subSequence(1, w.length()));
        }
        return b;
    }

    /**
     * loop to convert into Snake Case , word split by none-letter character or upper-case character.<br/>
     * <b>note:</b> Abbreviations are keep in lower-case as one word.
     *
     * @param src source
     * @return result
     */
   @ApiStatus.AvailableSince("0.1.3") static CharSequence toSnake(CharSequence src) {
        if (src == null || src.isEmpty()) return src;
        if (src.length() == 1) return String.valueOf(toLowerCase(src.charAt(0)));


        var b = new StringBuilder();
        var w = new StringBuilder();
        var pp = 0;
        var pc = false;
        for (var i = 0; i < src.length(); i++) {
            var c = src.charAt(i);
            if (isAlphabetic(c)) {
                if (isUpperCase(c)) {
                    if (i == 0) {//first initial
                        pp++;
                        pc = true;
                        w.append(c);
                        continue;
                    }
                    if (pc && pp > 0) {//continue upper-case
                        w.append(c);
                        pp++;
                        continue;
                    }
                    addSnakeWord(b, w);
                    pp += pc ? 1 : 0;
                    pc = true;
                } else {
                    if (!w.isEmpty()) {
                        var lc = w.charAt(w.length() - 1);
                        if (isUpperCase(lc)) {
                            w.deleteCharAt(w.length() - 1);
                            addSnakeWord(b, w);
                            w.append(lc);
                        }
                    }
                    pp = 0;
                    pc = false;
                }
                w.append(c);
            } else {
                pp = 0;
                pc = false;
                addSnakeWord(b, w);
            }
        }
        //final word
        if (!w.isEmpty()) {
            if (!b.isEmpty()) b.append('_');
            b.append(w.toString().toLowerCase());
        }
        return b;

    }

    /**
     * loop to convert into Screaming Snake Case , word split by none-letter character or upper-case character.<br/>
     * <b>note:</b> Abbreviations are keep in UPPER-CASE as one word.
     *
     * @param src source
     * @return result
     */
  @ApiStatus.AvailableSince("0.1.3")  static CharSequence toScreaming(CharSequence src) {
        if (src == null || src.isEmpty()) return src;
        if (src.length() == 1) return String.valueOf(toUpperCase(src.charAt(0)));

        var b = new StringBuilder();
        var w = new StringBuilder();
        var pp = 0;
        var pc = false;
        for (var i = 0; i < src.length(); i++) {
            var c = src.charAt(i);
            if (isAlphabetic(c)) {
                if (isUpperCase(c)) {
                    if (i == 0) {//first initial
                        pp++;
                        pc = true;
                        w.append(c);
                        continue;
                    }
                    if (pc && pp > 0) {//continue upper-case
                        w.append(c);
                        pp++;
                        continue;
                    }
                    addScreamingWord(b, w);
                    pp += pc ? 1 : 0;
                    pc = true;
                } else {
                    if (!w.isEmpty()) {
                        var lc = w.charAt(w.length() - 1);
                        if (isUpperCase(lc)) {
                            w.deleteCharAt(w.length() - 1);
                            addScreamingWord(b, w);
                            w.append(lc);
                        }
                    }
                    pp = 0;
                    pc = false;
                }
                w.append(c);
            } else {
                pp = 0;
                pc = false;
                addScreamingWord(b, w);
            }
        }
        //final word
        if (!w.isEmpty()) {
            if (!b.isEmpty()) b.append('_');
            b.append(w.toString().toUpperCase());
        }
        return b;
    }

    /**
     * loop to convert into List of word , word split by none-letter character or upper-case character.<br/>
     * <b>note:</b> Abbreviations are keep as one word.
     *
     * @param src source
     * @return result
     */
   @ApiStatus.AvailableSince("0.1.3") static List<String> toWords(CharSequence src) {
        if (src == null || src.isEmpty()) return Collections.emptyList();
        if (src.length() == 1) return Collections.singletonList(String.valueOf(toUpperCase(src.charAt(0))));

        var b = new ArrayList<String>();
        var w = new StringBuilder();
        var pp = 0;
        var pc = false;
        for (var i = 0; i < src.length(); i++) {
            var c = src.charAt(i);
            if (isAlphabetic(c)) {
                if (isUpperCase(c)) {
                    if (i == 0) {//first initial
                        pp++;
                        pc = true;
                        w.append(c);
                        continue;
                    }
                    if (pc && pp > 0) {//continue upper-case
                        w.append(c);
                        pp++;
                        continue;
                    }
                    if (!w.isEmpty()) {
                        b.add(w.toString());
                        w.setLength(0);
                    }
                    pp += pc ? 1 : 0;
                    pc = true;
                } else {
                    if (!w.isEmpty()) {
                        var lc = w.charAt(w.length() - 1);
                        if (isUpperCase(lc)) {
                            w.deleteCharAt(w.length() - 1);
                            if (!w.isEmpty()) {
                                b.add(w.toString());
                                w.setLength(0);
                            }
                            w.append(lc);
                        }
                    }
                    pp = 0;
                    pc = false;
                }
                w.append(c);
            } else {
                pp = 0;
                pc = false;
                if (!w.isEmpty()) {
                    b.add(w.toString());
                    w.setLength(0);
                }
            }
        }
        //final word
        if (!w.isEmpty()) {
            b.add(w.toString());
        }
        return b;
    }

    @ApiStatus.Internal
    static void addCamelWord(StringBuilder b, StringBuilder w) {
        if (!w.isEmpty()) {
            if (b.isEmpty()) {
                if (fullUpperCase(w)) {
                    b.append(w.toString().toLowerCase());
                } else {
                    b.append(toLowerCase(w.charAt(0))).append(w.subSequence(1, w.length()).toString().toLowerCase());
                }
            } else {
                b.append(toUpperCase(w.charAt(0))).append(w.subSequence(1, w.length()).toString().toLowerCase());
            }
            w.setLength(0);
        }
    }

    @ApiStatus.Internal
    static void addSnakeWord(StringBuilder b, StringBuilder w) {
        if (!w.isEmpty()) {
            if (!b.isEmpty()) b.append('_');
            b.append(w.toString().toLowerCase());
            w.setLength(0);
        }
    }

    @ApiStatus.Internal
    static void addScreamingWord(StringBuilder b, StringBuilder w) {
        if (!w.isEmpty()) {
            if (!b.isEmpty()) b.append('_');
            b.append(w.toString().toUpperCase());
            w.setLength(0);
        }
    }

    /**
     * @param src source
     * @return true if contains all upper-case letters
     */
    static boolean fullUpperCase(CharSequence src) {
        if (src == null || src.isEmpty()) return false;
        if (src.length() == 1) return isUpperCase(src.charAt(0));
        for (int i = 0; i < src.length(); i++) {
            if (!isUpperCase(src.charAt(i))) return false;
        }
        return true;
    }
}
