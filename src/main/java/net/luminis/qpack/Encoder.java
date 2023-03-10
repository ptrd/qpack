package net.luminis.qpack;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Encoder {

    public static final Charset HTTP_HEADER_CHARSET = Charset.forName("US-ASCII");

    private final Huffman huffman;
    private final StaticTable staticTable;
    private final List<AbstractMap.Entry<String, String>> dynamicTable;

    public Encoder() {
        staticTable = new StaticTable();
        huffman = new Huffman();
        dynamicTable = new ArrayList<>();
    }

    /**
     * Compresses a set of headers into a QPack Header Block.
     * See https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5
     * @param headers
     * @return the created header block. Note that the underlying array will be larger than the number of bytes written in the buffer.
     * Use buffer.limit() to determine how many bytes to use.
     */
    public ByteBuffer compressHeaders(List<Map.Entry<String, String>> headers) {
        int estimatedSize = 10 + headers.stream().mapToInt(entry -> entry.getKey().length() + entry.getValue().length()).sum();
        ByteBuffer buffer = ByteBuffer.allocate(estimatedSize);

        insertHeaderBlockPrefix(buffer);

        headers.forEach(entry -> compressEntry(entry, buffer));

        buffer.limit(buffer.position());
        return buffer;
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.1
    private void insertHeaderBlockPrefix(ByteBuffer buffer) {
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
    }

    void compressEntry(Map.Entry<String, String> entry, ByteBuffer buffer) {
        int index = staticTable.findByNameAndValue(entry.getKey(), entry.getValue());
        if (index >= 0) {
            if (staticTable.lookupNameValue(index).getValue().equals(entry.getValue())) {
                insertIndexedHeaderField(index, buffer);
            }
            else {
                insertLiteralHeaderFieldWithNsmeReference(index, entry.getValue(), buffer);
            }
        }
        else {
            insertLiteralHeaderFieldWithoutNameReference(entry, buffer);
        }
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.2
    private void insertIndexedHeaderField(int index, ByteBuffer buffer) {
        insertPrefixedInteger(6, (byte) 0xc0, index, buffer);
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.4
    private void insertLiteralHeaderFieldWithNsmeReference(int index, String value, ByteBuffer buffer) {
        insertPrefixedInteger(4, (byte) 0x50, index, buffer);
        byte[] valueBytes = value.getBytes(HTTP_HEADER_CHARSET);
        insertPrefixedInteger(7, (byte) 0x00, valueBytes.length, buffer);
        buffer.put(valueBytes);
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.6
    private void insertLiteralHeaderFieldWithoutNameReference(Map.Entry<String, String> entry, ByteBuffer buffer) {
        byte[] keyBytes = entry.getKey().getBytes(HTTP_HEADER_CHARSET);
        insertPrefixedInteger(3,(byte) 0x20, keyBytes.length, buffer);
        buffer.put(keyBytes);
        byte[] valueBytes = entry.getValue().getBytes(HTTP_HEADER_CHARSET);
        insertPrefixedInteger(7, (byte) 0x00, valueBytes.length, buffer);
        buffer.put(valueBytes);
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.1.1
    // https://tools.ietf.org/html/rfc7541#section-5.1
    void insertPrefixedInteger(int prefixLength, byte prefix, int value, ByteBuffer buffer) {
        int maxPrefix = (int) (Math.pow(2, prefixLength) - 1);
        if (value < maxPrefix) {
            buffer.put((byte) (prefix | value));
        }
        else {
            buffer.put((byte) (prefix | maxPrefix));
            int remainder = value - maxPrefix;
            while (remainder > 128) {
                byte next = (byte) ((remainder % 128) | 0x80);
                buffer.put(next);
                remainder = remainder / 128;
            }
            buffer.put((byte) remainder);
        }
    }

}
