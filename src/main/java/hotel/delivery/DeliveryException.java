package hotel.delivery;

import java.util.Objects;

/** Provider-independent errors. Vendor exceptions and raw protocol data are never exposed. */
public final class DeliveryException extends Exception {
    public enum Reason {
        ROOM_NOT_FOUND, ITEM_UNAVAILABLE, BUSY, INVALID_ORDER, UNAVAILABLE, PROTOCOL_ERROR
    }

    private final Reason reason;

    public DeliveryException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason);
    }

    public Reason reason() {
        return reason;
    }
}
