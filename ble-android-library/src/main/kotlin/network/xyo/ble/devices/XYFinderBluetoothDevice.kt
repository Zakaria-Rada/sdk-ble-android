package network.xyo.ble.devices

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import network.xyo.ble.firmware.OtaUpdate
import network.xyo.ble.gatt.XYBluetoothError
import network.xyo.ble.gatt.XYBluetoothResult
import network.xyo.ble.gatt.asyncBle
import network.xyo.ble.scanner.XYScanResult
import network.xyo.core.XYBase
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class XYFinderBluetoothDevice(context: Context, scanResult: XYScanResult, hash: Int) : XYIBeaconBluetoothDevice(context, scanResult, hash), Comparable<XYFinderBluetoothDevice> {

    enum class Family {
        Unknown,
        XY1,
        XY2,
        XY3,
        Mobile,
        Gps,
        Near,
        XY4,
        Webble
    }

    enum class Proximity {
        None,
        OutOfRange,
        VeryFar,
        Far,
        Medium,
        Near,
        VeryNear,
        Touching
    }

    enum class ButtonPress(val state: Int) {
        None(0),
        Single(1),
        Double(2),
        Long(3)
    }

    override fun compareTo(other: XYFinderBluetoothDevice): Int {
        val d1 = distance
        val d2 = other.distance
        if (d1 == null) {
            if (d2 == null) {
                return 0
            }
            return -1
        }
        return when {
            d2 == null -> 1
            d1 == d2 -> 0
            d1 > d2 -> 1
            else -> -1
        }
    }

    override val id: String
        get() {
            return "$prefix:$uuid.${major.toInt()}.${minor.and(0xfff0).or(0x0004).toInt()}"
        }

    internal open val prefix = "xy:finder"

    val family: Family
        get() {
            return when (this@XYFinderBluetoothDevice) {
                is XYMobileBluetoothDevice -> {
                    Family.Mobile
                }
                is XYGpsBluetoothDevice -> {
                    Family.Gps
                }
                is XY4BluetoothDevice -> {
                    Family.XY4
                }
                is XY3BluetoothDevice -> {
                    Family.XY3
                }
                else -> {
                    Family.Unknown
                }
            }
        }

    //the distance is in meters, so these are what we subjectively think are the fuzzy proximity values
    val proximity: Proximity
        get() {

            val distance = distance ?: return Proximity.OutOfRange

            if (distance < 0.0) {
                return Proximity.None
            }

            if (distance < 0.5) {
                return Proximity.Touching
            }

            if (distance < 15) {
                return Proximity.VeryNear
            }

            if (distance < 30) {
                return Proximity.Near
            }

            if (distance < 60) {
                return Proximity.Medium
            }

            if (distance < 120) {
                return Proximity.Far
            }

            return Proximity.VeryFar
        }

    //signal the user to where it is, usually make it beep
    open fun find(): Deferred<XYBluetoothResult<Int>> {
        log.error(UnsupportedOperationException().toString(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    //turn off finding, if supported
    open fun stopFind(): Deferred<XYBluetoothResult<Int>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun lock(): Deferred<XYBluetoothResult<ByteArray>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<ByteArray>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun unlock(): Deferred<XYBluetoothResult<ByteArray>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<ByteArray>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun stayAwake(): Deferred<XYBluetoothResult<Int>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun fallAsleep(): Deferred<XYBluetoothResult<Int>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun restart(): Deferred<XYBluetoothResult<Int>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun batteryLevel(): Deferred<XYBluetoothResult<Int>> {
        log.error(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun updateFirmware(stream: InputStream, listener: OtaUpdate.Listener) {

    }

    open fun updateFirmware(filename: String, listener: OtaUpdate.Listener) {

    }

    open fun cancelUpdateFirmware() {

    }

    open val distance: Double?
        get() {
            val rssi = rssi ?: return null
            val dist: Double
            val ratio: Double = rssi*1.0/power
            dist = if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            }
            else {
                (0.89976)*Math.pow(ratio,7.7095) + 0.111
            }
            return dist
        }

    internal open fun reportButtonPressed(state: ButtonPress) {
        log.info("reportButtonPressed")
        GlobalScope.launch {
            synchronized(listeners) {
                for (listener in listeners) {
                    val xyFinderListener = listener.value as? Listener
                    if (xyFinderListener != null) {
                        log.info("reportButtonPressed: $xyFinderListener")
                        GlobalScope.launch {
                            when (state) {
                                ButtonPress.Single -> xyFinderListener.buttonSinglePressed(this@XYFinderBluetoothDevice)
                                ButtonPress.Double -> xyFinderListener.buttonDoublePressed(this@XYFinderBluetoothDevice)
                                ButtonPress.Long -> xyFinderListener.buttonLongPressed(this@XYFinderBluetoothDevice)
                                else -> {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    open class Listener : XYIBeaconBluetoothDevice.Listener() {
        open fun buttonSinglePressed(device: XYFinderBluetoothDevice) {}

        open fun buttonDoublePressed(device: XYFinderBluetoothDevice) {}

        open fun buttonLongPressed(device: XYFinderBluetoothDevice) {}
    }

    companion object : XYBase() {

        fun enable(enable: Boolean) {
            if (enable) {
                XYIBeaconBluetoothDevice.enable(true)
            }
        }

        fun buttonPressFromInt(index: Int): ButtonPress {
            return when (index) {
                1 -> ButtonPress.Single
                2 -> ButtonPress.Double
                3 -> ButtonPress.Long
                else -> {
                    ButtonPress.None
                }
            }
        }

        var canCreate = false

        internal fun addCreator(uuid: UUID, creator: XYCreator) {
            XYIBeaconBluetoothDevice.uuidToCreator[uuid] = this.creator
            uuidToCreator[uuid] = creator
        }

        internal fun removeCreator(uuid: UUID) {
            uuidToCreator.remove(uuid)
        }

        private val uuidToCreator = HashMap<UUID, XYCreator>()

        internal val creator = object : XYCreator() {
            override fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: ConcurrentHashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {

                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
                if (bytes != null) {
                    val buffer = ByteBuffer.wrap(bytes)
                    buffer.position(2) //skip the type and size

                    // get uuid
                    val high = buffer.long
                    val low = buffer.long
                    val uuidFromScan = UUID(high, low)

                    for ((uuid, creator) in uuidToCreator) {
                        if (uuid == uuidFromScan) {
                            creator.getDevicesFromScanResult(context, scanResult, globalDevices, foundDevices)
                            return
                        }
                    }
                }

                val hash = hashFromScanResult(scanResult)

                if (canCreate && hash != null) {
                    foundDevices[hash] = globalDevices[hash] ?: XYFinderBluetoothDevice(context, scanResult, hash)
                }
            }
        }

        internal fun hashFromScanResult(scanResult: XYScanResult): Int? {
            return XYIBeaconBluetoothDevice.hashFromScanResult(scanResult)
        }

        val compareDistance = kotlin.Comparator<XYFinderBluetoothDevice> { o1, o2 ->
            if (o1 == null || o2 == null) {
                if (o1 != null && o2 == null) return@Comparator -1
                if (o2 != null && o1 == null) return@Comparator 1
                return@Comparator 0
            }
            o1.compareTo(o2)
        }

        fun sortedList(devices: ConcurrentHashMap<Int, XYBluetoothDevice>): List<XYFinderBluetoothDevice> {
            val result = ArrayList<XYFinderBluetoothDevice>()
            for ((_, device) in devices) {
                val deviceToAdd = device as? XYFinderBluetoothDevice
                if (deviceToAdd != null) {
                    result.add(deviceToAdd)
                }
            }
            return result.sortedWith(compareDistance)
        }
    }
}