package hotel.delivery;

import hotel.domain.DeliveryOrder;
import hotel.domain.DeliveryReceipt;

/**
 * Bridge Implementor. Accepts a validated, non-null order and returns a non-null receipt.
 * All operational failures use DeliveryException. SILENT forbids a knock or doorbell.
 * A provider unable to honor an order must reject it instead of weakening its requirements.
 * The stable, lowercase id is used for runtime registration and routing.
 */
public interface DeliveryProvider {
    String id();

    DeliveryReceipt deliver(DeliveryOrder order) throws DeliveryException;
}
