package hotel.domain;

import java.util.Objects;

/** Acceptance of a dispatch job, not proof that physical delivery has finished. */
public record DeliveryReceipt(String reference, DeliveryOrder order) {
    public DeliveryReceipt {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("A dispatch reference is required.");
        }
        Objects.requireNonNull(order, "Order is required.");
    }
}
