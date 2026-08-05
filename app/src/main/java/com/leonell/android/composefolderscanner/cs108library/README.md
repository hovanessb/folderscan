# cs108-Library (Kotlin port)

A Kotlin port of `cslibrary4a` from the [CS108 Android Java Bluetooth Demo App and
SDK](https://github.com/cslrfid/CS108-Android-Java-App), adapted for use with Jetpack
Compose and coroutines.

## Differences from the upstream Java library

The port tracks upstream's CS108 (R2000) behaviour. It does **not** include the newer
`Cs710Library4A` / `RfidReaderChipE710` support upstream added for the CS710S reader, and it
keeps the original monolithic `Cs108Connector` rather than upstream's
`CsReaderConnector` / `RfidConnector` / `BarcodeConnector` decomposition.

Fixes applied on top of the original port:

- `isRfidFailure` / `isBarcodeFailure` were ported as `val x = ...`, which evaluates once
  during construction. Both were therefore permanently `false` and no caller could detect a
  module fault. They are now live getters, matching upstream's methods.
- `invAlgo` was a plain field, so assigning it never reached the reader and the inventory
  algorithm silently stayed on whatever the chip already had. It now delegates to
  `setInvAlgo` / `invAlgo1`.
- `population` was likewise a plain field, so assigning it never reprogrammed the Q value.
  It now routes through `setPopulation`.
- `BluetoothConnector` was only constructed when a log `TextView` was supplied, so passing
  `null` (the correct thing to do from a ViewModel) left the whole BLE stack null. The log
  view is now optional throughout.
- Deprecated no-argument `Handler()` constructors now bind explicitly to the main looper.

## Reading tags efficiently

`onRFIDEvent()` returns a single packet per call and re-takes the reader guard each time.
Draining a burst of N tags that way costs N guarded round-trips and pushes callers into a
spin loop. Two additions make bulk reads cheap:

```kotlin
// Take the whole backlog in one guarded pass.
val packets: List<Rx000pkgData> = library.drainRfidEvents()

// Discard packets left over from a previous operation before starting a new one.
library.flushRfidEvents()
```

See `ConvergenceHandgunViewModel` for the reader loop built on these: it drains in bulk,
parses and de-duplicates off the main thread, backs off when idle, and publishes batches of
`ScanModel` on a `SharedFlow` instead of one UI state update per tag.

## Usage sketch

```kotlin
// Start the library once, with the application context.
convergenceModel.startBleLibrary(applicationContext)

// Connect to a device discovered by DeviceViewModel's BLE scan.
convergenceModel.setCurrentConnection(device)

// Collect tag batches from a LaunchedEffect -- never during composition.
LaunchedEffect(Unit) {
   convergenceModel.tagReads.collect { reads ->
      // `reads` is already de-duplicated within the batch.
   }
}
```

Pulling the reader's trigger starts and stops the inventory automatically, through the
library's notification listener.

## Licence

See `LICENSE` in this directory for the upstream terms.
