package hotel.service;

import hotel.delivery.DeliveryException;
import hotel.delivery.DeliveryProvider;
import hotel.domain.DeliveryOrder;
import hotel.domain.NotificationMode;
import hotel.domain.RoomServiceRequest;

/** Example hotel policy: silent handover and small orders to reduce disturbance. */
public final class QuietRoomService extends RoomService {
    public QuietRoomService(DeliveryProvider provider) {
        super(provider);
    }

    @Override
    protected DeliveryOrder createOrder(RoomServiceRequest request) throws DeliveryException {
        if (request.quantity() > 3) {
            throw new DeliveryException(DeliveryException.Reason.INVALID_ORDER,
                    "Quiet service accepts at most three items per request.");
        }
        return new DeliveryOrder(request, NotificationMode.SILENT);
    }
}
