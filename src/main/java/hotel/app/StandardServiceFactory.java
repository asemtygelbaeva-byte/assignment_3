package hotel.app;

import hotel.delivery.DeliveryProvider;
import hotel.service.RoomService;
import hotel.service.StandardRoomService;

public final class StandardServiceFactory implements RoomServiceFactory {
    @Override
    public String id() { return "standard"; }

    @Override
    public RoomService create(DeliveryProvider provider) {
        return new StandardRoomService(provider);
    }
}
