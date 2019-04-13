package net.luminis.qpack;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
