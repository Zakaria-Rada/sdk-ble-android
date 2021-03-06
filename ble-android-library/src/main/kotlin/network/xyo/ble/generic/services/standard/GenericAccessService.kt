package network.xyo.ble.generic.services.standard

import java.util.UUID
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.services.ByteCharacteristic
import network.xyo.ble.generic.services.Service

enum class GenericAccessServiceCharacteristics(val uuid: UUID) {
    DeviceName(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")),
    Appearance(UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")),
    PrivacyFlag(UUID.fromString("00002a02-0000-1000-8000-00805f9b34fb")),
    ReconnectionAddress(UUID.fromString("00002a03-0000-1000-8000-00805f9b34fb")),
    PeripheralPreferredConnectionParameters(UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"))
}

class GenericAccessService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return TxPowerService.uuid
        }

    val deviceName = ByteCharacteristic(this, GenericAccessServiceCharacteristics.DeviceName.uuid, "Device Name")
    val appearance = ByteCharacteristic(this, GenericAccessServiceCharacteristics.Appearance.uuid, "Appearance")
    val privacyFlag = ByteCharacteristic(this, GenericAccessServiceCharacteristics.PrivacyFlag.uuid, "Privacy Flag")
    val reconnectionAddress = ByteCharacteristic(this, GenericAccessServiceCharacteristics.ReconnectionAddress.uuid, "Reconnection Address")
    val peripheralPreferredConnectionParameters = ByteCharacteristic(this, GenericAccessServiceCharacteristics.PeripheralPreferredConnectionParameters.uuid, "Peripheral Preferred Connection Parameters")

    companion object {
        val uuid: UUID = UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")
    }
}
