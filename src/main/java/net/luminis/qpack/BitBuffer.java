package net.luminis.qpack;

import java.nio.ByteBuffer;

public class BitBuffer {

    private final ByteBuffer data;

    private int head;
    private int bitsInHead;

    public BitBuffer(byte[] data) {
        this.data = ByteBuffer.wrap(data);
        for (int i = 0; i < 4; i++) {
            if (i < data.length) {
                head = (head << 8) | (this.data.get() & 0xff);
                bitsInHead += 8;
            }
            else {
                head = head << 8;
            }
        }
    }

    byte peek() {
        return (byte) ((head & 0xff000000) >> 24);
    }

    public void shift(int size) {
        head = head << size;
        int ones = (int) Math.pow(2, size) - 1;
        head = head | ones;

        if (size <= bitsInHead) {
            bitsInHead -= size;
        }
        else {
            bitsInHead = 0;
        }
    }

    public int remaining() {
        return bitsInHead;
    }
}
