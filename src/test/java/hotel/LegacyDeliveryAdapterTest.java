package hotel;

import hotel.delivery.*;
import hotel.domain.*;
import hotel.vendor.LegacyHotelGateway;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class LegacyDeliveryAdapterTest {
    private static DeliveryOrder order(Amenity item, NotificationMode mode) {
        return new DeliveryOrder(new RoomServiceRequest(42, item, 2), mode);
    }

    private static final class GatewayStub extends LegacyHotelGateway {
        String[] reply = {"0", "JOB-TEST"};
        String sku;
        byte count;
        String room;
        int flags;
        int calls;
        LinkFault linkFailure;
        RuntimeException runtimeFailure;

        @Override
        public String[] submit(String sku, byte count, String room, int flags) throws LinkFault {
            calls++;
            this.sku = sku;
            this.count = count;
            this.room = room;
            this.flags = flags;
            if (linkFailure != null) throw linkFailure;
            if (runtimeFailure != null) throw runtimeFailure;
            return reply;
        }
    }

    static Stream<Arguments> conversions() {
        return Stream.of(
                Arguments.of(Amenity.TOWELS, "LIN-TWL", NotificationMode.RING_BELL, 0),
                Arguments.of(Amenity.WATER, "MIN-H2O", NotificationMode.RING_BELL, 0),
                Arguments.of(Amenity.BREAKFAST, "FNB-BRK", NotificationMode.RING_BELL, 0),
                Arguments.of(Amenity.TOWELS, "LIN-TWL", NotificationMode.SILENT, 4),
                Arguments.of(Amenity.WATER, "MIN-H2O", NotificationMode.SILENT, 4),
                Arguments.of(Amenity.BREAKFAST, "FNB-BRK", NotificationMode.SILENT, 4));
    }

    @ParameterizedTest
    @MethodSource("conversions")
    void convertsArgumentOrderTypesAndNotificationFlags(Amenity item, String sku,
                                                        NotificationMode mode, int flags) throws Exception {
        var gateway = new GatewayStub();
        var request = order(item, mode);
        var receipt = new LegacyDeliveryAdapter(gateway).deliver(request);
        assertAll(
                () -> assertEquals(1, gateway.calls),
                () -> assertEquals(sku, gateway.sku),
                () -> assertEquals((byte) 2, gateway.count),
                () -> assertEquals("R0042", gateway.room),
                () -> assertEquals(flags, gateway.flags),
                () -> assertEquals("JOB-TEST", receipt.reference()),
                () -> assertSame(request, receipt.order()));
    }

    @ParameterizedTest
    @CsvSource({"NROOM, ROOM_NOT_FOUND", "STOCK, ITEM_UNAVAILABLE", "BUSY, BUSY", "ARG, INVALID_ORDER"})
    void translatesEveryDocumentedErrorStatus(String status, DeliveryException.Reason expected) {
        var gateway = new GatewayStub();
        gateway.reply = new String[] {status, "vendor-private-detail"};
        assertNormalizedFailure(gateway, expected);
    }

    @Test
    void translatesNullSentinelToUnavailable() {
        var gateway = new GatewayStub();
        gateway.reply = null;
        assertNormalizedFailure(gateway, DeliveryException.Reason.UNAVAILABLE);
    }

    @Test
    void translatesCheckedVendorExceptionWithoutExposingItsCause() {
        var gateway = new GatewayStub();
        gateway.linkFailure = new LegacyHotelGateway.LinkFault("vendor-private-detail");
        assertNormalizedFailure(gateway, DeliveryException.Reason.UNAVAILABLE);
    }

    @Test
    void containsUnexpectedVendorRuntimeFailures() {
        var gateway = new GatewayStub();
        gateway.runtimeFailure = new IllegalStateException("vendor-private-detail");
        assertNormalizedFailure(gateway, DeliveryException.Reason.PROTOCOL_ERROR);
    }

    static Stream<Arguments> malformedReplies() {
        return Stream.<String[]>of(
                new String[0], new String[] {"0"}, new String[] {"0", "ID", "extra"},
                new String[] {null, "ID"}, new String[] {"UNKNOWN", "vendor-private-detail"},
                new String[] {"0", null}, new String[] {"0", ""},
                new String[] {"0", "bad id"}, new String[] {"0", "JOB\n123"})
                .map(reply -> Arguments.of((Object) reply));
    }

    @ParameterizedTest
    @MethodSource("malformedReplies")
    void rejectsMalformedAndUnknownResponses(String[] reply) {
        var gateway = new GatewayStub();
        gateway.reply = reply;
        assertNormalizedFailure(gateway, DeliveryException.Reason.PROTOCOL_ERROR);
    }

    @Test
    void roomAndQuantityUpperBoundsAreNotTruncated() throws Exception {
        var gateway = new GatewayStub();
        new LegacyDeliveryAdapter(gateway).deliver(new DeliveryOrder(
                new RoomServiceRequest(9999, Amenity.TOWELS, 10), NotificationMode.RING_BELL));
        assertEquals("R9999", gateway.room);
        assertEquals((byte) 10, gateway.count);
    }

    private static void assertNormalizedFailure(GatewayStub gateway, DeliveryException.Reason reason) {
        var adapter = new LegacyDeliveryAdapter(gateway);
        var ex = assertThrowsExactly(DeliveryException.class,
                () -> adapter.deliver(order(Amenity.WATER, NotificationMode.SILENT)));
        assertEquals(reason, ex.reason());
        assertNull(ex.getCause());
        assertEquals(0, ex.getSuppressed().length);
        assertFalse(ex.getMessage().contains("vendor-private-detail"));
        assertFalse(ex.getMessage().contains("LinkFault"));
        assertFalse(ex.getMessage().contains("NROOM"));
        assertFalse(ex.getMessage().contains("STOCK"));
        assertFalse(ex.getMessage().contains("ARG"));
        assertEquals(1, gateway.calls);
    }
}
