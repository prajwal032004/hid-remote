package com.example.bluetooth

import android.bluetooth.BluetoothDevice

enum class DeviceType {
    COMPUTER,
    TV,
    PHONE,
    AUDIO,
    UNKNOWN
}

data class BluetoothDeviceModel(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val deviceType: DeviceType,
    val device: BluetoothDevice
)

sealed interface HidConnectionState {
    data class NotSupported(val reason: String) : HidConnectionState
    data object BluetoothOff : HidConnectionState
    data object PermissionsRequired : HidConnectionState
    data object Initializing : HidConnectionState
    data object Registering : HidConnectionState
    data object ReadyToConnect : HidConnectionState
    data class Connecting(
        val deviceName: String,
        val address: String,
        val attempt: Int = 0
    ) : HidConnectionState
    data class Connected(
        val device: BluetoothDevice,
        val deviceName: String,
        val address: String
    ) : HidConnectionState
    data object Disconnecting : HidConnectionState
    data class Error(val message: String) : HidConnectionState
}
