package hotel.delivery;

import hotel.domain.DeliveryOrder;
import hotel.domain.DeliveryReceipt;
import hotel.domain.NotificationMode;
import hotel.vendor.LegacyHotelGateway;
import java.util.Locale;
import java.util.Objects;

/** Object Adapter: only this application class knows the legacy protocol. */
public final class LegacyDeliveryAdapter implements DeliveryProvider {
    private final LegacyHotelGateway gateway;

    public LegacyDeliveryAdapter() {
        this(new LegacyHotelGateway());
    }

    public LegacyDeliveryAdapter(LegacyHotelGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway);
    }

    @Override
    public String id() {
        return "legacy";
    }

    @Override
    public DeliveryReceipt deliver(DeliveryOrder order) throws DeliveryException {
        var request = order.request();
        String sku = switch (request.amenity()) {
            case TOWELS -> "LIN-TWL";
            case WATER -> "MIN-H2O";
            case BREAKFAST -> "FNB-BRK";
        };
        String roomCode = String.format(Locale.ROOT, "R%04d", request.room());
        int flags = order.notification() == NotificationMode.SILENT ? 4 : 0;
        String[] reply;
        try {
            reply = gateway.submit(sku, (byte) request.quantity(), roomCode, flags);
        } catch (LegacyHotelGateway.LinkFault ex) {
            // Deliberately omit the vendor cause and its message at the contract boundary.
            throw failure(DeliveryException.Reason.UNAVAILABLE, "The delivery service is unavailable.");
        } catch (RuntimeException ex) {
            throw failure(DeliveryException.Reason.PROTOCOL_ERROR, "The delivery service failed internally.");
        }
        if (reply == null) {
            throw failure(DeliveryException.Reason.UNAVAILABLE, "The delivery service is unavailable.");
        }
        if (reply.length != 2 || reply[0] == null) {
            throw protocolError();
        }
        return switch (reply[0]) {
            case "0" -> {
                if (reply[1] == null || !reply[1].matches("[A-Z0-9-]+")) {
                    throw protocolError();
                }
                yield new DeliveryReceipt(reply[1], order);
            }
            case "NROOM" -> throw failure(DeliveryException.Reason.ROOM_NOT_FOUND,
                    "The room is not registered.");
            case "STOCK" -> throw failure(DeliveryException.Reason.ITEM_UNAVAILABLE,
                    "The requested item is unavailable.");
            case "BUSY" -> throw failure(DeliveryException.Reason.BUSY,
                    "The delivery queue is full. Try again later.");
            case "ARG" -> throw failure(DeliveryException.Reason.INVALID_ORDER,
                    "The delivery service rejected the order.");
            default -> throw protocolError();
        };
    }

    private static DeliveryException protocolError() {
        return failure(DeliveryException.Reason.PROTOCOL_ERROR,
                "The delivery service returned an invalid response.");
    }

    private static DeliveryException failure(DeliveryException.Reason reason, String message) {
        return new DeliveryException(reason, message);
    }
}
