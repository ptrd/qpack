package net.luminis.qpack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Decoder for Huffman code as specified by https://datatracker.ietf.org/doc/html/rfc7541#appendix-B.
 * The decoding is implemented by nested lookup tables, where each lookup key is 8 bits. As the given Huffman code
 * has a maximum code length of 30 bits, the maximum nesting is 4 levels.
 * For example, the code for '\n' (decimal 10) is |11111111|11111111|11111111|111100, this requires four lookups: the
 * 1st, 2nd and 3rd point to the table that contains an entry for 0b111100xx.
 * As most codes are not an exact multiple of 8, the lookup must take into account the "don't care" bits. To simplify
 * the lookup, all values for the don't cares are included in the table.
 * For example, the code for '%' is 0b010101 (6 bits), and the table contains 4 entries (0b01010100, 0b01010101,
 * 0b01010110, 0b01010111), all pointing to the same table entry for '%'.
 */
public class Huffman {

    private static TableEntry[] lookupTable = null;
    private static final int KEY_SIZE = 8;
    public static final int TABLE_SIZE = (int) Math.pow(2, KEY_SIZE);

    public Huffman() {
        if (lookupTable == null) {
            lookupTable = new TableEntry[TABLE_SIZE];
            Map<String, Integer> codeTable = new HashMap<>();
            try {
                InputStream resourceAsStream = this.getClass().getResourceAsStream("huffmancode.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

                String line;
                int index = 0;
                line = reader.readLine();
                while (line != null) {
                    codeTable.put(extractBitPattern(line), index);
                    index++;
                    line = reader.readLine();
                }
            } catch (IOException e) {
                // Impossible when library is build correctly.
                throw new RuntimeException("Corrupt library, missing internal resource.");
            }

            codeTable.entrySet().forEach(entry -> {
                addToLookupTable(lookupTable, entry.getKey(), entry.getValue());
            });
        }
    }

    /**
     * Decodes a string of Huffman encoded bytes.
     * @param bytes
     * @return
     */
    public String decode(byte[] bytes) {
        StringBuffer string = new StringBuffer(bytes.length);
        BitBuffer buffer = new BitBuffer(bytes);
        while (buffer.hasRemaining()) {
            TableEntry symbol = lookup(lookupTable, buffer);
            if (symbol != null) {
                string.append(symbol.character);
            }
        }
        return string.toString();
    }

    /**
     * Performs (recursive) symbol lookup for the first character in the buffer with the given table.
     * @param table   the lookup table used for the lookup (is an argument to allow for recursion)
     * @param buffer  the buffer containing the bits that will be decoded.
     * @return  the symbol represented by the code or null if there is no match
     */
    private TableEntry lookup(TableEntry[] table, BitBuffer buffer) {
        int key = (int) buffer.peek() & 0xff;
        TableEntry mappedSymbol = table[key];
        if (mappedSymbol.isSymbol()) {
            buffer.shift(mappedSymbol.codeLength);
            return mappedSymbol;
        }
        else if (buffer.remaining() >= KEY_SIZE) {
            if (mappedSymbol.subTable == null) {
                throw new IllegalStateException("Missing subtable!");
            }
            buffer.shift(KEY_SIZE);
            return lookup(mappedSymbol.subTable, buffer);
        }
        else {
            // End of buffer contains some non-character bits (probably just 1's), as total length of character encodings
            // in the buffer is not a multiple of 8.
            buffer.shift(buffer.remaining());
            return null;
        }
    }

    /**
     * Adds a symbol with the given code to the lookup table recursively. If the code length is larger than 8, the
     * symbol will not be added to the table directly, but indirectly via one or more linked (nested) tables.
     * @param table  the table to add the symbol to
     * @param code   the code to add as a String of 1's and 0's
     * @param symbolValue  the symbol symbolValue to add (integer representation)
     */
    private void addToLookupTable(TableEntry[] table, String code, int symbolValue) {
        if (code.length() <= KEY_SIZE) {
            int codeValue = parseBits(code, code.length());
            TableEntry mappedSymbol = new TableEntry(symbolValue, code.length());
            generateCodeKeys(codeValue, code.length())
                    .forEach(key -> table[key] = mappedSymbol);
        }
        else {
            int prefixCode = parseBits(code, KEY_SIZE);
            String suffix = code.substring(KEY_SIZE);
            if (table[prefixCode] == null) {
                table[prefixCode] = new TableEntry();
            }
            addToLookupTable(table[prefixCode].subTable, suffix, symbolValue);
        }
    }

    /**
     * Generates keys for the given code. As a code can be less than 8 bits, keys must be generated for all values for
     * the LSB's that are not part of the key. For example, if the code is 0b001100 (6 bits), keys are generated for all
     * values of the 2 least significant bits: 0b00110000, 0b00110001, 0b00110010, 0b00110011
     * @param codeValue
     * @param bits
     * @return
     */
    private IntStream generateCodeKeys(int codeValue, int bits) {
        int baseValue = codeValue << (KEY_SIZE - bits);
        int maxAddition = (int) Math.pow(2, KEY_SIZE - bits);
        return IntStream.range(0, maxAddition).map(addition -> baseValue | addition);
    }

    private int parseBits(String code, int count) {
        return Integer.parseInt(code.substring(0, count), 2);
    }

    private String extractBitPattern(String line) {
        int firstSpace = line.indexOf(" ");
        return line.substring(0, firstSpace).replaceAll("\\|", "");
    }

    private static class TableEntry {

        final char character;
        final int codeLength;
        final TableEntry[] subTable;

        public TableEntry(int character, int codeLength) {
            this.character = (char) character;
            this.codeLength = codeLength;
            this.subTable = null;
        }

        public TableEntry() {
            this.character = 0;
            this.codeLength = 0;
            subTable = new TableEntry[(int) Math.pow(2, KEY_SIZE)];;
        }

        boolean isSymbol() {
            return subTable == null;
        }
    }
}
