/*
 * Copyright © 2019, 2020, 2021, 2022, 2023, 2024, 2025 Peter Doornbosch
 *
 * This file is part of Flupke, a HTTP3 client Java library
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

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EncoderTest {

    private EncoderImpl encoder;

    @Before
    public void initEncoder() {
        encoder = new EncoderImpl();
    }

    @Test
    public void encodeIntegerWith5bitPrefix() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.1
        ByteBuffer buffer = ByteBuffer.allocate(8);
        encoder.insertPrefixedInteger(5, (byte) 0x60, 10, buffer);

        assertThat(buffer.array()).startsWith(0x6a);
        assertThat(buffer.position()).isEqualTo(1);
    }

    @Test
    public void encodePrefixedInteger() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.2
        ByteBuffer buffer = ByteBuffer.allocate(8);
        encoder.insertPrefixedInteger(5, (byte) 0, 1337, buffer);

        assertThat(buffer.array()).startsWith(0x1f, 0x9a, 0x0a);
        assertThat(buffer.position()).isEqualTo(3);
    }

    @Test
    public void encodeIntegerStartingAtOctetBoundary() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.3
        ByteBuffer buffer = ByteBuffer.allocate(8);
        encoder.insertPrefixedInteger(8, (byte) 0, 42, buffer);

        assertThat(buffer.array()).startsWith(0x2a);
        assertThat(buffer.position()).isEqualTo(1);
    }

    @Test
    public void compressPseudoHeaders() {
        List<Map.Entry<String, String>> headers = List.of(
                new AbstractMap.SimpleEntry<>(":method", "GET"),
                new AbstractMap.SimpleEntry<>(":scheme", "https"),
                new AbstractMap.SimpleEntry<>(":path", "/")
        );


        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                (byte) (0xc0 | 17),  // Indexed Header Field, static table, index 17
                (byte) (0xc0 | 23),  // Indexed Header Field, static table, index 23
                (byte) (0xc0 | 1)    // Indexed Header Field, static table, index 1
        };

        ByteBuffer result = encoder.compressHeaders(headers);
        assertThat(result.array()).startsWith(expected);
        assertThat(result.limit()).isEqualTo(expected.length);
    }

    @Test
    public void compressIndexedNameWithLiteralValue() {
        ByteBuffer result = encoder.compressHeaders(List.of(new AbstractMap.SimpleEntry<>(":method", "TRACE")));

        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                0x5f,  // 0101 1111  (first index of ":method" is 15)
                0x00,
                0x05,  // value length, no huffman
                0x54,  // T
                0x52,  // R
                0x41,  // A
                0x43,  // C
                0x45,  // E
        };
        assertThat(result.array()).startsWith(expected);
        assertThat(result.limit()).isEqualTo(expected.length);
    }

    @Test
    public void compressLiteral() {
        ByteBuffer result = encoder.compressHeaders(List.of(new AbstractMap.SimpleEntry<>("X-Custom-Header", "anyvalue")));
        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                0x27,  // 0010 0111
                0x08,  // Length = 15, 15 - 7 = 8
                0x58, 0x2d, 0x43, 0x75, 0x73, 0x74, 0x6f, 0x6d, 0x2d, 0x48, 0x65, 0x61, 0x64, 0x65, 0x72, // X-Custom-Header
                0x08, // Length = 8
                0x61, 0x6e, 0x79, 0x76, 0x61, 0x6c, 0x75, 0x65,  // anyvalue
        };
        assertThat(result.array()).startsWith(expected);
        assertThat(result.limit()).isEqualTo(expected.length);
    }
}