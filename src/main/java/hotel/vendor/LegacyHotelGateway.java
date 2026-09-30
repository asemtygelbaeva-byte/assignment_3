package hotel.vendor;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Frozen teaching fixture for an independently designed legacy hotel API.
 * This is a simulation, not a real third-party SDK. No hotel application types are used.
 *
 * Native call: SKU first, byte count, room text (R0001..R9999), notification flags last.
 * Flags: 0 rings the bell; 4 requests silent delivery.
 * Result: two strings [status, jobId]. Status "0" means accepted. Error statuses:
 * NROOM, STOCK, BUSY, ARG. Null means unavailable. LinkFault is a transport failure.
 * Rooms 901..906 are deterministic failure fixtures; other rooms accept valid requests.
 */
public class LegacyHotelGateway {
    private final AtomicInteger sequence = new AtomicInteger();

    public String[] submit(String sku, byte count, String roomCode, int flags) throws LinkFault {
        if (sku == null || !Set.of("LIN-TWL", "MIN-H2O", "FNB-BRK").contains(sku)
                || count < 1 || count > 10 || roomCode == null
                || !roomCode.matches("R[0-9]{4}") || roomCode.equals("R0000")
                || (flags != 0 && flags != 4)) {
            return new String[] {"ARG", ""};
        }
        return switch (roomCode) {
            case "R0901" -> new String[] {"NROOM", ""};
            case "R0902" -> new String[] {"STOCK", ""};
            case "R0903" -> new String[] {"BUSY", ""};
            case "R0904" -> null;
            case "R0905" -> throw new LinkFault("Legacy transport link dropped.");
            case "R0906" -> new String[] {"ARG", ""};
            default -> new String[] {"0", "JOB-" + sequence.incrementAndGet()};
        };
    }

    public static class LinkFault extends Exception {
        public LinkFault(String message) {
            super(message);
        }
    }
}
