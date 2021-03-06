package network.xyo.ble.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_link_loss.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import network.xyo.ble.devices.xy.XY2BluetoothDevice
import network.xyo.ble.devices.xy.XY3BluetoothDevice
import network.xyo.ble.devices.xy.XY4BluetoothDevice
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.sample.R
import network.xyo.ble.sample.XYDeviceData


@kotlin.ExperimentalUnsignedTypes
class LinkLossFragment : XYDeviceFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_link_loss, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_link_loss_refresh.setOnClickListener {
            initLinkLossValues()
        }
    }

    override fun onResume() {
        super.onResume()

        if (deviceData?.alertLevel.isNullOrEmpty()) {
            initLinkLossValues()
        } else {
            updateUI()
        }
    }

    private fun updateUI() {
        activity?.runOnUiThread {
            text_alert_level?.text = deviceData?.alertLevel
        }
    }

    private fun initLinkLossValues() {

        when (device) {
            is XY4BluetoothDevice -> {
                val x4 = (device as? XY4BluetoothDevice)
                x4?.let {
                    getXY4Values(x4)
                }
            }
            is XY3BluetoothDevice -> {
                val x3 = (device as? XY3BluetoothDevice)
                x3?.let {
                    getXY3Values(x3)
                }
            }
            is XY2BluetoothDevice -> {
                text_alert_level.text = getString(R.string.not_supported_x2)
            }
            else -> {
                text_alert_level.text = getString(R.string.unknown_device)
            }

        }
    }

    private fun getXY4Values(device: XY4BluetoothDevice) {
        GlobalScope.launch {
            var hasConnectionError = true

            device.connection {
                hasConnectionError = false

                deviceData?.let {
                    it.alertLevel = device.linkLossService.alertLevel.get().format()
                }

                return@connection XYBluetoothResult(true)

            }

            updateUI()
            checkConnectionError(hasConnectionError)
        }
    }

    private fun getXY3Values(device: XY3BluetoothDevice) {
        GlobalScope.launch {
            var hasConnectionError = true

            device.connection {
                hasConnectionError = false

                deviceData?.let {
                    it.alertLevel = device.linkLossService.alertLevel.get().format()
                }

                return@connection XYBluetoothResult(true)

            }

            updateUI()
            checkConnectionError(hasConnectionError)
        }
    }

    companion object {

        fun newInstance() =
                LinkLossFragment()

        fun newInstance (device: XYBluetoothDevice?, deviceData : XYDeviceData?) : LinkLossFragment {
            val frag = LinkLossFragment()
            frag.device = device
            frag.deviceData = deviceData
            return frag
        }
    }

}
