package com.example.support.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TicketMergeExceptionTest {

    @Test
    void keepsTheSuppliedMessage() {
        TicketMergeException exception = new TicketMergeException("Cannot merge ticket TCK-1002 into itself");

        assertEquals("Cannot merge ticket TCK-1002 into itself", exception.getMessage());
    }

    @Test
    void isAnUncheckedExceptionLikeTheOtherDomainExceptions() {
        assertTrue(RuntimeException.class.isAssignableFrom(TicketMergeException.class));
    }

    @Test
    void isDistinctFromValidationAndNotFoundSoTheControllerCanMapItSeparately() {
        assertFalse(ValidationException.class.isAssignableFrom(TicketMergeException.class));
        assertFalse(TicketNotFoundException.class.isAssignableFrom(TicketMergeException.class));
        assertFalse(InvalidStatusTransitionException.class.isAssignableFrom(TicketMergeException.class));
    }

    @Test
    void isThrowableAndCatchableByItsOwnType() {
        TicketMergeException exception = assertThrows(TicketMergeException.class, () -> {
            throw new TicketMergeException("boom");
        });

        assertEquals("boom", exception.getMessage());
    }
}
