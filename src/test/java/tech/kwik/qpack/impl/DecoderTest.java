package tech.kwik.qpack.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class DecoderTest {

    private DecoderImpl decoder;

    @Before
    public void initDecoder() {
        decoder = new DecoderImpl();
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
    public void parseIndexedHeaderFieldShouldThrowWhenStreamEmpty() throws IOException {
        assertThatThrownBy(
                () -> decoder.parseIndexedHeaderField(wrap(new byte[0]))
        ).isInstanceOf(EOFException.class);
    }

    @Test
    public void parseIndexedHeaderFieldShouldThrowWhenStreamTruncated() throws IOException {
        assertThatThrownBy(
                () -> decoder.parseIndexedHeaderField(wrap((byte) 0xff))
        ).isInstanceOf(EOFException.class);
    }

    @Test
    public void parseLiteralHeaderFieldWithNameReferenceStaticTable() throws IOException {
        Map.Entry<String, String> entry = decoder.parseLiteralHeaderFieldWithNameReference(wrap((byte) 0x51, (byte) 0x81, (byte) 0x63));

        assertThat(entry.getKey()).isEqualTo(":path");
        assertThat(entry.getValue()).isEqualTo("/");
    }

    @Test
    public void parseLiteralHeaderFieldWithNameReferenceShouldThrowWhenStreamEmpty() throws IOException {
        assertThatThrownBy(
                () -> decoder.parseLiteralHeaderFieldWithNameReference(wrap(new byte[0]))
        ).isInstanceOf(EOFException.class);
    }

    @Test
    public void parseLiteralHeaderFieldWithNameReferenceShouldThrowWhenStreamTruncated() throws IOException {
        assertThatThrownBy(
                () -> decoder.parseLiteralHeaderFieldWithNameReference(wrap((byte) 0x51, (byte) 0x81))
        ).isInstanceOf(EOFException.class);
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
    public void parseLiteralHeaderFieldWithoutNameReferenceShouldThrowWhenStreamEmtpy() throws IOException {
        assertThatThrownBy(
                () -> decoder.parseLiteralHeaderFieldWithoutNameReference(wrap(new byte[0]))
                ).isInstanceOf(EOFException.class);
    }

    @Test
    public void parseLiteralHeaderFieldWithoutNameReferenceShouldThrowWhenStreamTruncated() throws IOException {
        assertThatThrownBy(
                () -> decoder.parseLiteralHeaderFieldWithoutNameReference(
                        wrap((byte) 0x24, (byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x67,
                                (byte) 0x04, (byte) 0x48, (byte) 0x66, (byte) 0x6b)) //, (byte) 0x55))
                ).isInstanceOf(EOFException.class);
    }

    @Test
    public void parseNonAsciiCharInLiteralHeaderFieldWithoutNameReference() throws IOException {
        Map.Entry<String, String> entry = decoder.parseLiteralHeaderFieldWithoutNameReference(
                wrap((byte) 0x24, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65,
                        (byte) 0x04, (byte) 0x42, (byte) 0xf6, (byte) 0x72, (byte) 0x6e));

        assertThat(entry.getKey()).isEqualTo("name");
        String value = entry.getValue();
        assertThat(value.charAt(0)).isEqualTo('B');
        assertThat((int) value.charAt(1)).isEqualTo(0xf6);
        assertThat(value.charAt(1)).isEqualTo('ö');   // This works because in UTF-16, 246 is also ö
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
