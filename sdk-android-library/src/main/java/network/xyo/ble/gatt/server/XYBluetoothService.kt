package network.xyo.ble.gatt.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.*
import kotlin.collections.HashMap

open class XYBluetoothService (uuid: UUID, serviceType : Int) : BluetoothGattService(uuid, serviceType) {
    private val listeners = HashMap<String, XYBluetoothServiceListener>()
    private val characteristics = HashMap<UUID, BluetoothGattCharacteristic>()

    fun addListener (key : String, listener : XYBluetoothServiceListener) {
        listeners[key] = listener
    }

    fun removeListener(key: String) {
        listeners.remove(key)
    }

    override fun addCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        characteristics[characteristic.uuid] = characteristic

        if (characteristic is XYBluetoothCharacteristic) {
            characteristic.addListener(this.toString(), object : XYBluetoothCharacteristic.XYBluetoothCharacteristicListener {
                override fun onChange() {
                    for ((_, listener) in listeners) {
                        listener.onCharacteristicChange(characteristic)
                    }
                }
            })
        }

        return super.addCharacteristic(characteristic)
    }

    open fun onBluetoothCharacteristicWrite (characteristic: BluetoothGattCharacteristic, device: BluetoothDevice?, value : ByteArray?) : Boolean? {
        val characteristicHandler = characteristics[characteristic.uuid]
        if (characteristicHandler is XYBluetoothCharacteristic) {
            return characteristicHandler.onWriteRequest(value, device)
        }
        return null
    }

    open fun onBluetoothCharacteristicReadRequest (characteristic: BluetoothGattCharacteristic, device: BluetoothDevice?, offset : Int) : XYBluetoothGattServer.XYReadRequest? {
        val characteristicHandler = characteristics[characteristic.uuid]
        if (characteristicHandler is XYBluetoothCharacteristic) {
            return characteristicHandler.onReadRequest(device, offset)
        }
        return null
    }

    interface XYBluetoothServiceListener {
        fun onCharacteristicChange(characteristic: BluetoothGattCharacteristic)
    }
}