package com.leonell.android.composefolderscanner.services

import java.util.UUID

/** CS108 GATT service advertised by the handheld reader; used to filter BLE scan results. */
val SERVICE_UUID: UUID = UUID.fromString("00009800-0000-1000-8000-00805f9b34fb")

const val FolderLoggerWorkName =
   "com.leonell.android.composefolderscanner.services.FolderLoggerWorker"

/** Offset between the reader's dB(uV) RSSI encoding and dBm. */
const val dBuV_dBm: Float = 74F
