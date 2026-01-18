/*
 * Copyright © 2026 Peter Doornbosch
 *
 * This file is part of Flupke, a HTTP3 Java library.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableEntryTest {

    @Test
    void entriesWithSameNameAndValueShouldBeEqual() {
        TableEntry entry1 = new TableEntry("name", "value");
        TableEntry entry2 = new TableEntry("name", "value");
        assertEquals(entry1, entry2);
    }

    @Test
    void entriesWithDifferentNamesShouldNotBeEqual() {
        TableEntry entry1 = new TableEntry("name1", "value");
        TableEntry entry2 = new TableEntry("name2", "value");
        assert !entry1.equals(entry2);
    }

    void entriesWithDifferentValuesShouldNotBeEqual() {
        TableEntry entry1 = new TableEntry("name", "value1");
        TableEntry entry2 = new TableEntry("name", "value2");
        assert !entry1.equals(entry2);
    }

    @Test
    void entryWithNullValueShouldEqualsEntryWithSameNameAndNullValue() {
        TableEntry entry1 = new TableEntry("name", null);
        TableEntry entry2 = new TableEntry("name", null);
        assertEquals(entry1, entry2);

    }
}