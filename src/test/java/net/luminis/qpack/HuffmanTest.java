package net.luminis.qpack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class HuffmanTest {

    private Huffman huffman = new Huffman();

    @Test
    public void decodeSingleByte() {
        String decoded = huffman.decode(new byte[] { (byte) 0xfc });

        assertThat(decoded).isEqualTo("X");
    }

    @Test
    public void decode302() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.6.1
        String decoded = huffman.decode(new byte[] { 0x64, 0x02 });

        assertThat(decoded).isEqualTo("302");
    }

    @Test
    public void decodeFourBytes() {
        // 12345678123456781234567812345678
        // P      j      t    r     .     eos
        // 11010111110100010011011000101111
        String decoded = huffman.decode(new byte[] { (byte) 0b11010111, (byte) 0b11010001, 0b00110110, 0b00101111 });

        assertThat(decoded).isEqualTo("Pjtr.");
    }

    @Test
    public void decodeFiveBytes() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.6.1
        String decoded = huffman.decode(new byte[] { (byte) 0xae, (byte) 0xc3, 0x77, 0x1a, 0x4b });

        assertThat(decoded).isEqualTo("private");
    }

    @Test
    public void decodeExampleUri() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.6.1
        String decoded = huffman.decode(
                new byte[] { (byte) 0x9d, 0x29, (byte) 0xad, 0x17, 0x18, 0x63, (byte) 0xc7, (byte) 0x8f,
                        0x0b, (byte) 0x97, (byte) 0xc8, (byte) 0xe9, (byte) 0xae, (byte) 0x82, (byte) 0xae, 0x43, (byte) 0xd3 });

        assertThat(decoded).isEqualTo("https://www.example.com");
    }

    @Test
    public void decodeDateTime() {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.6.1
        String decoded = huffman.decode(
                new byte[] { (byte) 0xd0, 0x7a, (byte) 0xbe, (byte) 0x94, 0x10, 0x54, (byte) 0xd4, 0x44,
                        (byte) 0xa8, 0x20, 0x05, (byte) 0x95, 0x04, 0x0b, (byte) 0x81, 0x66,
                        (byte) 0xe0, (byte) 0x82, (byte) 0xa6, 0x2d, 0x1b, (byte) 0xff });

        assertThat(decoded).isEqualTo("Mon, 21 Oct 2013 20:13:21 GMT");
    }

    @Test
    public void decodeShortStringWithTwoByteHuffmanCodes() {
        // 123456781234567812345678123456781234567812345678
        // #           h     a    s    h     !
        // 11111111101010011100011010001001111111111000
        String decoded = huffman.decode(new byte[] { (byte) 0b11111111, (byte) 0b10101001, (byte) 0b11000110, (byte) 0b10001001, (byte) 0b11111111, (byte) 0b10001111 });

        assertThat(decoded).isEqualTo("#hash!");
    }

    @Test
    public void decodeSymbolsWithTwoByteHuffmanCodes() {
        // 1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16
        // 12345678123456781234567812345678123456781234567812345678123456781234567812345678123456781234567812345678123456781234567812345678
        // (         {              [            ^             #           $            +          <              ?         @            !
        // 11111110101111111111111101111111111011111111111111001111111110101111111111001111111110111111111111111001111111100111111111101011
        //
        // 17      18      19      20      21      22      23      24      25      26      27      28      29      30
        // 1234567812345678123456781234567812345678123456781234567812345678123456781234567812345678123456781234567812345678
        //         >           "         '          `              ~            ]            }             )
        // 1111100011111111101111111110011111111101011111111111110111111111111011111111111100111111111111011111111011
        String decoded = huffman.decode(new byte[] {
                (byte) 0b11111110, (byte) 0b10111111, (byte) 0b11111111, (byte) 0b01111111,
                (byte) 0b11101111, (byte) 0b11111111, (byte) 0b11001111, (byte) 0b11111010,
                (byte) 0b11111111, (byte) 0b11001111, (byte) 0b11111011, (byte) 0b11111111,
                (byte) 0b11111001, (byte) 0b11111110, (byte) 0b01111111, (byte) 0b11101011,
                (byte) 0b11111000, (byte) 0b11111111, (byte) 0b10111111, (byte) 0b11100111,
                (byte) 0b11111101, (byte) 0b01111111, (byte) 0b11111101, (byte) 0b11111111,
                (byte) 0b11101111, (byte) 0b11111111, (byte) 0b00111111, (byte) 0b11111101,
                (byte) 0b11111110, (byte) 0b11111111
        });

        assertThat(decoded).isEqualTo("({[^#$+<?@!>\"'`~]})");
    }

    @Test
    public void decodeThreeByteHuffmanCode() {
        // '\' -> 11111111|11111110|000
        String decoded = huffman.decode(new byte[] { (byte) 0b11111111, (byte) 0b11111110, (byte) 0b00011111 });
        assertThat(decoded).isEqualTo("\\");
    }

    @Test
    public void decodeFourByteHuffmanCode() {
        // 10 -> |11111111|11111111|11111111|111100
        String decoded = huffman.decode(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11110011 });
        assertThat(decoded).isEqualTo("\n");
    }

    @Test
    public void decodeThreeAndFourByteHoffmanCodes() {
        // 246, 224, 231, 254 |11111111|11111111|11111101|001, |11111111|11111110|1100, |11111111|11111111|1110011, |11111111|11111111|11111110|000
        String decoded = huffman.decode(new byte[]{ (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111101, (byte) 0b00111111, (byte) 0b11111111,
                (byte) 0b11011001, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11001111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111000, (byte) 0b01111111 });

        assertThat(decoded.charAt(0)).isEqualTo((char) 246);
        assertThat(decoded.charAt(1)).isEqualTo((char) 224);
        assertThat(decoded.charAt(2)).isEqualTo((char) 231);
        assertThat(decoded.charAt(3)).isEqualTo((char) 254);
    }

    /**
     * QPACK is character encoding agnostic: it operates on opaque octets.
     * For convenience, the decode method returns String (because Decoder provides Strings).
     * This test shows that characters outside the ASCII range are left unchanged.
     * However, to get the unchanged bytes out of the string, getBytes(StandardCharsets.ISO_8859_1) must be used.
     */
    @Test
    public void testUnicodeTearsOfJoySurvivesDecoding() {
        // 240 159 152 130   UTF-8 encoded tears-of-joy emoji
        String decoded = huffman.decode(
                new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111010, (byte) 0b11_111111,
                        (byte) 0b11111111, (byte) 0b11111011, (byte) 0b11_111111, (byte) 0b11111111,
                        (byte) 0b11110010, (byte) 0b0_1111111, (byte) 0b11111111, (byte) 0b00111_111 });

        assertThat(decoded.charAt(0)).isEqualTo((char) 240);
        assertThat(decoded.charAt(1)).isEqualTo((char) 159);
        assertThat(decoded.charAt(2)).isEqualTo((char) 152);
        assertThat(decoded.charAt(3)).isEqualTo((char) 130);

        byte[] tearsOfJoyUtf8 = new byte[] { (byte) 0xf0, (byte) 0x9f, (byte) 0x98, (byte) 0x82 };
        assertThat(decoded.getBytes(StandardCharsets.ISO_8859_1)).isEqualTo(tearsOfJoyUtf8);

        // As we know the original tears of joy was (character) encoded with UTF-8
        String reinterpreted = new String(decoded.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        // UTF-16 encoding of tears-of-joy is D83D DE02
        assertThat(reinterpreted.length()).isEqualTo(2);
        assertThat(reinterpreted.charAt(0)).isEqualTo((char) 0xd83d);
        assertThat(reinterpreted.charAt(1)).isEqualTo((char) 0xde02);
    }

    /**
     * Proofs that String.getBytes(StandardCharsets.ISO_8859_1) returns the raw unchanged character values.
     */
    @Test
    public void testISO8859_1DecodingReturnsRawCharValue() {
        for (int i = 0; i < 256; i++) {
            char c = (char) i;
            String s = new StringBuffer().append(c).toString();
            byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
            assertThat(bytes.length).isEqualTo(1);
            assertThat(Byte.toUnsignedInt(bytes[0])).isEqualTo(i);
        }
    }
}
