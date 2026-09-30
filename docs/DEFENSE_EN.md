# Oral defense guide

**Student:** Assem Tugelbay  
**Group:** SE-2540

## A short opening explanation

My system automates hotel room-service requests. Guests can choose standard service, which permits a doorbell, or quiet service, which requires silence and limits order size. The hotel can dispatch through staff, a delivery robot, or a legacy gateway. I used Bridge because service policies and delivery technologies change independently. I used Adapter because the legacy gateway does not accept the typed orders or report failures in the form required by the delivery interface.

The adapter is one implementation of the bridge interface. For example, the same quiet-service object can use a robot or the legacy gateway. The service policy does not need to know which technology is behind the interface.

## Show the code in this order

1. Open `DeliveryProvider`. Explain the typed order, receipt, and common exception contract.
2. Open `RoomService`. Point to the injected provider field and the final `request` method.
3. Compare `StandardRoomService` with `QuietRoomService`. Show the notification change and quiet-order limit.
4. Open the two native providers. Explain that both directly implement the shared interface.
5. Open `LegacyHotelGateway` and compare its signature and failure mechanisms with the interface.
6. Open `LegacyDeliveryAdapter`. Show input conversion, status translation, null handling, and exception normalization.
7. Open `RoomServiceApplication` and the two service descriptors. Explain runtime selection without a provider switch.
8. Run the tests and identify the recording provider, gateway stub, and test-only OCP extensions.

## Demonstration commands

Build and run all tests:

```sh
mvn clean verify
```

Show the same policy with different technologies:

```sh
java -jar target/smart-hotel-1.0.0.jar quiet staff:512 water 2
java -jar target/smart-hotel-1.0.0.jar quiet robot:512 water 2
java -jar target/smart-hotel-1.0.0.jar quiet legacy:512 water 2
```

All three receipts must show `notification=SILENT`. Explain that these are simulated acceptance receipts, not proof of physical delivery.

Show a different policy using the same adapted provider:

```sh
java -jar target/smart-hotel-1.0.0.jar standard legacy:512 water 2
```

The receipt now shows `RING_BELL`. The legacy adapter did not change.

Show error translation and policy validation:

```sh
java -jar target/smart-hotel-1.0.0.jar standard legacy:903 water 1
java -jar target/smart-hotel-1.0.0.jar standard legacy:905 water 1
java -jar target/smart-hotel-1.0.0.jar quiet staff:512 towels 4
```

These demonstrate `BUSY`, `UNAVAILABLE`, and `INVALID_ORDER`. The last request is rejected before the provider is called.

## Likely questions and answers

**Where is the bridge?**

The `DeliveryProvider` reference inside `RoomService` connects the service-policy hierarchy to the delivery-implementation hierarchy. The service delegates through the interface rather than inheriting from a delivery implementation.

**Why is this more than a strategy selected by a string?**

Runtime selection is an additional requirement. The structural design has two independently extensible hierarchies: service refinements inherit from `RoomService`, and delivery implementations implement `DeliveryProvider`. Selecting an algorithm alone would not establish this two-axis structure.

**Why are both patterns necessary?**

Bridge provides independent variation of policy and technology. Adapter makes the incompatible gateway usable as one of those technologies. Removing the adapter leaves a signature, data, and error mismatch; removing the bridge loses the explicit independent service-policy hierarchy.

**Is the legacy class only different by method name?**

No. It uses four scalar parameters in a different order, string SKU and room encodings, a byte count, numeric notification flags, a string-array result, status codes, a null sentinel, and its own checked exception. The adapter converts all of these into the application contract.

**Did you modify the legacy source?**

The gateway is a frozen teaching fixture that represents a separately designed API. It does not implement or import application contracts. The recorded checksum documents its unchanged source; all integration changes belong to the adapter. It is not claimed to be an actual third-party SDK.

**What happens when the gateway adds an unknown error code?**

The adapter throws `DeliveryException` with reason `PROTOCOL_ERROR` and a generic message. It does not assume that an unknown response means success.

**Why do you not preserve the original exception as the cause?**

The assignment requires that adapted-class-specific failures do not leak through. A cause would expose `LinkFault` and vendor diagnostics. The public exception therefore contains only the shared reason and normalized message. A real deployment could record a sanitized internal diagnostic separately.

**Can every provider handle every valid request?**

No. Every provider accepts the same contract, but capabilities differ. The robot rejects breakfast and orders larger than its tray capacity. Such limitations are reported through the shared error contract. The six basic combinations are demonstrated with a supported two-item water order.

**Which required complexity module did you choose?**

Dynamic implementor selection. The input destination supplies the provider ID. `ServiceLoader` discovers providers, and the application resolves the ID and injects the chosen instance. There is no two-way adapter.

**How would you add a fourth provider?**

Implement `DeliveryProvider`, give it a unique ID and public no-argument constructor, and register its class in the provider descriptor. No existing Java class changes. The tests demonstrate this with a locker provider.

**How would you add another service policy?**

Extend `RoomService`, implement `createOrder`, provide a `RoomServiceFactory`, and register it. No existing Java class changes. The tests demonstrate a courtesy policy that dispatches a single item silently through every existing provider.

**What do the factories do here?**

They let the registry construct new service refinements while injecting the selected provider. They are a small extension mechanism; Bridge and Adapter remain the main design patterns under assessment.

**How are the tests independent of the delivery systems?**

A recording provider captures the exact order and number of calls. A gateway stub returns selected responses or throws selected exceptions. This allows tests to check behavior without network access, hardware, or timing dependencies. Separate integration tests use the real in-process fixtures.

**What is the main limitation?**

Dispatch is synchronous and has no persistent status or idempotency. A failed connection may leave acceptance uncertain, so automatic retries could duplicate orders. Safe retries require a durable request identity and status lookup.

**Why do factories and service files not violate Open Closed?**

The requirement is that adding a variant should not require editing existing classes. New classes and configuration entries are expected. There is no branching change in the application or existing service/provider code.

## Before the defense

Be able to trace one quiet legacy request by hand, explain each conversion, locate both test doubles, and add a small new variant. Read the limitation carefully and distinguish dispatch acceptance from delivery completion. The project files support the explanation, but the defense must demonstrate your own understanding.
