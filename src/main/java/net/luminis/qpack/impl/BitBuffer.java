package net.luminis.qpack.impl;

import java.nio.ByteBuffer;

public class BitBuffer {

    private final ByteBuffer data;

    private int head;
    private int bitsInHead;
    private int unsetInHead;

    public BitBuffer(byte[] data) {
        this.data = ByteBuffer.wrap(data);
        for (int i = 0; i < 4; i++) {
            if (i < data.length) {
                head = (head << 8) | (this.data.get() & 0x000000ff);
                bitsInHead += 8;
            }
            else {
                head = (head << 8) | 0x000000ff;
            }
        }
    }

    byte peek() {
        return (byte) ((head & 0xff000000) >> 24);
    }

    public void shift(int size) {
        if (unsetInHead + size >= 8) {
            // Shift that much so exactly 8 bits are not set
            head = head << (8 - unsetInHead);
            // Move in next byte (if any, otherwise move in EOS)
            if (data.remaining() > 0) {
                head = head | (data.get() & 0x000000ff);
                bitsInHead += 8;
            }
            else {
                head = head | 0x000000ff;
            }
            // Shift the rest
            int remaining = size - (8 - unsetInHead);
            head = head << remaining;
            bitsInHead -= size;
            unsetInHead = remaining;
        }
        else {
            head = head << size;
            bitsInHead -= size;
            unsetInHead += size;
        }
    }

    public boolean hasRemaining() {
        return bitsInHead > 0;
    }

    public int remaining() {
        return bitsInHead > 0? bitsInHead: 0;
    }
}
