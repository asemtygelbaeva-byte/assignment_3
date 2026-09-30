package hotel.domain;

import java.util.Objects;

public record DeliveryOrder(RoomServiceRequest request, NotificationMode notification) {
    public DeliveryOrder {
        Objects.requireNonNull(request, "Request is required.");
        Objects.requireNonNull(notification, "Notification mode is required.");
    }
}
