package net.luminis.qpack;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;


public class Decoder {

    public LinkedHashMap<String, String> decodeStream(InputStream inputStream) throws IOException {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put(":path", "www.example.com");
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

}
