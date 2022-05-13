package com.nuvoton.nuisptool_android.ISPTool

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.Util.Log
import com.nuvoton.nuisptool_android.R
import java.util.HashMap
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nuvoton.nuisptool_android.ISPActivity
import com.nuvoton.nuisptool_android.MainActivity
import androidx.core.content.ContextCompat.startActivity
import com.nuvoton.nuisptool_android.Util.DialogTool


object OTGManager {

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    /**
     * 初始化
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun init(context: Context) {
        USBManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        _pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private var TAG = "OTGManager"
    public lateinit var USBManager: UsbManager
    private lateinit var _pendingIntent: PendingIntent
    private lateinit var _USBDevice: UsbDevice
    private var _DeviceListener: ((UsbDevice) -> Unit)? = null
    private var _isOnlineListener: ((Boolean) -> Unit)? = null
    private var _isRegisterReceiver = false

    fun start(context: Context) {
        if(_isRegisterReceiver == true){
            return
        }
        //監聽事件廣播註冊
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
//        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        context.registerReceiver(broadcastReceiver, filter)
        _isRegisterReceiver = true
    }

    fun get_USBDevice(): UsbDevice {
        return _USBDevice
    }

    /**
     * 接收系統事件廣播
     * 處理權限問題
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == ACTION_USB_PERMISSION) {
                Log.i(TAG, "Action ---- ACTION_USB_PERMISSION")
                //取得權限後
                Log.i(TAG, "deviceName:" + _USBDevice.deviceName)
                Log.i(TAG, "productId:" + _USBDevice.productId)
                Log.i(TAG, "deviceId:" + _USBDevice.deviceId)
                Log.i(TAG, "deviceProtocol:" + _USBDevice.deviceProtocol)
                Log.i(TAG, "deviceClass:" + _USBDevice.deviceClass)
                Log.i(TAG, "vendorId:" + _USBDevice.vendorId)
//                Log.i(TAG, "getInterface 5 :" + _USBDevice.getInterface(5))

                _DeviceListener.let {
                    if (it != null) {
                        it(_USBDevice)
                    }
                }

            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Log.i(TAG, "Action ---- ACTION_USB_DEVICE_ATTACHED")

                startUsbConnecting(context!!)

                if (_isOnlineListener != null) {
                    _isOnlineListener?.invoke(true)
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.i(TAG, "Action ---- ACTION_USB_DEVICE_DETACHED")

                if (_isOnlineListener != null) {
                    _isOnlineListener?.invoke(false)
                }

            }
        }
    }

    fun startUsbConnecting(context: Context) {
        Log.i(TAG, "startUsbConnecting")
        val usbDevices: HashMap<String, UsbDevice>? = USBManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            var keep = true
            usbDevices.forEach { entry ->
                _USBDevice = entry.value
                val deviceVendorId: Int = _USBDevice.vendorId
                Log.i(TAG, "vendorId: " + deviceVendorId)

                USBManager.requestPermission(_USBDevice, _pendingIntent)

                if (!keep) {
                    return
                }
            }
        } else {

            DialogTool.showAlertDialog(
                context,
                "No USB Device Connected",
                true,
                false,
                callback = { isOk, isNo ->}
            )

            Log.i(TAG, "no usb device connected")

        }
    }

    fun setGetUsbDeviceListener(callbacks: (UsbDevice) -> Unit) {
        _DeviceListener = callbacks
    }

    fun setIsOnlineListener(callbacks: (Boolean) -> Unit) {
        _isOnlineListener = callbacks
    }
}