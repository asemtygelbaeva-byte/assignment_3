package hotel;

import hotel.delivery.DeliveryException;
import hotel.delivery.DeliveryProvider;
import hotel.domain.*;
import hotel.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {
    private static final RoomServiceRequest REQUEST = new RoomServiceRequest(512, Amenity.WATER, 2);

    private static final class RecordingProvider implements DeliveryProvider {
        int calls;
        DeliveryOrder received;
        DeliveryReceipt receipt;
        DeliveryException failure;

        public String id() { return "recording"; }

        public DeliveryReceipt deliver(DeliveryOrder order) throws DeliveryException {
            calls++;
            received = order;
            if (failure != null) throw failure;
            receipt = new DeliveryReceipt("TEST-1", order);
            return receipt;
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothRefinementsDelegateExactlyOnceAndReturnTheProvidersReceipt(boolean quiet) throws Exception {
        var provider = new RecordingProvider();
        RoomService service = quiet ? new QuietRoomService(provider) : new StandardRoomService(provider);
        var receipt = service.request(REQUEST);
        assertEquals(1, provider.calls);
        assertSame(REQUEST, provider.received.request());
        assertEquals(quiet ? NotificationMode.SILENT : NotificationMode.RING_BELL,
                provider.received.notification());
        assertSame(provider.receipt, receipt);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothRefinementsPreserveTheCommonFailureContract(boolean quiet) {
        var provider = new RecordingProvider();
        provider.failure = new DeliveryException(DeliveryException.Reason.BUSY, "Try later.");
        RoomService service = quiet ? new QuietRoomService(provider) : new StandardRoomService(provider);
        var failure = assertThrows(DeliveryException.class, () -> service.request(REQUEST));
        assertSame(provider.failure, failure);
        assertEquals(1, provider.calls);
    }

    @Test
    void quietPolicyRejectsLargeOrdersBeforeDispatch() {
        var provider = new RecordingProvider();
        var failure = assertThrows(DeliveryException.class, () -> new QuietRoomService(provider)
                .request(new RoomServiceRequest(512, Amenity.TOWELS, 4)));
        assertEquals(DeliveryException.Reason.INVALID_ORDER, failure.reason());
        assertEquals(0, provider.calls);
    }

    @Test
    void standardPolicyCanDelegateALargeOrder() throws Exception {
        var provider = new RecordingProvider();
        new StandardRoomService(provider).request(new RoomServiceRequest(512, Amenity.TOWELS, 10));
        assertEquals(10, provider.received.request().quantity());
    }

    @Test
    void quietPolicyAcceptsItsBoundaryQuantity() throws Exception {
        var provider = new RecordingProvider();
        new QuietRoomService(provider).request(new RoomServiceRequest(512, Amenity.TOWELS, 3));
        assertEquals(1, provider.calls);
    }
}
