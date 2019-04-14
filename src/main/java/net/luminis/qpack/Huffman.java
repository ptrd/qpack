package net.luminis.qpack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;


public class Huffman {

    private static Map<Integer, MappedSymbol> lookupTable = null;


    public Huffman() {
        if (lookupTable == null) {
            lookupTable = new HashMap<>();
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

            createLookupTable(codeTable, 8);
        }
    }

    public String decode(byte[] bytes) {
        StringBuffer string = new StringBuffer(bytes.length);
        BitBuffer buffer = new BitBuffer(bytes);
        while (buffer.hasRemaining()) {
            int key = (int) buffer.peek() & 0xff;
            MappedSymbol mappedSymbol = lookupTable.get(key);
            if (mappedSymbol != null) {
                if (mappedSymbol.isPresent()) {
                    string.append(mappedSymbol.character);
                    buffer.shift(mappedSymbol.codeLength);
                }
                else {
                    buffer.shift(8);
                    int subKey = (int) buffer.peek() & 0xff;
                    mappedSymbol = mappedSymbol.subTable.get(subKey);
                    if (mappedSymbol != null) {
                        string.append(mappedSymbol.character);
                        buffer.shift(mappedSymbol.codeLength);
                    }
                    else {
                        break;
                    }
                }
            }
            else  {
                break;
            }

        }
        return string.toString();
    }

    private void createLookupTable(Map<String, Integer> codeTable, int n) {
        codeTable.entrySet().forEach(entry -> {
            String code = entry.getKey();
            if (code.length() <= n) {
                int codeValue = parseBits(code, code.length());
                int codeSizeInBytes = ((n-1) / 8) + 1;
                generateExtendedCodes(codeValue, code.length(), codeSizeInBytes)
                        .forEach(c -> lookupTable.put(c, new MappedSymbol(entry.getValue(), code.length())));
            }
            else if (code.length() <= 2*n) {
                int primaryCode = parseBits(code, n);
                int secondaryCode = parseBits(code.substring(n), code.length()-n);
                MappedSymbol mapping;
                if (lookupTable.containsKey(primaryCode)) {
                    mapping = lookupTable.get(primaryCode);
                }
                else {
                    mapping = new MappedSymbol();
                }
                lookupTable.put(primaryCode, mapping);
                int codeSizeInBytes = ((n-1) / 8) + 1;
                generateExtendedCodes(secondaryCode, code.length()-n, codeSizeInBytes)
                        .forEach(c -> mapping.subTable.put(c, new MappedSymbol(entry.getValue(), code.length()-n)));
            }
        });
    }

    private IntStream generateExtendedCodes(int codeValue, int bits, int codeSizeInBytes) {
        int baseValue = codeValue << (codeSizeInBytes * 8 - bits);
        int maxAddition = (int) Math.pow(2, codeSizeInBytes * 8 - bits);
        return IntStream.range(0, maxAddition).map(addition -> baseValue | addition);
    }

    private int parseBits(String code, int bits) {
        return Integer.parseInt(code.substring(0, bits), 2);
    }

    private String extractBitPattern(String line) {
        int firstSpace = line.indexOf(" ");
        return line.substring(0, firstSpace).replaceAll("\\|", "");
    }

    private static class MappedSymbol {

        final char character;
        final int codeLength;
        final Map<Integer, MappedSymbol> subTable;

        public MappedSymbol(int character, int codeLength) {
            this.character = (char) character;
            this.codeLength = codeLength;
            this.subTable = null;
        }

        public MappedSymbol() {
            this.character = 0;
            this.codeLength = 0;
            subTable = new HashMap<>();
        }

        boolean isPresent() {
            return subTable == null;
        }
    }

}
