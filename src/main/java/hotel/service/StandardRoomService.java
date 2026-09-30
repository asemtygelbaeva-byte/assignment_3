package hotel.service;

import hotel.delivery.DeliveryProvider;
import hotel.domain.DeliveryOrder;
import hotel.domain.NotificationMode;
import hotel.domain.RoomServiceRequest;

public final class StandardRoomService extends RoomService {
    public StandardRoomService(DeliveryProvider provider) {
        super(provider);
    }

    @Override
    protected DeliveryOrder createOrder(RoomServiceRequest request) {
        return new DeliveryOrder(request, NotificationMode.RING_BELL);
    }
}
