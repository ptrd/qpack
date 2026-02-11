/*
 * Copyright © 2026 Peter Doornbosch
 *
 * This file is part of Flupke, a HTTP3 Java library.
 *
 * Flupke is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Flupke is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tech.kwik.qpack.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kwik.qpack.impl.PrefixedInteger.insertPrefixedInteger;
import static tech.kwik.qpack.impl.PrefixedInteger.parsePrefixedInteger;

class PrefixedIntegerTest {

    @Test
    void encodeIntegerWith5bitPrefix() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.1
        ByteBuffer buffer = ByteBuffer.allocate(8);
        insertPrefixedInteger(5, (byte) 0x60, 10, buffer);

        assertThat(buffer.array()).startsWith(0x6a);
        assertThat(buffer.position()).isEqualTo(1);
    }

    @Test
    void encodePrefixedInteger() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.2
        ByteBuffer buffer = ByteBuffer.allocate(8);
        insertPrefixedInteger(5, (byte) 0, 1337, buffer);

        assertThat(buffer.array()).startsWith(0x1f, 0x9a, 0x0a);
        assertThat(buffer.position()).isEqualTo(3);
    }

    @Test
    void encodeIntegerStartingAtOctetBoundary() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.3
        ByteBuffer buffer = ByteBuffer.allocate(8);
        insertPrefixedInteger(8, (byte) 0, 42, buffer);

        assertThat(buffer.array()).startsWith(0x2a);
        assertThat(buffer.position()).isEqualTo(1);
    }

    @Test
    public void parseIntegerWith5bitPrefix() throws IOException {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.1
        long value = parsePrefixedInteger(5, wrap((byte) 0x0a));

        assertThat(value).isEqualTo(10);
    }

    @Test
    public void parseThreeBytePrefixedInteger() throws IOException {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.2
        long value = parsePrefixedInteger(5, wrap((byte) 0xff, (byte) 0x9a, (byte) 0x0a));

        assertThat(value).isEqualTo(1337);
    }

    @Test
    public void parseIntegerStartingAtOctetBoundary() throws IOException {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.3
        long value = parsePrefixedInteger(8, wrap((byte) 42));

        assertThat(value).isEqualTo(42);
    }

    private PushbackInputStream wrap(byte... bytes) {
        return new PushbackInputStream(new ByteArrayInputStream(bytes));
    }
}
