# Hardware Requirements

Android 10 is the oldest supported release; the app is tested through Android 13.

## Barcode scanning

1. **Zebra TC210K** (and the older TC21/TC26) -- the integrated imager is driven through
   Zebra DataWedge. The app provisions its own DataWedge profile (`INCOMMSETTINGS`) on
   launch, so devices do not need to be staged by hand. Decodes are delivered by broadcast.
2. Any USB or Bluetooth **keyboard-wedge** scanner -- digits followed by Enter are buffered
   and treated as one scan.
3. Any Android 10+ device with a camera.

## RFID

Convergence Systems **CS108** handheld reader, over Bluetooth LE.

Bluetooth permissions differ across the supported range: Android 10/11 use
`BLUETOOTH`/`BLUETOOTH_ADMIN` plus a location grant, Android 12+ use
`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`. The app requests whichever applies at runtime.
