package hotel.service;

import hotel.delivery.DeliveryException;
import hotel.delivery.DeliveryProvider;
import hotel.domain.DeliveryOrder;
import hotel.domain.DeliveryReceipt;
import hotel.domain.RoomServiceRequest;
import java.util.Objects;

/** Bridge Abstraction: guest-service policy varies separately from dispatch technology. */
public abstract class RoomService {
    private final DeliveryProvider provider;

    protected RoomService(DeliveryProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    public final DeliveryReceipt request(RoomServiceRequest request) throws DeliveryException {
        return provider.deliver(createOrder(Objects.requireNonNull(request)));
    }

    protected abstract DeliveryOrder createOrder(RoomServiceRequest request) throws DeliveryException;
}
