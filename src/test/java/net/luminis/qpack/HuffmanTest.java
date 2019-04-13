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

}
