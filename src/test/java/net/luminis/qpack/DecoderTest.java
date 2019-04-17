package net.luminis.qpack;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class DecoderTest {

    private Decoder decoder = new Decoder();

    @Test
    public void parseIntegerWith5bitPrefix() throws IOException {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.1
        long value = decoder.parsePrefixedInteger(5, wrap((byte) 0x0a));

        assertThat(value).isEqualTo(10);
    }

    @Test
    public void parsePrefixedInteger() throws IOException {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.2
        long value = decoder.parsePrefixedInteger(5, wrap((byte) 0xff, (byte) 0x9a, (byte) 0x0a));

        assertThat(value).isEqualTo(1337);
    }

    @Test
    public void parseIntegerStartingAtOctetBoundary() throws IOException {
        // Taken from https://tools.ietf.org/html/rfc7541#appendix-C.1.3
        long value = decoder.parsePrefixedInteger(8, wrap((byte) 42));

        assertThat(value).isEqualTo(42);
    }

    @Test
    public void parseLiteralHeaderFieldWithNameReferenceStaticTable() throws IOException {
        Map.Entry<String, String> entry = decoder.parseLiteralHeaderFieldWithNameReference(wrap((byte) 0x51, (byte) 0x81, (byte) 0x63));

        assertThat(entry.getKey()).isEqualTo(":path");
        assertThat(entry.getValue()).isEqualTo("/");
    }

    private PushbackInputStream wrap(byte... bytes) {
        return new PushbackInputStream(new ByteArrayInputStream(bytes));
    }

}
