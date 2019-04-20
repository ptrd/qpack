package net.luminis.qpack;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.*;


public class Decoder {

    private final Huffman huffman;
    private final StaticTable staticTable;
    private final List<AbstractMap.Entry<String, String>> dynamicTable;

    public Decoder() {
        staticTable = new StaticTable();
        huffman = new Huffman();
        dynamicTable = new ArrayList<>();
    }

    public void decodeEncoderStream(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 16);
        int instruction = pushbackInputStream.read();
        pushbackInputStream.unread(instruction);

        while (instruction > 0) {  // EOF returns -1

            if ((instruction & 0x80) == 0x80) {
                parseInsertWithNameReference(pushbackInputStream);
            } else {
                System.err.println("Error: unknown instruction " + instruction);
            }

            instruction = pushbackInputStream.read();
            pushbackInputStream.unread(instruction);
        }
    }

    public List<Map.Entry<String, String>> decodeStream(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 16);
        List<Map.Entry<String, String>> headers = new ArrayList<>();

        // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.1
        // "Header Block Prefix"
        long requiredInsertCount = parsePrefixedInteger(8, pushbackInputStream);
        int deltaBase = (int) parsePrefixedInteger(7, pushbackInputStream);

        int instruction = pushbackInputStream.read();
        pushbackInputStream.unread(instruction);
        while (instruction > 0) {  // EOF returns -1
            Map.Entry<String, String> entry = null;
            if ((instruction & 0x80) == 0x80) {
                entry = parseIndexedHeaderField(pushbackInputStream);
            }
            else if ((instruction & 0xc0) == 0x40) {
                entry = parseLiteralHeaderFieldWithNameReference(pushbackInputStream);
            }
            else if ((instruction & 0xe0) == 0x20) {
                entry = parseLiteralHeaderFieldWithoutNameReference(pushbackInputStream);
            }
            else {
                System.err.println("Error: unknown instruction " + instruction);
                break;
            }

            if (entry != null) {
                headers.add(entry);
            }
            instruction = pushbackInputStream.read();
            pushbackInputStream.unread(instruction);
        }

        return headers;
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.3.1
    void parseInsertWithNameReference(PushbackInputStream inputStream) throws IOException {
        int first = inputStream.read();
        inputStream.unread(first);

        int index = (int) parsePrefixedInteger(6, inputStream);
        boolean referStatic = (first & 0x40) == 0x40;
        String name = referStatic? staticTable.lookupName(index): lookupDynamicTable(index).getKey();

        String value = parseStringValue(inputStream);
        addToTable(name, value);
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

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.2
    Map.Entry<String, String> parseIndexedHeaderField(PushbackInputStream inputStream) throws IOException {
        byte first = (byte) inputStream.read();  // TODO: might return -1 becauseof end-of-stream
        inputStream.unread(first);
        boolean inStaticTable = (first & 0x40) == 0x40;
        int index = (int) parsePrefixedInteger(6, inputStream);

        if (inStaticTable) {
            return staticTable.lookupNameValue(index);
        }
        else {
            return lookupDynamicTable(index);
        }
    }


    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.4
    Map.Entry<String, String> parseLiteralHeaderFieldWithNameReference(PushbackInputStream inputStream) throws IOException {
        byte first = (byte) inputStream.read();  // TODO: might return -1 becauseof end-of-stream
        inputStream.unread(first);
        boolean inStaticTable = (first & 0x10) == 0x10;
        int nameIndex = (int) parsePrefixedInteger(4, inputStream);
        String name = inStaticTable? staticTable.lookupName(nameIndex): "<tbd>";

        String value = parseStringValue(inputStream);

        return new AbstractMap.SimpleEntry<>(name, value);
    }

    // https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-4.5.6
    Map.Entry<String, String> parseLiteralHeaderFieldWithoutNameReference(PushbackInputStream inputStream) throws IOException {
        String name = parseStringValue(3, inputStream);
        String value = parseStringValue(inputStream);
        return new AbstractMap.SimpleEntry<>(name, value);
    }

    Map.Entry<String, String> lookupDynamicTable(int index) {
        if (index < dynamicTable.size()) {
            return dynamicTable.get(index);
        }
        else {
            return null;
        }
    }

    private String parseStringValue(PushbackInputStream inputStream) throws IOException {
        int firstByte = inputStream.read();
        inputStream.unread(firstByte);
        boolean huffmanEncoded = (firstByte & 0x80) == 0x80;
        int valueLength = (int) parsePrefixedInteger(7, inputStream);
        byte[] rawValue = new byte[valueLength];
        inputStream.read(rawValue);  // TODO: might read less when reading from a network stream....
        return huffmanEncoded? huffman.decode(rawValue): new String(rawValue);
    }

    private String parseStringValue(int prefixLength, PushbackInputStream inputStream) throws IOException {
        int huffmanFlagMask;
        switch(prefixLength) {
            case 3:
                huffmanFlagMask = 0x08;
                break;
            default:
                throw new RuntimeException("tbd");
        }

        int firstByte = inputStream.read();
        inputStream.unread(firstByte);
        boolean huffmanEncoded = (firstByte & huffmanFlagMask) == huffmanFlagMask;
        int length = (int) parsePrefixedInteger(prefixLength, inputStream);
        byte[] rawBytes = new byte[length];
        inputStream.read(rawBytes);  // TODO: might read less when reading from a network stream....
        return huffmanEncoded? huffman.decode(rawBytes): new String(rawBytes);
    }

    private void addToTable(String name, String value) {
        dynamicTable.add(new AbstractMap.SimpleEntry<>(name, value));
    }

}
