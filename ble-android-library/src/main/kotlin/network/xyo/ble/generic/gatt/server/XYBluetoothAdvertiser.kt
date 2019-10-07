package network.xyo.ble.generic.gatt.server

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import network.xyo.ble.generic.XYBluetoothBase
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.gatt.peripheral.asyncBle

open class XYBluetoothAdvertiser(context: Context) : XYBluetoothBase(context) {
    var advertisingData: AdvertiseData? = null
    private var advertisingResponse: AdvertiseData? = null

    protected val listeners = HashMap<String, AdvertiseCallback>()
    private val bleAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    private var advertisingMode: Int? = null
    private var advertisingTxLever: Int? = null
    private var connectible: Boolean? = null
    private var timeout: Int? = null

    val isMultiAdvertisementSupported: Boolean
        get() = bluetoothAdapter?.isMultipleAdvertisementSupported ?: false

    fun addListener(key: String, listener: AdvertiseCallback) {
        listeners[key] = listener
    }

    protected fun removeListener(key: String) {
        listeners.remove(key)
    }

    open suspend fun startAdvertising() = asyncBle {
        if (bleAdvertiser != null) {

            if (!isMultiAdvertisementSupported && advertisingResponse != null) {
                return@asyncBle XYBluetoothResult(null, XYBluetoothResult.ErrorCode.AdvertisingScanResponseNotSupported)
            }

            val startCode = suspendCoroutine<Int> { cont ->
                addListener("startAdvertising", object : AdvertiseCallback() {
                    override fun onStartFailure(errorCode: Int) {
                        removeListener("startAdvertising")
                        cont.resume(errorCode)
                    }

                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                        removeListener("startAdvertising")
                        cont.resume(0)
                    }
                })

                if (advertisingResponse == null) {
                    bleAdvertiser.startAdvertising(buildAdvertisingSettings(), advertisingData, primaryCallback)
                } else {
                    bleAdvertiser.startAdvertising(buildAdvertisingSettings(), advertisingData, advertisingResponse, primaryCallback)
                }
            }

            when (startCode) {
                0 -> return@asyncBle XYBluetoothResult(startCode)
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> return@asyncBle XYBluetoothResult(startCode, XYBluetoothResult.ErrorCode.AdvertisingAlreadyStarted)
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> return@asyncBle XYBluetoothResult(startCode, XYBluetoothResult.ErrorCode.AdvertisingDataTooLarge)
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> return@asyncBle XYBluetoothResult(startCode, XYBluetoothResult.ErrorCode.AdvertisingNotSupported)
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> return@asyncBle XYBluetoothResult(startCode, XYBluetoothResult.ErrorCode.AdvertisingInternalError)
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> return@asyncBle XYBluetoothResult(startCode, XYBluetoothResult.ErrorCode.TooManyAdvertisers)
            }
        }

        return@asyncBle XYBluetoothResult<Int>(XYBluetoothResult.ErrorCode.NoAdvertiser)
    }

    fun stopAdvertising() {
        bleAdvertiser?.stopAdvertising(primaryCallback)
    }

    open fun changeContactable(newConnectible: Boolean) {
        connectible = newConnectible
    }

    open fun changeTimeout(newTimeout: Int?) {
        timeout = newTimeout
    }

    open fun changeAdvertisingMode(newAdvertisingMode: Int?): Boolean {
        if (newAdvertisingMode != null) {
            if (!(newAdvertisingMode == AdvertiseSettings.ADVERTISE_MODE_BALANCED ||
                    newAdvertisingMode == AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY ||
                    newAdvertisingMode == AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)) {
                return false
            }
        }
        advertisingMode = newAdvertisingMode

        return true
    }

    open fun changeAdvertisingTxLevel(newAdvertisingTxLevel: Int?): Boolean {
        if (newAdvertisingTxLevel != null) {
            if (!(newAdvertisingTxLevel == AdvertiseSettings.ADVERTISE_TX_POWER_HIGH ||
                    newAdvertisingTxLevel == AdvertiseSettings.ADVERTISE_TX_POWER_LOW ||
                    newAdvertisingTxLevel == AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM ||
                    newAdvertisingTxLevel == AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)) {
                return false
            }
        }

        advertisingTxLever = newAdvertisingTxLevel

        return true
    }

    protected open fun buildAdvertisingSettings(): AdvertiseSettings {
        val builder = AdvertiseSettings.Builder()

        advertisingMode?.let { builder.setAdvertiseMode(it) }
        connectible?.let { builder.setConnectable(it) }
        timeout?.let { builder.setTimeout(it) }
        advertisingTxLever?.let { builder.setTxPowerLevel(it) }

        return builder.build()
    }

    private val primaryCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            for ((_, listener) in listeners) {
                listener.onStartFailure(errorCode)
            }
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)

            for ((_, listener) in listeners) {
                listener.onStartSuccess(settingsInEffect)
            }
        }
    }
}
