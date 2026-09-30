package hotel.app;

import hotel.delivery.DeliveryException;
import hotel.delivery.DeliveryProvider;
import hotel.domain.Amenity;
import hotel.domain.DeliveryReceipt;
import hotel.domain.RoomServiceRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

/** Selects the provider from the destination prefix supplied by the caller. */
public final class RoomServiceApplication {
    private final Map<String, DeliveryProvider> providers;
    private final Map<String, RoomServiceFactory> services;

    public RoomServiceApplication(Iterable<DeliveryProvider> providers,
                                  Iterable<RoomServiceFactory> services) {
        this.providers = index(providers, DeliveryProvider::id);
        this.services = index(services, RoomServiceFactory::id);
    }

    public static RoomServiceApplication discover() {
        return new RoomServiceApplication(ServiceLoader.load(DeliveryProvider.class),
                ServiceLoader.load(RoomServiceFactory.class));
    }

    public DeliveryReceipt request(String mode, String destination, Amenity amenity, int quantity)
            throws DeliveryException {
        RoomServiceFactory factory = services.get(mode);
        if (factory == null) throw new IllegalArgumentException("Unknown service mode: " + mode);
        String[] parts = destination.split(":", -1);
        if (parts.length != 2 || !parts[1].matches("[0-9]{1,4}")) {
            throw new IllegalArgumentException("Destination must have the form provider:room.");
        }
        DeliveryProvider provider = providers.get(parts[0]);
        if (provider == null) throw new IllegalArgumentException("Unknown delivery provider: " + parts[0]);
        var request = new RoomServiceRequest(Integer.parseInt(parts[1]), amenity, quantity);
        return factory.create(provider).request(request);
    }

    private static <T> Map<String, T> index(Iterable<T> entries, Function<T, String> id) {
        Map<String, T> result = new HashMap<>();
        for (T entry : entries) {
            String key = id.apply(entry);
            if (key == null || !key.matches("[a-z][a-z0-9-]*")) {
                throw new IllegalArgumentException("Plugin IDs must be lowercase names.");
            }
            if (result.putIfAbsent(key, entry) != null) {
                throw new IllegalArgumentException("Duplicate plugin ID: " + key);
            }
        }
        return Map.copyOf(result);
    }
}
