package net.luminis.qpack;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class Decoder {

    private StaticTable staticTable;

    public Decoder() {
        staticTable = new StaticTable();
    }

    public LinkedHashMap<String, String> decodeStream(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 16);
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();

        // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.1
        // "Header Block Prefix"
        long requiredInsertCount = parsePrefixedInteger(8, pushbackInputStream);
        int base = inputStream.read();

        int instruction = pushbackInputStream.read();
        pushbackInputStream.unread(instruction);
        while (instruction > 0) {
            Map.Entry<String, String> entry = null;
            if ((instruction & 0xc0) == 0x40) {
                entry = parseLiteralHeaderFieldWithNameReference(pushbackInputStream);
            }
            else {
                System.err.println("Error: unknown instruction " + instruction);
                break;
            }

            if (entry != null) {
                headers.put(entry.getKey(), entry.getValue());
            }
            instruction = pushbackInputStream.read();
            pushbackInputStream.unread(instruction);
        }

        return headers;
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.1.1
    // "The prefixed integer from Section 5.1 of [RFC7541] is used heavily
    //   throughout this document.  The format from [RFC7541] is used
    //   unmodified.  QPACK implementations MUST be able to decode integers up
    //   to 62 bits long."
    long parsePrefixedInteger(int prefixLength, InputStream input) throws IOException {
        int maxPrefix = (int) (Math.pow(2, prefixLength) - 1);
        int initialValue = input.read() & maxPrefix;
        if (initialValue < maxPrefix) {
            return initialValue;
        }

        long value = initialValue;
        int factor = 0;
        byte next;
        do {
            next = (byte) input.read();
            value += ((next & 0x7f) << factor);
            factor += 7;
        }
        while ((next & 0x80) == 0x80);

        return value;
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.4
    Map.Entry<String, String> parseLiteralHeaderFieldWithNameReference(PushbackInputStream inputStream) throws IOException {
        byte first = (byte) inputStream.read();
        inputStream.unread(first);
        boolean inStaticTable = (first & 0x10) == 0x10;
        int nameIndex = (int) parsePrefixedInteger(4, inputStream);
        String name = inStaticTable? staticTable.lookupName(nameIndex): "<tbd>";

        int firstValueByte = inputStream.read();
        inputStream.unread(firstValueByte);

        boolean huffmanEncoded = (firstValueByte & 0x80) == 0x80;
        int valueLength = (int) parsePrefixedInteger(7, inputStream);
        byte[] rawValue = new byte[valueLength];
        inputStream.read(rawValue);  // TODO: might read less when reading from a network stream....
        String value = huffmanEncoded? new Huffman().decode(rawValue): new String(rawValue);

        return new AbstractMap.SimpleEntry<>(name, value);
    }


}