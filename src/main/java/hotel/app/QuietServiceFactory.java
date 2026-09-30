package hotel.app;

import hotel.delivery.DeliveryProvider;
import hotel.service.RoomService;
import hotel.service.QuietRoomService;

public final class QuietServiceFactory implements RoomServiceFactory {
    @Override
    public String id() { return "quiet"; }

    @Override
    public RoomService create(DeliveryProvider provider) {
        return new QuietRoomService(provider);
    }
}
