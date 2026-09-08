package com.example.support.service;

import com.example.support.exception.TicketMergeException;
import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Rules for merging a duplicate ticket into a surviving one. Holds no state and no
 * dependencies: callers load the tickets, apply these rules and persist the results.
 *
 * <p>Both {@link #mergeInto} and {@link #asTombstone} mutate the ticket they are given,
 * so callers must pass detached copies such as those returned by the repository.
 */
final class TicketMerger {

    static final String TRUNCATION_MARKER = "… [truncated]";

    private TicketMerger() {
    }

    /**
     * Checks the merge rules that depend on the state of the two tickets. Identity,
     * blank ids and existence are the caller's responsibility and are checked first.
     *
     * @throws TicketMergeException if the pair cannot be merged
     */
    static void requireMergeable(Ticket source, Ticket target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (source.isMerged()) {
            throw new TicketMergeException("Ticket " + source.getId()
                    + " has already been merged into " + source.getMergedIntoId());
        }
        if (target.isMerged()) {
            throw new TicketMergeException("Target ticket " + target.getId()
                    + " has already been merged into " + target.getMergedIntoId());
        }
        if (target.getStatus() == TicketStatus.CLOSED) {
            throw new TicketMergeException("Closed ticket " + target.getId()
                    + " cannot be a merge target");
        }
        if (!sameCustomer(source, target)) {
            throw new TicketMergeException("Cannot merge tickets belonging to different customers: '"
                    + source.getCustomerName().trim() + "' and '" + target.getCustomerName().trim() + "'");
        }
    }

    /**
     * Absorbs the source into the target: the priority is raised to the more urgent of
     * the two and the source's subject and description are appended. Identity, status,
     * customer and subject of the target are left alone.
     */
    static Ticket mergeInto(Ticket target, Ticket source, Instant now) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(now, "now");
        target.setPriority(TicketPriority.highestOf(target.getPriority(), source.getPriority()));
        target.setDescription(append(target.getDescription(), mergedFromBlock(source, now)));
        target.setUpdatedAt(now);
        return target;
    }

    /**
     * Turns the source into a tombstone: closed, annotated and pointing at the ticket it
     * was merged into. The status is set directly because a merge is not a lifecycle
     * transition and an already closed ticket must remain mergeable.
     */
    static Ticket asTombstone(Ticket source, String targetId, Instant now) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(now, "now");
        source.setStatus(TicketStatus.CLOSED);
        source.setDescription(append(source.getDescription(), mergedIntoBlock(targetId, now)));
        source.markMergedInto(targetId, now);
        source.setUpdatedAt(now);
        return source;
    }

    private static boolean sameCustomer(Ticket source, Ticket target) {
        return source.getCustomerName().trim().equalsIgnoreCase(target.getCustomerName().trim());
    }

    private static String mergedFromBlock(Ticket source, Instant now) {
        return "\n\n--- Merged from %s (%s, %s) ---\n%s\n\n%s".formatted(
                source.getId(), source.getCustomerName(), now, source.getSubject(), source.getDescription());
    }

    private static String mergedIntoBlock(String targetId, Instant now) {
        return "\n\n--- Merged into %s on %s ---".formatted(targetId, now);
    }

    /**
     * Appends {@code block} to {@code base}, truncating only the block so the result never
     * exceeds the description limit and the original text stays intact. When there is not
     * even room for the truncation marker the base is returned unchanged.
     */
    private static String append(String base, String block) {
        int limit = TicketValidator.MAX_DESCRIPTION_LENGTH;
        if (base.length() + block.length() <= limit) {
            return base + block;
        }
        int available = limit - base.length();
        if (available < TRUNCATION_MARKER.length()) {
            return base;
        }
        return base + block.substring(0, available - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
    }
}
