package hotel.app;

import hotel.delivery.DeliveryProvider;
import hotel.service.RoomService;

/** Registration hook for new abstraction variants; not an additional complexity module. */
public interface RoomServiceFactory {
    String id();
    RoomService create(DeliveryProvider provider);
}
