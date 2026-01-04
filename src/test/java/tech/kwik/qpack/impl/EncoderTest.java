/*
 * Copyright © 2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026 Peter Doornbosch
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kwik.qpack.Encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class EncoderTest {

    private EncoderImpl encoder;
    private EncoderImpl encoderWithHuffman;

    @BeforeEach
    void initEncoder() {
        encoder = new EncoderImpl(false);
        encoderWithHuffman = new EncoderImpl(true);
    }

    //region prefixed integer encoding
    @Test
    void encodeIntegerWith5bitPrefix() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.1
        ByteBuffer buffer = ByteBuffer.allocate(8);
        encoder.insertPrefixedInteger(5, (byte) 0x60, 10, buffer);

        assertThat(buffer.array()).startsWith(0x6a);
        assertThat(buffer.position()).isEqualTo(1);
    }

    @Test
    void encodePrefixedInteger() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.2
        ByteBuffer buffer = ByteBuffer.allocate(8);
        encoder.insertPrefixedInteger(5, (byte) 0, 1337, buffer);

        assertThat(buffer.array()).startsWith(0x1f, 0x9a, 0x0a);
        assertThat(buffer.position()).isEqualTo(3);
    }

    @Test
    void encodeIntegerStartingAtOctetBoundary() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.3
        ByteBuffer buffer = ByteBuffer.allocate(8);
        encoder.insertPrefixedInteger(8, (byte) 0, 42, buffer);

        assertThat(buffer.array()).startsWith(0x2a);
        assertThat(buffer.position()).isEqualTo(1);
    }
    //endregion

    //region compress headers with static table, no huffman encoding
    @Test
    void compressPseudoHeaders() {
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
    void compressIndexedNameWithLiteralValue() {
        ByteBuffer result = encoder.compressHeaders(List.of(new AbstractMap.SimpleEntry<>(":method", "TRACE")));

        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                0x5f,  // 0101 1111  (first index of ":method" is 15)
                0x00,  // (2nd byte of index 15)
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
    void compressLiteral() {
        ByteBuffer result = encoder.compressHeaders(List.of(new AbstractMap.SimpleEntry<>("X-Custom-Header", "anyvalue")));
        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                0x27,  // 0010 0111 (https://www.rfc-editor.org/rfc/rfc9204.html#section-4.5.6, no huffman, name length 15)
                0x08,  // (2nd byte of length 15, 15 - 7 = 8)
                0x58, 0x2d, 0x43, 0x75, 0x73, 0x74, 0x6f, 0x6d, 0x2d, 0x48, 0x65, 0x61, 0x64, 0x65, 0x72, // "X-Custom-Header"
                0x08, // Length = 8
                0x61, 0x6e, 0x79, 0x76, 0x61, 0x6c, 0x75, 0x65,  // "anyvalue"
        };
        assertThat(result.array()).startsWith(expected);
        assertThat(result.limit()).isEqualTo(expected.length);
    }
    //endregion

    //region compress headers with static table, with huffman encoding
    @Test
    void compressIndexedNameWithLiteralValueWithHuffmanEncoding() throws IOException {
        ByteBuffer result = encoderWithHuffman.compressHeaders(List.of(new AbstractMap.SimpleEntry<>(":method", "TRACE")));

        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                0x5f,  // 0101 1111  (first index of ":method" is 15)
                0x00, // (2nd byte of index 15)
                (byte) 0x85,  // value length, huffman
                // TRACE = 1101111 1101101 100001 1011110 1100000
                //       = 11011111 10110110 00011011 11011000 00 111111
                (byte) 0b11011111, (byte) 0b10110110, 0b00011011, (byte) 0b11011000, 0b00111111
        };

        assertThat(result.array()).startsWith(expected);
        assertThat(result.limit()).isEqualTo(expected.length);
    }

    @Test
    void compressLiteralWithHuffman() {
        ByteBuffer result = encoderWithHuffman.compressHeaders(List.of(new AbstractMap.SimpleEntry<>("X-Custom-Header", "anyvalue")));
        byte[] expected = new byte[] {
                0x00,  // Required Insert Count
                0x00,  // Delta Base
                0x2f,  // 0010 1111 (https://www.rfc-editor.org/rfc/rfc9204.html#section-4.5.6, with huffman, name length 11)
                0x04,  // (2nd byte of length 11, 11 - 7 = 4)
                // X-Custom-Header = 11111100 010110 1011110 101101 01000 01001 00111 101001 010110 1100011 00101 00011 100100 00101 101100
                //                 = 11111100 01011010 11110101 10101000  01001001 11101001  01011011 00011001 01000111  00100001 01101100
                (byte) 0b11111100, 0b01011010, (byte) 0b11110101, (byte) 0b10101000, 0b01001001, (byte) 0b11101001, 0b01011011, 0b00011001, 0b01000111, 0b00100001, 0b01101100,
                (byte) 0x86, // Huffman, Length = 6
                // anyvalue = 00011 101010 1111010 1110111 00011 101000 101101 00101
                //          = 00011101 01011110 10111011 10001110  10001011 01001011
                0b00011101, 0b01011110, (byte) 0b10111011, (byte) 0b10001110, (byte) 0b10001011, 0b01001011
        };
        assertThat(result.array()).startsWith(expected);
        assertThat(result.limit()).isEqualTo(expected.length);
    }
    //endregion

    //region encoder settings
    @Test
    void huffmanEncodingCanBeEnabledOrDisabled() throws IOException {
        List<Map.Entry<String, String>> headers = List.of(new AbstractMap.SimpleEntry<>("X-Test-Header", "testvalue"));

        ByteBuffer resultWithoutHuffman = Encoder.newBuilder().useHuffmanEncoding(false).build().compressHeaders(headers);
        ByteBuffer resultWithHuffman = Encoder.newBuilder().useHuffmanEncoding(true).build().compressHeaders(headers);

        assertThat(resultWithoutHuffman.limit()).isGreaterThan(resultWithHuffman.limit());
    }
    //endregion

    //region edge cases
    @Test
    void encodingNumerousHeadersShouldNotCauseBufferOverflow() {
        List<Map.Entry<String, String>> headers = List.of(
                new AbstractMap.SimpleEntry<>("X-Header-1", "value1"),
                new AbstractMap.SimpleEntry<>("X-Header-2", "value2"),
                new AbstractMap.SimpleEntry<>("X-Header-3", "value3"),
                new AbstractMap.SimpleEntry<>("X-Header-4", "value4"),
                new AbstractMap.SimpleEntry<>("X-Header-5", "value5"),
                new AbstractMap.SimpleEntry<>("X-Header-6", "value6"),
                new AbstractMap.SimpleEntry<>("X-Header-7", "value7"),
                new AbstractMap.SimpleEntry<>("X-Header-8", "value8"),
                new AbstractMap.SimpleEntry<>("X-Header-9", "value9"),
                new AbstractMap.SimpleEntry<>("X-Header-10", "value10"),
                new AbstractMap.SimpleEntry<>("X-Header-11", "value11"),
                new AbstractMap.SimpleEntry<>("X-Header-12", "value12"),
                new AbstractMap.SimpleEntry<>("X-Header-13", "value13"),
                new AbstractMap.SimpleEntry<>("X-Header-14", "value14"),
                new AbstractMap.SimpleEntry<>("X-Header-15", "value15"),
                new AbstractMap.SimpleEntry<>("X-Header-16", "value16"),
                new AbstractMap.SimpleEntry<>("X-Header-17", "value17")
        );

        ByteBuffer result = encoder.compressHeaders(headers);
        assertThat(result.limit()).isLessThan(result.capacity());
    }

    @Test
    void encodeVeryLongHeaderAndValue() {
        String longHeader = "X-Header-";
        for (int i = 0; i < 66000; i++) {
            longHeader += "x";
        }
        String longValue = "value1-";
        for (int i = 0; i < 66000; i++) {
            longValue += "x";
        }
        List<Map.Entry<String, String>> headers = List.of(new AbstractMap.SimpleEntry<>(longHeader, longValue));

        ByteBuffer result = encoder.compressHeaders(headers);
        assertThat(result.limit()).isGreaterThan(0);
    }
    //endregion
}