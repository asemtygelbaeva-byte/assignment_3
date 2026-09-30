# Smart Hotel Room Service and Automation System

**Student:** Assem Tugelbay  
**Group:** SE-2540

## Problem and scope

A smart hotel needs to offer different guest-service policies while delivering amenities through changing technologies. Standard service permits a doorbell. Quiet service requires silent handover and limits requests to three items to reduce disturbance. Staff dispatch, robot delivery, and an older hotel gateway must support these policies through one consistent application contract. The program automates routing and dispatch acceptance; delivery systems are simulated, and a receipt confirms acceptance rather than physical completion.

## Bridge and Adapter in one design

`RoomService` is the Bridge Abstraction. It stores a `DeliveryProvider` reference and delegates a prepared order through that interface. `StandardRoomService` and `QuietRoomService` are the Refined Abstractions. `StaffDeliveryProvider`, `RobotDeliveryProvider`, and `LegacyDeliveryAdapter` are the three production Implementors. Only the last wraps an incompatible class. The six policy/provider combinations work through composition, avoiding separate classes such as `QuietRobotService` and `QuietLegacyService`.

Bridge alone would separate policy from delivery, but it would not make the legacy API satisfy `DeliveryProvider`. Adapter alone would normalize that API, but would not establish an independent hierarchy for service policies. Here, the adapter is one implementor behind the bridge, so both patterns solve parts of the same request flow.

## Why adaptation is necessary

The normal operation is `deliver(DeliveryOrder)`, returning `DeliveryReceipt` or throwing `DeliveryException`. The wrapped operation is `submit(String sku, byte count, String roomCode, int flags)`, returning a string array or null and potentially throwing `LinkFault`. It differs in name, signature, argument order, data types, encoding, and failure mechanism.

For example, a quiet water request for room 512 becomes SKU `MIN-H2O`, a byte quantity, room `R0512`, and notification flag `4`. The adapter translates all four documented error statuses into shared error reasons. Null and checked transport failures become `UNAVAILABLE`; unknown statuses, malformed replies, and unexpected runtime exceptions become `PROTOCOL_ERROR`. Messages and causes contain no vendor diagnostics. `RoomService` never imports the gateway, its exception, or its protocol constants. The gateway is a frozen teaching fixture with its own API, and its source is not changed to implement the application interface.

## Required complexity module

The selected module is **dynamic implementor selection**. An input destination such as `staff:512`, `robot:512`, or `legacy:512` determines the concrete implementor at runtime, including the adapted one. `RoomServiceApplication` uses a registry populated by Java `ServiceLoader` and injects the selected provider into a registered service refinement. The client contains no provider-specific switch or concrete provider construction. No two-way adapter is implemented.

## Extensibility and verification

A new provider requires one class and service registration. A new service policy requires a `RoomService` subclass, a factory, and registration. Neither extension edits existing Java classes. Registration files are configuration points, not changes to policy or delivery logic. JUnit 5 tests demonstrate both extensions through a test-only locker provider and courtesy service. Recording providers verify normal delegation and shared failures for both production refinements; gateway stubs verify parameter conversion and every legacy failure form. Integration tests exercise all six combinations.

## Limitation

The contract models synchronous dispatch acceptance only. It has no persistent queue, delivery tracking, cancellation, or idempotency key. Consequently, an unavailable connection cannot prove whether a job was accepted before the connection failed, and the application does not retry automatically. A production version would need durable job identity and status tracking before safe retries could be introduced.
