package com.example.support.service;

import com.example.support.exception.ValidationException;

/** Field-level validation rules shared by create and update operations. */
final class TicketValidator {

    static final int MAX_CUSTOMER_NAME_LENGTH = 100;
    static final int MAX_SUBJECT_LENGTH = 150;
    static final int MAX_DESCRIPTION_LENGTH = 5000;

    private TicketValidator() {
    }

    static String requireCustomerName(String value) {
        return requireText(value, "customerName", MAX_CUSTOMER_NAME_LENGTH);
    }

    static String requireSubject(String value) {
        return requireText(value, "subject", MAX_SUBJECT_LENGTH);
    }

    static String requireDescription(String value) {
        return requireText(value, "description", MAX_DESCRIPTION_LENGTH);
    }

    static String requireTicketId(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("ticketId must not be blank");
        }
        return value.trim();
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field + " must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }
}
