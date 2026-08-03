# Ignite Folder Tracking

An Android application to inventory folders using RFID, barcode scanning and the built-in
camera. It can also use Geiger Search to hone in on a specific folder in a room, and pull up
details of a folder including what colour tabbings it should have.

Scans are queued locally and uploaded to an intranet folder-tracking server in the
background, so the app keeps working when the network does not.

**Supported devices:** Android 10 through Android 13 (`minSdk 29`, `targetSdk 34`).

1. [Developing](./docs/development.md)
2. [Building an APK](./docs/build.md)
3. [Hardware Requirements](./docs/hardware.md)
4. [Hatting - English](./docs/hatting.md)
5. [Hatting - Spanish](./docs/hatting.md)

## First run

Open **SETUP** in the bottom bar and enter the server address, username and password. Nothing
that talks to the server will work until these are set; credentials are encrypted with an
AES-GCM key held in the device keystore and never leave app-private storage.
