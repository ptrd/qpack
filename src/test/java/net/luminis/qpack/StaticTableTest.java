package net.luminis.qpack;


import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StaticTableTest {

    private static StaticTable staticTable;

    @BeforeClass
    public static void initStaticTable() {
        staticTable = new StaticTable();
    }

    @Test
    public void testFirstValue() {
        // draft-ietf-quic-qpack-07#section-3.1
        // "Note the QPACK static table is indexed from 0, whereas the HPACK
        //   static table is indexed from 1."
        assertThat(staticTable.lookupName(0)).isEqualTo(":authority");
        assertThat(staticTable.lookupNameValue(0).getKey()).isEqualTo(":authority");
    }

    @Test
    public void testEmptyValue() {
        // "All entries in the static table have a name and a value.  However,
        //   values can be empty (that is, have a length of 0)."
        assertThat(staticTable.lookupNameValue(0).getValue()).isEqualTo("");
    }

    @Test
    public void testPathValue() {
        assertThat(staticTable.lookupName(1)).isEqualTo(":path");
        assertThat(staticTable.lookupNameValue(1).getKey()).isEqualTo(":path");
        assertThat(staticTable.lookupNameValue(1).getValue()).isEqualTo("/");
    }

    @Test
    public void testMethodGet() {
        assertThat(staticTable.lookupName(17)).isEqualTo(":method");
        assertThat(staticTable.lookupNameValue(17).getKey()).isEqualTo(":method");
        assertThat(staticTable.lookupNameValue(17).getValue()).isEqualTo("GET");
    }

    @Test
    public void testNonAlfaNumericValue() {
        assertThat(staticTable.lookupName(29)).isEqualTo("accept");
        assertThat(staticTable.lookupNameValue(29).getKey()).isEqualTo("accept");
        assertThat(staticTable.lookupNameValue(29).getValue()).isEqualTo("*/*");
    }

    @Test
    public void testHeaderWithLongName() {
        assertThat(staticTable.lookupName(33)).isEqualTo("access-control-allow-headers");
        assertThat(staticTable.lookupNameValue(33).getKey()).isEqualTo("access-control-allow-headers");
        assertThat(staticTable.lookupNameValue(33).getValue()).isEqualTo("cache-control");
    }

    @Test
    public void testValueWithSpace() {
        assertThat(staticTable.lookupName(41)).isEqualTo("cache-control");
        assertThat(staticTable.lookupNameValue(41).getKey()).isEqualTo("cache-control");
        assertThat(staticTable.lookupNameValue(41).getValue()).isEqualTo("public, max-age=31536000");
    }

    @Test
    public void testLongValue() {
        assertThat(staticTable.lookupName(47)).isEqualTo("content-type");
        assertThat(staticTable.lookupNameValue(47).getKey()).isEqualTo("content-type");
        assertThat(staticTable.lookupNameValue(47).getValue()).isEqualTo("application/x-www-form-urlencoded");
    }

    @Test
    public void testLastValue() {
        assertThat(staticTable.lookupName(98)).isEqualTo("x-frame-options");
        assertThat(staticTable.lookupNameValue(98).getKey()).isEqualTo("x-frame-options");
        assertThat(staticTable.lookupNameValue(98).getValue()).isEqualTo("sameorigin");
    }

    @Test
    public void testInvalidTableIndex() {
        assertThatThrownBy(
                () -> staticTable.lookupName(99))
                .isInstanceOf(HttpQPackDecompressionFailedException.class);
        assertThatThrownBy(
                () -> staticTable.lookupNameValue(99))
                .isInstanceOf(HttpQPackDecompressionFailedException.class);
    }

}
