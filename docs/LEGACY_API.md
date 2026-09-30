# Frozen legacy hotel API

`src/main/java/hotel/vendor/LegacyHotelGateway.java` is a self-contained teaching fixture representing an independently designed hotel gateway. It is not a downloaded SDK or a connection to a real hotel. Its separate protocol is intentional: it has no imports from `hotel.domain`, `hotel.delivery`, `hotel.service`, or `hotel.app`.

SHA-256 of the frozen source:

```text
f47f15ace7bb76c16d2613c36fecfd19e34fe3d0102a26b4ac18c242ae92d700
```

Check it from the project root on macOS or Linux:

```sh
shasum -a 256 src/main/java/hotel/vendor/LegacyHotelGateway.java
```

The integration is implemented in `LegacyDeliveryAdapter`; the wrapped class is not edited to conform to `DeliveryProvider`. Tests subclass the gateway to stub external outcomes, without changing the frozen source.

The native request is `submit(sku, count, roomCode, flags)`. Item codes are `LIN-TWL`, `MIN-H2O`, and `FNB-BRK`; the count is a byte from 1 to 10; room codes are `R0001` through `R9999`; notification flags are `0` for a doorbell and `4` for silence. An accepted response is `["0", "JOB-n"]`. The documented rejection statuses are `NROOM`, `STOCK`, `BUSY`, and `ARG`. The API also signals unavailability with null and connection failure with `LinkFault`.

For valid requests, rooms 901 through 906 exercise the six failure fixtures described in the README. These are deterministic simulation controls, not hotel business data.
