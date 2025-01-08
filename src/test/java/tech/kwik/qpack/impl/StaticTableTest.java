/*
 * Copyright © 2019, 2020, 2021, 2022, 2023, 2024, 2025 Peter Doornbosch
 *
 * This file is part of Flupke, a HTTP3 client Java library
 *
 * Flupke is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Flupke is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tech.kwik.qpack.impl;


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

    @Test
    public void testFindByNameAndValueMatchesSingleItem() {
        assertThat(staticTable.findByNameAndValue("age", "0")).isEqualTo(2);
    }

    @Test
    public void testFindByNameAndValueMatchesMultipleNames() {
        assertThat(staticTable.findByNameAndValue(":method", "OPTIONS")).isEqualTo(19);
    }

    @Test
    public void testFindByNameAndValueMatchesNoValue() {
        assertThat(staticTable.findByNameAndValue(":status", "201")).isEqualTo(24);
    }
}
