package network.xyo.ble.devices

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import network.xyo.ble.scanner.XYScanResult
import network.xyo.ble.services.standard.*
import network.xyo.ble.services.xy3.*
import network.xyo.base.XYBase
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@kotlin.ExperimentalUnsignedTypes
open class XY3BluetoothDevice(context: Context, scanResult: XYScanResult, hash: String) : XYFinderBluetoothDevice(context, scanResult, hash) {

    val alertNotification by lazy { AlertNotificationService(this) }
    val batteryService by lazy { BatteryService(this) }
    val currentTimeService by lazy { CurrentTimeService(this) }
    val deviceInformationService by lazy { DeviceInformationService(this) }
    val genericAccessService by lazy { GenericAccessService(this) }
    val genericAttributeService by lazy { GenericAttributeService(this) }
    val linkLossService by lazy { LinkLossService(this) }
    val txPowerService by lazy { TxPowerService(this) }

    val basicConfigService by lazy { BasicConfigService(this) }
    val controlService by lazy { ControlService(this) }
    val csrOtaService by lazy { CsrOtaService(this) }
    val extendedConfigService by lazy { ExtendedConfigService(this) }
    val extendedControlService by lazy { ExtendedControlService(this) }
    val sensorService by lazy { SensorService(this) }

    private var lastButtonPressTime = 0L

    enum class StayAwake(val state: Int) {
        Off(0),
        On(1)
    }

    private val buttonListener = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic?.uuid == controlService.button.uuid) {
                reportButtonPressed(buttonPressFromInt(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)))
            }
        }
    }

    init {
        centralCallback.addListener("xy3", buttonListener)
    }

    //we only allow mac addresses that end in 4 to be updated since those are the connectible ones
    override fun updateBluetoothDevice(device: BluetoothDevice?) {
        if (device?.address?.endsWith("4") == true) {
            this.device = device
        }
        lastAdTime = now
    }

    override val minor: UShort
        get() {
            //we have to mask the low nibble for the power level
            return _minor.and(0xfff0.toUShort()).or(0x0004.toUShort())
        }

    override val prefix = "xy:ibeacon"

    override suspend fun find() = connection {
        return@connection controlService.buzzerSelect.set(2)
    }

    override suspend fun stopFind() = connection {
        return@connection controlService.buzzerSelect.set(-1)
    }

    override suspend fun lock() = connection {
        return@connection basicConfigService.lock.set(DEFAULT_LOCK_CODE)
    }

    override suspend fun unlock() = connection {
        return@connection basicConfigService.unlock.set(DEFAULT_LOCK_CODE)
    }

    override suspend fun stayAwake() = connection {
        return@connection extendedConfigService.registration.set(1)
    }

    override suspend fun fallAsleep() = connection {
        return@connection extendedConfigService.registration.set(0)
    }

    override fun onDetect(scanResult: XYScanResult?) {
        super.onDetect(scanResult)
        if (scanResult != null) {
            if (pressFromScanResult(scanResult)) {
                if (now - lastButtonPressTime > BUTTON_ADVERTISEMENT_LENGTH) {
                    reportButtonPressed(ButtonPress.Single)
                    lastButtonPressTime = now
                }
            }
        }
    }

    /*private fun enableButtonNotifyIfConnected() {
        if (connectionState == ConnectionState.Connected) {
            controlService.button.enableNotify(true)
        }
    }*/

    override fun reportButtonPressed(state: ButtonPress) {
        super.reportButtonPressed(state)
        //every time a notify fires, we have to re-enable it
        //enableButtonNotifyIfConnected()
        reportGlobalButtonPressed(this, state)
    }

    open class Listener : XYFinderBluetoothDevice.Listener()

    companion object : XYBase() {

        private val FAMILY_UUID = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")!!

        //this is how long the xy4 will broadcast ads with power level 8 when a button is pressed once
        private const val BUTTON_ADVERTISEMENT_LENGTH = 30 * 1000

        private val DEFAULT_LOCK_CODE = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

        protected val globalListeners = HashMap<String, Listener>()

        fun enable(enable: Boolean) {
            if (enable) {
                XYFinderBluetoothDevice.enable(true)
                addCreator(FAMILY_UUID, creator)
            } else {
                removeCreator(FAMILY_UUID)
            }
        }

        private fun majorFromScanResult(scanResult: XYScanResult): Int? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.getShort(18).toInt()
            } else {
                null
            }
        }

        private fun minorFromScanResult(scanResult: XYScanResult): Int? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.getShort(20).toInt().and(0xfff0).or(0x0004)
            } else {
                null
            }
        }

        @kotlin.ExperimentalUnsignedTypes
        internal fun pressFromScanResult(scanResult: XYScanResult): Boolean {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                val minor = buffer.getShort(20).toUShort()
                val buttonBit = minor.and(0x0008.toUShort())
                buttonBit == 0x0008.toUShort()
            } else {
                false
            }
        }

        fun addGlobalListener(key: String, listener: Listener) {
            GlobalScope.launch {
                synchronized(globalListeners) {
                    globalListeners.put(key, listener)
                }
            }
        }

        fun removeGlobalListener(key: String) {
            GlobalScope.launch {
                synchronized(globalListeners) {
                    globalListeners.remove(key)
                }
            }
        }

        fun reportGlobalButtonPressed(device: XY3BluetoothDevice, state: ButtonPress) {
            GlobalScope.launch {
                synchronized(globalListeners) {
                    for (listener in globalListeners) {
                        val xyFinderListener = listener.value as? XYFinderBluetoothDevice.Listener
                        if (xyFinderListener != null) {
                            log.info("reportButtonPressed: $xyFinderListener")
                            GlobalScope.launch {
                                when (state) {
                                    ButtonPress.Single -> xyFinderListener.buttonSinglePressed(device)
                                    ButtonPress.Double -> xyFinderListener.buttonDoublePressed(device)
                                    ButtonPress.Long -> xyFinderListener.buttonLongPressed(device)
                                    else -> {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        internal val creator = object : XYCreator() {
            override fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: ConcurrentHashMap<String, XYBluetoothDevice>, foundDevices: HashMap<String, XYBluetoothDevice>) {
                val hash = hashFromScanResult(scanResult)
                foundDevices[hash] = globalDevices[hash]
                        ?: XY3BluetoothDevice(context, scanResult, hash)
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): String {
            val uuid = iBeaconUuidFromScanResult(scanResult)
            val major = majorFromScanResult(scanResult)
            val minor = minorFromScanResult(scanResult)
            return "$uuid:$major:$minor"
        }

    }
}