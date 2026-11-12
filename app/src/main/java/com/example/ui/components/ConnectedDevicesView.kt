package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BluetoothDevice
import com.example.data.model.GpioPin
import com.example.data.model.NetworkInterface
import com.example.data.model.UsbDevice
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RaspiRed
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectedDevicesView(
    usbDevices: List<UsbDevice>,
    gpioPins: List<GpioPin>,
    networkInterfaces: List<NetworkInterface>,
    bluetoothDevices: List<BluetoothDevice>,
    onToggleGpio: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("USB (${usbDevices.size})", "GPIO (40)", "Network", "Bluetooth")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = "Connected Devices",
                        tint = TelemetryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected Hardware & Peripherals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                contentColor = RaspiRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = RaspiRed
                    )
                },
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (selectedTab == index) Color.White else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> UsbDeviceList(usbDevices)
                1 -> GpioMatrixView(gpioPins, onToggleGpio)
                2 -> NetworkInterfacesView(networkInterfaces)
                3 -> BluetoothDeviceList(bluetoothDevices)
            }
        }
    }
}

@Composable
private fun UsbDeviceList(devices: List<UsbDevice>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (devices.isEmpty()) {
            Text(
                text = "No USB devices detected",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        } else {
            devices.forEach { dev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (dev.isStorage) Icons.Default.Memory else Icons.Default.Usb,
                                contentDescription = dev.name,
                                tint = if (dev.isStorage) EmeraldGreen else TelemetryCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = dev.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${dev.bus} | VID:${dev.vendorId} PID:${dev.productId} | ${dev.speed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GpioMatrixView(pins: List<GpioPin>, onTogglePin: (Int) -> Unit) {
    Column {
        Text(
            text = "40-Pin Header Matrix (Tap GPIO pin to toggle simulation)",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Group into physical pin pairs: (Pin 1, Pin 2), (Pin 3, Pin 4), etc.
        val pinPairs = remember(pins) {
            pins.chunked(2)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            pinPairs.take(10).forEach { pair ->
                val left = pair.getOrNull(0)
                val right = pair.getOrNull(1)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (left != null) {
                        GpioPinBadge(left, isLeft = true, onToggle = { onTogglePin(left.physicalPin) })
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Central pin header separator
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(14.dp)
                            .background(DarkBorder, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.Yellow)
                        )
                    }

                    if (right != null) {
                        GpioPinBadge(right, isLeft = false, onToggle = { onTogglePin(right.physicalPin) })
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GpioPinBadge(pin: GpioPin, isLeft: Boolean, onToggle: () -> Unit) {
    val isInteractive = pin.bcm != null && !pin.isPowerOrGround
    val stateColor = when {
        pin.isPowerOrGround && pin.name.contains("5V") -> RaspiRed
        pin.isPowerOrGround && pin.name.contains("3V3") -> Color(0xFFFFB300)
        pin.isPowerOrGround && pin.name.contains("GND") -> Color.Gray
        pin.isHigh -> EmeraldGreen
        else -> TextMuted
    }

    Row(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = isInteractive) { onToggle() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = if (isLeft) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLeft) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isLeft) Alignment.End else Alignment.Start) {
            Text(
                text = pin.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = if (pin.isPowerOrGround) stateColor else TextPrimary
            )
            Text(
                text = "Pin ${pin.physicalPin} ${if (isInteractive) (if (pin.isHigh) "[HIGH]" else "[LOW]") else ""}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = stateColor
            )
        }

        if (isLeft) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
        }
    }
}

@Composable
private fun NetworkInterfacesView(interfaces: List<NetworkInterface>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        interfaces.forEach { net ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (net.type == "Wi-Fi") Icons.Default.Wifi else Icons.Default.Sensors,
                            contentDescription = net.name,
                            tint = if (net.isUp) EmeraldGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = net.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = net.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = "IP: ${net.ipAddress}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = TelemetryCyan
                        )
                        Text(
                            text = "MAC: ${net.macAddress}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (net.isUp) EmeraldGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (net.isUp) "UP" else "DOWN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (net.isUp) EmeraldGreen else Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "↓ %.1f KB/s".format(net.rxKbps),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        color = EmeraldGreen
                    )
                    Text(
                        text = "↑ %.1f KB/s".format(net.txKbps),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        color = TelemetryCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothDeviceList(devices: List<BluetoothDevice>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (devices.isEmpty()) {
            Text(text = "No Bluetooth devices paired", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            devices.forEach { bt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = bt.name,
                            tint = TelemetryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = bt.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                            Text(text = "${bt.address} • ${bt.deviceType}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Text(
                        text = if (bt.isConnected) "Connected" else "Paired",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (bt.isConnected) EmeraldGreen else TextMuted
                        )
                    )
                }
            }
        }
    }
}
