package hotel;

import hotel.app.*;
import hotel.delivery.*;
import hotel.domain.*;
import hotel.service.RoomService;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RoomServiceApplicationTest {
    @ParameterizedTest
    @CsvSource({
            "standard, staff:512, STAFF-, RING_BELL", "quiet, staff:512, STAFF-, SILENT",
            "standard, robot:512, ROBOT-, RING_BELL", "quiet, robot:512, ROBOT-, SILENT",
            "standard, legacy:512, JOB-, RING_BELL", "quiet, legacy:512, JOB-, SILENT"})
    void inputSelectsAllSixRealBridgeCombinations(String mode, String destination, String prefix,
                                                 NotificationMode notification) throws Exception {
        var result = RoomServiceApplication.discover().request(mode, destination, Amenity.WATER, 2);
        assertTrue(result.reference().startsWith(prefix));
        assertEquals(notification, result.order().notification());
        assertEquals(512, result.order().request().room());
    }

    @ParameterizedTest
    @CsvSource({"901, ROOM_NOT_FOUND", "902, ITEM_UNAVAILABLE", "903, BUSY",
            "904, UNAVAILABLE", "905, UNAVAILABLE", "906, INVALID_ORDER"})
    void realLegacyFixtureFailuresReachTheClientThroughTheCommonContract(int room,
                                                                        DeliveryException.Reason expected) {
        var app = RoomServiceApplication.discover();
        var ex = assertThrows(DeliveryException.class,
                () -> app.request("standard", "legacy:" + room, Amenity.WATER, 1));
        assertEquals(expected, ex.reason());
        assertNull(ex.getCause());
    }

    @Test
    void productionHasExactlyThreeProvidersAndTwoServices() {
        assertEquals(3, ServiceLoader.load(DeliveryProvider.class).stream().count());
        assertEquals(2, ServiceLoader.load(RoomServiceFactory.class).stream().count());
    }

    // Test-only extensions demonstrate OCP without editing any production class.
    private static final class LockerDelivery implements DeliveryProvider {
        public String id() { return "locker"; }
        public DeliveryReceipt deliver(DeliveryOrder order) {
            return new DeliveryReceipt("LOCKER-1", order);
        }
    }

    private static final class CourtesyRoomService extends RoomService {
        CourtesyRoomService(DeliveryProvider provider) { super(provider); }
        protected DeliveryOrder createOrder(RoomServiceRequest request) {
            var singleItem = new RoomServiceRequest(request.room(), request.amenity(), 1);
            return new DeliveryOrder(singleItem, NotificationMode.SILENT);
        }
    }

    @Test
    void newProviderWorksWithExistingAbstractionsAndUnchangedRouter() throws Exception {
        List<DeliveryProvider> providers = new ArrayList<>();
        ServiceLoader.load(DeliveryProvider.class).forEach(providers::add);
        providers.add(new LockerDelivery());
        var app = new RoomServiceApplication(providers, ServiceLoader.load(RoomServiceFactory.class));
        for (String mode : List.of("standard", "quiet")) {
            assertEquals("LOCKER-1", app.request(mode, "locker:512", Amenity.TOWELS, 2).reference());
        }
    }

    @Test
    void newAbstractionWorksWithAllExistingProvidersAndUnchangedRouter() throws Exception {
        RoomServiceFactory courtesy = new RoomServiceFactory() {
            public String id() { return "courtesy"; }
            public RoomService create(DeliveryProvider provider) { return new CourtesyRoomService(provider); }
        };
        List<RoomServiceFactory> services = new ArrayList<>();
        ServiceLoader.load(RoomServiceFactory.class).forEach(services::add);
        services.add(courtesy);
        var app = new RoomServiceApplication(ServiceLoader.load(DeliveryProvider.class), services);
        for (String id : List.of("staff", "robot", "legacy")) {
            var result = app.request("courtesy", id + ":512", Amenity.TOWELS, 2);
            assertEquals(1, result.order().request().quantity());
            assertEquals(NotificationMode.SILENT, result.order().notification());
        }
    }

    @Test
    void duplicateProviderIdsFailInsteadOfSilentlyChangingTheRoute() {
        assertThrows(IllegalArgumentException.class, () -> new RoomServiceApplication(
                List.of(new LockerDelivery(), new LockerDelivery()), List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "staff:", "staff:0", "staff:-1", "staff:10000",
            "staff:abc", "staff:1:2", "missing:512"})
    void rejectsInvalidDestinations(String destination) {
        var app = RoomServiceApplication.discover();
        assertThrows(IllegalArgumentException.class,
                () -> app.request("standard", destination, Amenity.WATER, 1));
    }

    @Test
    void rejectsAnUnknownServiceMode() {
        assertThrows(IllegalArgumentException.class, () -> RoomServiceApplication.discover()
                .request("missing", "staff:512", Amenity.WATER, 1));
    }
}
