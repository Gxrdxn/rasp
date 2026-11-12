package com.example.data.model

data class UsbDevice(
    val id: String,
    val name: String,
    val bus: String,
    val vendorId: String,
    val productId: String,
    val speed: String,
    val isStorage: Boolean = false,
    val isOnline: Boolean = true
)

enum class GpioMode {
    INPUT, OUTPUT, ALT0, PWM
}

data class GpioPin(
    val physicalPin: Int,
    val bcm: Int?,
    val name: String,
    val mode: GpioMode = GpioMode.OUTPUT,
    val isHigh: Boolean = false,
    val isPowerOrGround: Boolean = false,
    val isPwm: Boolean = false
)

data class NetworkInterface(
    val name: String,
    val ipAddress: String,
    val macAddress: String,
    val isUp: Boolean,
    val type: String, // "Ethernet" or "Wi-Fi"
    val rxKbps: Double,
    val txKbps: Double,
    val signalDbm: Int? = null
)

data class BluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val deviceType: String,
    val rssi: Int? = null
)
