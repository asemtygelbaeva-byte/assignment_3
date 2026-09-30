# Verification results

Verified on 30 September 2026 using Maven 3.9.11. The project compiled and passed the full test suite on Java 24.0.2 with `--release 17`, and independently on Java 17.0.14. Both runs reported 66 tests, zero failures, zero errors, and zero skipped tests.

The checked build used `mvn -o clean verify` because the pinned plugins and dependencies were already cached. For a normal first build, use `mvn clean verify`; offline mode is not required by the project.

| JUnit test class | Executed cases | Main behavior checked |
| --- | ---: | --- |
| `RoomServiceTest` | 7 | Delegation and common failures for both refinements; quiet policy limits |
| `LegacyDeliveryAdapterTest` | 23 | All input conversions, all documented failure statuses, null, checked and unchecked failures, malformed responses |
| `RoomServiceApplicationTest` | 25 | All six combinations, real legacy failures, registration, both extension axes, invalid destinations |
| `NativeProvidersAndCliTest` | 11 | Native capability limits, input validation, CLI output and exit codes |
| **Total** | **66** | **All passed** |

Recording providers and gateway stubs are hand-written test doubles. The test-only locker provider and courtesy service demonstrate extensions without modifications to production classes. Production registration remains exactly three providers and two service refinements.

The Java 17 check required a valid UTF-8 locale because the workspace directory contains Cyrillic characters. On macOS, `LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 mvn clean verify` avoids path decoding problems if the shell is configured with an unsupported locale. This is an environment issue; the ordinary build command works under a correctly configured locale.

The executable JAR was also exercised independently of JUnit. Captured success and failure output is in `sample-output.txt`. The legacy source checksum was compared with the frozen value. Both SVG class diagrams were rasterized and visually inspected, and the relationships were compared against the Java code.
