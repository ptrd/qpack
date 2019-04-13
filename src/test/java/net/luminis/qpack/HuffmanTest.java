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
}
