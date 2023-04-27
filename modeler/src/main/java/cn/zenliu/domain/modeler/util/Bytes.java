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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Simple Byte Buffer
 *
 * @author Zen.Liu
 * @since 2023-04-27
 */
public final class Bytes {
    private byte[] buf;
    private int index;
    private final int expand;

    /**
     * @param buf    initial byte array
     * @param index  current empty slot index
     * @param expand size for time each expand
     */
    Bytes(byte[] buf, int index, int expand) {
        this.buf = buf;
        this.index = index;
        this.expand = expand;
    }

    public static Bytes read(byte[] buf) {
        return new Bytes(buf, 0, 0);
    }

    public static Bytes write(byte[] buf, int offset, int expand) {
        return new Bytes(buf, offset, expand);
    }

    void ensure(int size) {
        var e = buf.length - index - size;
        if (e < 0) {
            if (expand > 0)
                this.buf = Arrays.copyOf(buf, buf.length + expand);
            else throw new IllegalStateException("data broken");
        }
    }


    public Bytes put(boolean val) {
        ensure(1);
        buf[index++] = (byte) (val ? 1 : 0);
        return this;
    }

    public static int zigzag(int i) {
        return (i << 1) ^ (i << 31);
    }

    public static int countVarint(int val) {
        var n = 0;
        do {
            n++;
            val >>>= 7L;
        } while (val != 0);
        return n;
    }

    public Bytes put(int val) {
        //val = zigzag(val);
        ensure(countVarint(val));
        do {
            var bits = val & 0x7F;
            val >>>= 7;
            buf[index++] = (byte) (bits + ((val != 0) ? 0x80 : 0));
        } while (val > 0);
        return this;
    }

    public static long zigzag(long i) {
        return (i << 1L) ^ (i << 63L);
    }

    public static int countVarint(long val) {
        var n = 0;
        do {
            n++;
            val >>>= 7L;
        } while (val != 0);
        return n;
    }

    public Bytes put(long val) {
        //val = zigzag(val);
        ensure(countVarint(val));
        do {
            var bits = val & 0x7FL;
            val >>>= 7L;
            buf[index++] = (byte) (bits + ((val != 0) ? 0x80L : 0L));
        } while (val > 0);
        return this;
    }

    public Bytes put(float val) {
        return put(Float.floatToIntBits(val));
    }

    public Bytes put(double val) {
        return put(Double.doubleToLongBits(val));
    }

    public Bytes put(byte[] val) {
        if (val == null) {
            put(0);
            return this;
        }
        var n = val.length;
        if (n == 0) {
            put(0);
            return this;
        }
        put(n);
        ensure(n);
        System.arraycopy(val, 0, buf, index, val.length);
        index += n;
        return this;
    }

    public Bytes put(String val) {
        if (val == null || val.isEmpty()) return put(0);
        var bs = val.getBytes(StandardCharsets.UTF_8);
        return put(bs);
    }

    public boolean readBoolean() {
        ensure(1);
        return buf[index++] == 1;
    }

    public static int unzigzag(int i) {
        return (i >>> 1) ^ -(i & 1);
    }

    public int readInt() {
        var i = 0;
        var s = 0;
        int b;
        do {
            if (s >= 32) throw new IllegalStateException("data broken");
            i |= (b = buf[index++]) & 0x7F << s;
            s += 7;
        } while ((b & 0x80) != 0);
        return i;
        // return unzigzag(i);
    }

    public static long unzigzag(long i) {
        return (i >>> 1) ^ -(i & 1);
    }

    public long readLong() {
        var i = 0L;
        var s = 0;
        int b;
        do {
            if (s >= 64) throw new IllegalStateException("data broken");
            i |= ((long) (b = buf[index++]) & 0x7F) << s;
            s += 7;
        } while ((b & 0x80) != 0);
        return i;
        // return unzigzag(i);
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public byte[] readBytes() {
        var n = readInt();
        if (n == 0) return new byte[0];
        var b = new byte[n];
        System.arraycopy(buf, index, b, 0, n);
        index += n;
        return b;
    }

    public String readString() {
        var bs = readBytes();
        if (bs.length == 0) return "";
        return new String(bs, StandardCharsets.UTF_8);
    }

    public byte[] buf() {
        return buf;
    }

    /**
     * last empty slot or next readable slot
     */
    public int index() {
        return index;
    }

    public int expand() {
        return expand;
    }


}




