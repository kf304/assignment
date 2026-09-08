package com.example.support.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import org.junit.jupiter.api.Test;

class TicketFilterTest {

    @Test
    void threeArgumentConstructorKeepsMergedTicketsIncluded() {
        TicketFilter filter = new TicketFilter(TicketStatus.OPEN, TicketPriority.LOW, "Marco Silva");

        assertEquals(TicketStatus.OPEN, filter.status());
        assertEquals(TicketPriority.LOW, filter.priority());
        assertEquals("Marco Silva", filter.customerName());
        assertFalse(filter.excludeMerged(), "existing behaviour must be preserved");
    }

    @Test
    void existingFactoriesKeepMergedTicketsIncluded() {
        assertFalse(TicketFilter.none().excludeMerged());
        assertFalse(TicketFilter.byStatus(TicketStatus.OPEN).excludeMerged());
        assertFalse(TicketFilter.byPriority(TicketPriority.URGENT).excludeMerged());
        assertFalse(TicketFilter.byCustomer("Priya Nair").excludeMerged());
    }

    @Test
    void excludingMergedSetsOnlyThatCriterion() {
        TicketFilter filter = TicketFilter.excludingMerged();

        assertTrue(filter.excludeMerged());
        assertNull(filter.status());
        assertNull(filter.priority());
        assertNull(filter.customerName());
    }

    @Test
    void canonicalConstructorCarriesTheFlag() {
        TicketFilter filter = new TicketFilter(TicketStatus.OPEN, null, null, true);

        assertTrue(filter.excludeMerged());
        assertEquals(TicketStatus.OPEN, filter.status());
    }
}
