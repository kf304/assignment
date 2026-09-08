package com.example.support.service;

import java.util.concurrent.atomic.AtomicLong;

/** Generates human-friendly, ascending ids such as {@code TCK-1001}. */
public class SequentialTicketIdGenerator implements TicketIdGenerator {

    private static final String PREFIX = "TCK-";
    private static final long FIRST_NUMBER = 1000L;

    private final AtomicLong counter;

    public SequentialTicketIdGenerator() {
        this(FIRST_NUMBER);
    }

    public SequentialTicketIdGenerator(long firstNumber) {
        this.counter = new AtomicLong(firstNumber);
    }

    @Override
    public String nextId() {
        return PREFIX + counter.getAndIncrement();
    }
}
