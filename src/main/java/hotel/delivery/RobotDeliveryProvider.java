package hotel.delivery;

import hotel.domain.Amenity;
import hotel.domain.DeliveryOrder;
import hotel.domain.DeliveryReceipt;
import java.util.concurrent.atomic.AtomicInteger;

/** In-process demonstration of an autonomous delivery queue with a three-item tray. */
public final class RobotDeliveryProvider implements DeliveryProvider {
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public String id() {
        return "robot";
    }

    @Override
    public DeliveryReceipt deliver(DeliveryOrder order) throws DeliveryException {
        if (order.request().room() == 901) {
            throw new DeliveryException(DeliveryException.Reason.ROOM_NOT_FOUND,
                    "The room is not registered.");
        }
        if (order.request().amenity() == Amenity.BREAKFAST) {
            throw new DeliveryException(DeliveryException.Reason.ITEM_UNAVAILABLE,
                    "This delivery service cannot carry hot meals.");
        }
        if (order.request().quantity() > 3) {
            throw new DeliveryException(DeliveryException.Reason.INVALID_ORDER,
                    "This delivery service accepts at most three items per trip.");
        }
        return new DeliveryReceipt("ROBOT-" + sequence.incrementAndGet(), order);
    }
}
