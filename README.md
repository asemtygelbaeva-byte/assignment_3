# Smart Hotel Room Service and Automation System

**Student:** Assem Tugelbay  
**Group:** SE-2540  
**Assignment:** 3 — Adapter and Bridge patterns

Assignment 3 combines **Bridge** and **Adapter** in one Java 17 console application. The hotel offers standard and quiet room service, while dispatch can be handled by staff, a delivery robot, or a legacy hotel gateway. Service policy and delivery technology vary independently.

The implementation automates request routing, service-policy validation, dispatch acceptance, and failure reporting. All three delivery systems are deterministic, in-process simulations. The program does not control real equipment or claim that a physical delivery has completed.

## Build and test

Requirements: **JDK 17 or newer and Maven 3.9 or newer**. The first build needs internet access to download Maven plugins and JUnit. There are no runtime library dependencies.

```sh
mvn clean verify
```

This single command compiles the application, runs all JUnit 5 tests, and creates `target/smart-hotel-1.0.0.jar`. To run only the tests:

```sh
mvn test
```

Open `pom.xml` as a Maven project in IntelliJ IDEA if you prefer an IDE. Use JDK 17+ and run `hotel.app.Main` with one of the argument lists below. The code and service-registration resources use the standard Maven layout.

## Run

```sh
java -jar target/smart-hotel-1.0.0.jar standard staff:512 towels 2
java -jar target/smart-hotel-1.0.0.jar quiet robot:512 water 2
java -jar target/smart-hotel-1.0.0.jar quiet legacy:512 breakfast 2
```

Example output for the last command:

```text
ACCEPTED JOB-1 | room=512 | item=BREAKFAST | quantity=2 | notification=SILENT
```

Arguments are `<mode> <provider:room> <amenity> <quantity>`.

| Input | Accepted values |
| --- | --- |
| Mode | `standard`, `quiet` |
| Provider | `staff`, `robot`, `legacy` |
| Room | Integer from 1 to 9999 |
| Amenity | `towels`, `water`, `breakfast`, case-insensitive |
| Quantity | Integer from 1 to 10; quiet service and robot deliveries allow at most 3 |

Mode and provider IDs are lowercase. The robot cannot carry breakfast because this demo models a robot without a heated compartment. A provider rejects an unsupported order through the shared error contract. It never silently changes the requested item, quantity, or notification mode.

The client reads the provider from the input destination: `robot:512` selects the robot and `legacy:512` selects the adapter. There is no provider switch, fallback, or concrete provider construction in `Main` or `RoomServiceApplication`.

## Pattern roles

| Role | Class or interface |
| --- | --- |
| Bridge Abstraction | `RoomService` |
| Refined Abstractions | `StandardRoomService`, `QuietRoomService` |
| Bridge Implementor | `DeliveryProvider` |
| Native Concrete Implementors | `StaffDeliveryProvider`, `RobotDeliveryProvider` |
| Adapter and third Concrete Implementor | `LegacyDeliveryAdapter` |
| Adaptee | `LegacyHotelGateway` |
| Runtime selection and composition | `RoomServiceApplication` |

`RoomService` holds a `DeliveryProvider`, creates a `DeliveryOrder` through the selected refinement, and delegates once to `deliver`. Standard service rings the doorbell; quiet service requires silent delivery and rejects orders larger than three items before dispatch. These are example hotel policies, not industry standards.

The provider interface accepts a validated order and returns a receipt or throws `DeliveryException`. Its six reasons are `ROOM_NOT_FOUND`, `ITEM_UNAVAILABLE`, `BUSY`, `INVALID_ORDER`, `UNAVAILABLE`, and `PROTOCOL_ERROR`. Invalid client input is rejected before dispatch as `IllegalArgumentException`.

## Genuine incompatibility

The frozen legacy class has no dependency on application interfaces or domain classes:

```java
String[] submit(String sku, byte count, String roomCode, int flags)
        throws LegacyHotelGateway.LinkFault;
```

The adapter converts a typed order into reordered scalar arguments: `WATER` becomes `MIN-H2O`, room `512` becomes `R0512`, the validated quantity becomes a `byte`, and `SILENT` becomes legacy flag `4`. The legacy API returns a string array, status codes, or `null`, and can throw its own checked exception. The native providers already implement the typed interface and use the shared exception.

| Legacy outcome | Public outcome |
| --- | --- |
| `0` and a valid job identifier | `DeliveryReceipt` with an opaque reference and the original order |
| `NROOM` | `ROOM_NOT_FOUND` |
| `STOCK` | `ITEM_UNAVAILABLE` |
| `BUSY` | `BUSY` |
| `ARG` | `INVALID_ORDER` |
| `null` or `LinkFault` | `UNAVAILABLE` |
| Unknown status, malformed response, invalid identifier, or unexpected runtime exception | `PROTOCOL_ERROR` |

The adapter returns only normalized messages. It does not expose vendor exceptions as causes or include vendor diagnostics in messages. A successful job identifier is an opaque reference, not a protocol status. JVM fatal errors such as `OutOfMemoryError` are outside the operational failure contract.

`LegacyHotelGateway` is an assignment-owned fixture of an independently designed API, not an actual third-party product. Its source was frozen before verification; the recorded checksum is in [the legacy API notes](docs/LEGACY_API.md).

## Required complexity module

The selected module is **dynamic implementor selection**. The two-way adapter module is not implemented.

`ServiceLoader` discovers implementations from these resources:

```text
src/main/resources/META-INF/services/hotel.delivery.DeliveryProvider
src/main/resources/META-INF/services/hotel.app.RoomServiceFactory
```

`RoomServiceApplication` indexes discovered objects by their IDs, rejects duplicate IDs, looks up the provider named in the input, and injects it into the selected service. The factories register service refinements; they are small construction hooks, not another required complexity module.

## Open Closed Principle

- **New delivery technology:** add a `DeliveryProvider` implementation with a unique ID and a public no-argument constructor, then register it in the provider resource. Existing Java classes stay unchanged, and both current service refinements can use it.
- **New service policy:** extend `RoomService`, implement a small `RoomServiceFactory`, and register the factory. Existing Java classes stay unchanged, and the new refinement can use every registered provider that supports its order.

Registration is configuration, so a descriptor changes when a new class is added. There is no edit to a Java selection switch. `RoomServiceApplicationTest` demonstrates both extensions using test-only classes: a locker provider and a courtesy service. They are not additional production variants.

## Failure demonstrations

```sh
java -jar target/smart-hotel-1.0.0.jar standard legacy:903 water 1
java -jar target/smart-hotel-1.0.0.jar quiet legacy:905 towels 1
java -jar target/smart-hotel-1.0.0.jar standard robot:512 breakfast 1
java -jar target/smart-hotel-1.0.0.jar quiet staff:512 towels 4
```

Legacy demo rooms `901` through `906` respectively simulate unknown room, unavailable stock, full queue, unavailable service, transport failure, and rejected arguments. Room `901` is also unknown to the native providers. These room numbers are test fixtures rather than real hotel rules.

Exit codes: `0` = accepted or help displayed, `1` = operational/policy failure, `2` = invalid CLI input. The program does not automatically retry a failed dispatch: a transport failure can occur after acceptance, and retrying without idempotency could duplicate an order.

## Verification and submission files

The checked run passed **66 JUnit 5 test invocations**, including parameterized cases. The tests use hand-written stubs and recording providers; Mockito is not required. They check both refinements, every legacy failure form, all six production combinations, routing, both OCP extensions, native restrictions, and CLI exit codes. See [the verification summary](docs/VERIFICATION.md).

- [UML class diagram](docs/uml.svg), [runtime registration diagram](docs/runtime-uml.svg), and [editable PlantUML source](docs/uml.puml)
- [Design rationale](docs/DESIGN_RATIONALE.md), a concise document intended to fit within two pages
- [Oral defense guide](docs/DEFENSE_EN.md)
- [Legacy API and checksum](docs/LEGACY_API.md)
- `src/main/java/` and `src/main/resources/`: application code and registration
- `src/test/java/`: JUnit 5 tests

JUnit reference: [JUnit 5.11.4 User Guide](https://docs.junit.org/5.11.4/user-guide/).

The checked-in SVG diagrams can be regenerated with `python3 tools/render_uml.py`; this optional documentation tool uses only the Python standard library. Python is not needed to build or run the Java project.

GitHub repository: [asemtygelbaeva-byte/assignment_3](https://github.com/asemtygelbaeva-byte/assignment_3).
