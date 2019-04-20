package net.luminis.qpack;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class DecoderTest {

    private Decoder decoder;

    @Before
    public void initDecoder() {
        decoder = new Decoder();
    }

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
    public void parseIndexedHeaderFieldStaticTable() throws IOException {
        Map.Entry<String, String> entry = decoder.parseIndexedHeaderField(wrap((byte) 0xd7));  // index 23

        assertThat(entry.getKey()).isEqualTo(":scheme");
        assertThat(entry.getValue()).isEqualTo("https");
    }

    @Test
    public void parseLiteralHeaderFieldWithNameReferenceStaticTable() throws IOException {
        Map.Entry<String, String> entry = decoder.parseLiteralHeaderFieldWithNameReference(wrap((byte) 0x51, (byte) 0x81, (byte) 0x63));

        assertThat(entry.getKey()).isEqualTo(":path");
        assertThat(entry.getValue()).isEqualTo("/");
    }

    @Test
    public void parseInsertWithNameReferencetoStaticTable() throws IOException {
        decoder.decodeEncoderStream(wrap((byte) 0xc1, (byte) 0x04, (byte) 0x2f, (byte) 0x69, (byte) 0x64, (byte) 0x78));

        assertThat(decoder.lookupDynamicTable(0).getKey()).isEqualTo(":path");
        assertThat(decoder.lookupDynamicTable(0).getValue()).isEqualTo("/idx");
    }

    @Test
    public void parseLiteralHeaderFieldWithoutNameReference() throws IOException {
        Map.Entry<String, String> entry = decoder.parseLiteralHeaderFieldWithoutNameReference(
                wrap((byte) 0x24, (byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x67,
                        (byte) 0x04, (byte) 0x48, (byte) 0x66, (byte) 0x6b, (byte) 0x55));

        assertThat(entry.getKey()).isEqualTo("etag");
        assertThat(entry.getValue()).isEqualTo("HfkU");
    }

    @Test
    public void parseInsertWithoutNameReference() throws IOException {
        decoder.parseInsertWithoutNameReference(wrap(
                (byte) 0x44, (byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x67,
                (byte) 0x04, (byte) 0x59, (byte) 0x72, (byte) 0x67, (byte) 0x3d
        ));

        assertThat(decoder.lookupDynamicTable(0).getKey()).isEqualTo("etag");
        assertThat(decoder.lookupDynamicTable(0).getValue()).isEqualTo("Yrg=");
    }

    private PushbackInputStream wrap(byte... bytes) {
        return new PushbackInputStream(new ByteArrayInputStream(bytes));
    }

}
