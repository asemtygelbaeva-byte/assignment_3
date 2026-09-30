package hotel.app;

import hotel.delivery.DeliveryException;
import hotel.domain.Amenity;
import java.io.PrintStream;
import java.util.Locale;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && args[0].equals("--help")) {
            usage(out);
            return 0;
        }
        if (args.length != 4) {
            usage(err);
            return 2;
        }
        try {
            var receipt = RoomServiceApplication.discover().request(args[0], args[1],
                    Amenity.valueOf(args[2].toUpperCase(Locale.ROOT)), Integer.parseInt(args[3]));
            var request = receipt.order().request();
            out.printf(Locale.ROOT, "ACCEPTED %s | room=%d | item=%s | quantity=%d | notification=%s%n",
                    receipt.reference(), request.room(), request.amenity(), request.quantity(),
                    receipt.order().notification());
            return 0;
        } catch (DeliveryException ex) {
            err.println("DELIVERY FAILED [" + ex.reason() + "]: " + ex.getMessage());
            return 1;
        } catch (IllegalArgumentException ex) {
            err.println("INVALID INPUT: " + ex.getMessage());
            usage(err);
            return 2;
        }
    }

    private static void usage(PrintStream out) {
        out.println("Usage: java -jar target/smart-hotel-1.0.0.jar <mode> <provider:room> <amenity> <quantity>");
        out.println("See README.md for registered modes, providers, and examples.");
    }
}
