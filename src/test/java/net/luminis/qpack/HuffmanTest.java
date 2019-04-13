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

}
