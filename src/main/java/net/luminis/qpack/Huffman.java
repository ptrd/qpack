package net.luminis.qpack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;


public class Huffman {

    private Map<String, Integer> codeTable = new HashMap<>();
    private Map<Integer, MappedSymbol> table = new HashMap<>();


    public Huffman() {
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
            e.printStackTrace();
        }

        createLookupTable(8);
    }

    public String decode(byte[] bytes) {
        StringBuffer string = new StringBuffer(bytes.length);
        BitBuffer buffer = new BitBuffer(bytes);
        while (buffer.remaining() > 0) {
            int key = (int) buffer.peek() & 0xff;
            MappedSymbol mappedSymbol = table.get(key);
            if (mappedSymbol != null){
                string.append(mappedSymbol.character);
                buffer.shift(mappedSymbol.codeLength);
            }
            else  {
                break;
            }

        }
        return string.toString();
    }

    private void createLookupTable(int n) {
        codeTable.entrySet().forEach(entry -> {
            String code = entry.getKey();
            if (code.length() <= n) {
                int codeValue = parseBits(code, code.length());
                int codeSizeInBytes = ((n-1) / 8) + 1;
                generateExtendedCodes(codeValue, code.length(), codeSizeInBytes)
                        .forEach(c -> table.put(c, new MappedSymbol(entry.getValue(), code.length())));
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

        public MappedSymbol(int character, int codeLength) {
            this.character = (char) character;
            this.codeLength = codeLength;
        }

        char character;
        int codeLength;
    }

}
