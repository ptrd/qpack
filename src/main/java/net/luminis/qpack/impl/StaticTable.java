package net.luminis.qpack.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-3.1
// "The static table consists of a predefined static list of header
//   fields, each of which has a fixed index over time."
// "All entries in the static table have a name and a value.  However,
//   values can be empty (that is, have a length of 0)."
public class StaticTable {

    private String[] names = new String[100];
    private String[] values = new String[100];

    StaticTable() {
        Pattern empty =        Pattern.compile("\\|\\s+\\|\\s+\\|\\s+\\|");
        Pattern nameOnly =     Pattern.compile("\\|\\s*(\\d+)\\s*" + "\\|\\s*([^\\|]+)\\s*" + "\\|\\s+\\|");
        Pattern nameValue =    Pattern.compile("\\|\\s*(\\d+)\\s*" + "\\|\\s*([^\\|]+)\\s*" + "\\|\\s*([^\\|]+)\\s*\\|");
        Pattern continuation = Pattern.compile("\\|\\s+"           + "\\|\\s*([^\\|]*)\\s*" + "\\|\\s*([^\\|]*)\\s*\\|");

        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream("statictable.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

            String line;
            int lastIndex = 0;
            line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (empty.matcher(line).matches()) {
                    // Skip
                }
                else if (nameOnly.matcher(line).matches()) {
                    Matcher m = nameOnly.matcher(line);
                    m.matches();
                    names[Integer.parseInt(m.group(1).trim())] = m.group(2).trim();
                    values[Integer.parseInt(m.group(1).trim())] = "";
                    lastIndex = Integer.parseInt(m.group(1).trim());
                }
                else if (nameValue.matcher(line).matches()) {
                    Matcher m = nameValue.matcher(line);
                    m.matches();
                    names[Integer.parseInt(m.group(1).trim())] = m.group(2).trim();
                    values[Integer.parseInt(m.group(1).trim())] = m.group(3).trim();
                    lastIndex = Integer.parseInt(m.group(1).trim());
                }
                else if (continuation.matcher(line).matches()) {
                    Matcher m = continuation.matcher(line);
                    m.matches();
                    String namePart = m.group(1).trim();
                    String valuePart = m.group(2).trim();
                    if (!namePart.isBlank()) {
                        names[lastIndex] = names[lastIndex] + namePart;
                    }
                    if (!valuePart.isBlank()) {
                        values[lastIndex] = values[lastIndex] + valuePart;
                    }
                }
                else {
                    throw new RuntimeException("Internal error: parsing static table definition failed.");
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            // Impossible when library is build correctly.
            throw new RuntimeException("Corrupt library, missing internal resource.");
        }
    }

    public String lookupName(int index) {
        String result = names[index];
        if (result == null) {
            throw new HttpQPackDecompressionFailedException();
        }
        return result;
    }

    public int findByNameAndValue(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        int firstMatch = -1;
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i])) {
                if (firstMatch < 0) {
                    firstMatch = i;
                }
                if (value.equals(values[i])) {
                    return i;
                }
            }
        }
        return firstMatch;
    }

    public Map.Entry<String, String> lookupNameValue(int index) {
        if (names[index] != null) {
            return new AbstractMap.SimpleImmutableEntry(names[index], values[index]);
        }
        else {
            throw new HttpQPackDecompressionFailedException();
        }
    }
}
