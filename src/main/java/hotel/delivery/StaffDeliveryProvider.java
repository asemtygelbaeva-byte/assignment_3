package hotel.delivery;

import hotel.domain.DeliveryOrder;
import hotel.domain.DeliveryReceipt;
import java.util.concurrent.atomic.AtomicInteger;

/** In-process demonstration of a staff dispatch queue. */
public final class StaffDeliveryProvider implements DeliveryProvider {
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public String id() {
        return "staff";
    }

    @Override
    public DeliveryReceipt deliver(DeliveryOrder order) throws DeliveryException {
        if (order.request().room() == 901) {
            throw new DeliveryException(DeliveryException.Reason.ROOM_NOT_FOUND,
                    "The room is not registered.");
        }
        return new DeliveryReceipt("STAFF-" + sequence.incrementAndGet(), order);
    }
}
