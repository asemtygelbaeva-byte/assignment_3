package hotel.domain;

import java.util.Objects;

public record RoomServiceRequest(int room, Amenity amenity, int quantity) {
    public RoomServiceRequest {
        if (room < 1 || room > 9999) {
            throw new IllegalArgumentException("Room must be between 1 and 9999.");
        }
        Objects.requireNonNull(amenity, "Amenity is required.");
        if (quantity < 1 || quantity > 10) {
            throw new IllegalArgumentException("Quantity must be between 1 and 10.");
        }
    }
}
