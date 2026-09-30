package hotel;

import hotel.app.Main;
import hotel.delivery.*;
import hotel.domain.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class NativeProvidersAndCliTest {
    @Test
    void staffCanDispatchABreakfastOrder() throws Exception {
        var order = new DeliveryOrder(new RoomServiceRequest(512, Amenity.BREAKFAST, 2), NotificationMode.SILENT);
        assertEquals(order, new StaffDeliveryProvider().deliver(order).order());
    }

    @ParameterizedTest
    @CsvSource({"BREAKFAST, 1, ITEM_UNAVAILABLE", "WATER, 4, INVALID_ORDER"})
    void robotUsesCommonErrorsForUnsupportedOrders(Amenity item, int count, DeliveryException.Reason reason) {
        var order = new DeliveryOrder(new RoomServiceRequest(512, item, count), NotificationMode.RING_BELL);
        var ex = assertThrows(DeliveryException.class, () -> new RobotDeliveryProvider().deliver(order));
        assertEquals(reason, ex.reason());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "10000, 1", "512, 0", "512, 11"})
    void validatesInputBeforeAnyProviderIsCalled(int room, int quantity) {
        assertThrows(IllegalArgumentException.class, () -> new RoomServiceRequest(room, Amenity.WATER, quantity));
    }

    @Test
    void cliPrintsNormalizedSuccess() {
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        int exit = Main.run(new String[] {"quiet", "legacy:512", "water", "2"},
                new PrintStream(output), new PrintStream(errors));
        assertEquals(0, exit);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("notification=SILENT"));
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void cliPrintsNormalizedFailureAndReturnsNonzero() {
        var errors = new ByteArrayOutputStream();
        int exit = Main.run(new String[] {"standard", "legacy:905", "water", "2"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(errors));
        assertEquals(1, exit);
        String message = errors.toString(StandardCharsets.UTF_8);
        assertTrue(message.contains("[UNAVAILABLE]"));
        assertFalse(message.contains("LinkFault"));
        assertFalse(message.contains("transport"));
    }

    @Test
    void cliHandlesInvalidQuantity() {
        var errors = new ByteArrayOutputStream();
        int exit = Main.run(new String[] {"standard", "staff:512", "water", "many"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(errors));
        assertEquals(2, exit);
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("INVALID INPUT"));
    }

    @Test
    void helpIsSuccessfulAndMissingArgumentsAreAnInputError() {
        var sink = new PrintStream(new ByteArrayOutputStream());
        assertEquals(0, Main.run(new String[] {"--help"}, sink, sink));
        assertEquals(2, Main.run(new String[0], sink, sink));
    }
}
