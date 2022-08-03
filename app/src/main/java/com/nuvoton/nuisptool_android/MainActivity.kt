package com.nuvoton.nuisptool_android


import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.ISPTool.*

import com.nuvoton.nuisptool_android.Util.Log
import com.nuvoton.nuisptool_android.Util.PermissionManager
import java.util.ArrayList
import android.widget.CompoundButton
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.updateListItems
import com.nuvoton.nuisptool_android.Bluetooth.BluetoothLeCmdManager
import com.nuvoton.nuisptool_android.Bluetooth.BluetoothLeData
import com.nuvoton.nuisptool_android.Bluetooth.BluetoothLeDataManager
import com.nuvoton.nuisptool_android.Util.DialogTool
import com.nuvoton.nuisptool_android.WiFi.SocketCmdManager
import com.nuvoton.nuisptool_android.WiFi.SocketManager
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class MainActivity : AppCompatActivity() {

    private var TAG = "MainActivity"

    private lateinit var _findDeviceButton: Button
    private lateinit var _connectDeviceButton: Button
    private lateinit var _mainMessageText: TextView

    //    private lateinit var _sp_interFace: Spinner
    private var _deviceID: String? = null
    private var _USBDevice: UsbDevice? = null
    private lateinit var _textVersion: TextView

    private var _radioButtonList: Array<RadioButton> = arrayOf()
    private lateinit var _radioButton_USB: RadioButton
    private lateinit var _radioButton_UART: RadioButton
    private lateinit var _radioButton_SPI: RadioButton
    private lateinit var _radioButton_I2C: RadioButton
    private lateinit var _radioButton_RS485: RadioButton
    private lateinit var _radioButton_CAN: RadioButton
    private lateinit var _radioButton_WiFi: RadioButton
    private lateinit var _radioButton_BLE: RadioButton

    //BLE
    private val _bdm = BluetoothLeDataManager.getInstance()
    //SCAN
    private var _scanResultArray: ArrayList<ScanResult> = ArrayList<ScanResult>()
    private var _scanResultDeviceArray: ArrayList<String> = ArrayList<String>()
    private lateinit var _alertDialog: MaterialDialog

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getSupportActionBar()!!.setTitle("Nuvoton Android ISP Tool")

        _findDeviceButton = findViewById<View>(R.id.button) as Button
        _findDeviceButton.setOnClickListener(onClickButton)
        _connectDeviceButton = findViewById<View>(R.id.connect) as Button
        _connectDeviceButton.setOnClickListener(onConnectClickButton)
        _connectDeviceButton.isEnabled = false
        _connectDeviceButton.setBackgroundColor(Color.GRAY)
        _mainMessageText = findViewById<View>(R.id.main_message) as TextView
        _textVersion = findViewById<View>(R.id.textVersion) as TextView
        _textVersion.text = "Version " + BuildConfig.VERSION_NAME

        _radioButton_USB = findViewById<View>(R.id.RadioButton_USB) as RadioButton
        _radioButton_USB.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_UART = findViewById<View>(R.id.RadioButton_UART) as RadioButton
        _radioButton_UART.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_SPI = findViewById<View>(R.id.RadioButton_SPI) as RadioButton
        _radioButton_SPI.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_I2C = findViewById<View>(R.id.RadioButton_I2C) as RadioButton
        _radioButton_I2C.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_RS485 = findViewById<View>(R.id.RadioButton_RS485) as RadioButton
        _radioButton_RS485.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_CAN = findViewById<View>(R.id.RadioButton_CAN) as RadioButton
        _radioButton_CAN.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_WiFi = findViewById<View>(R.id.RadioButton_WiFi) as RadioButton
        _radioButton_WiFi.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButton_BLE = findViewById<View>(R.id.RadioButton_BLE) as RadioButton
        _radioButton_BLE.setOnCheckedChangeListener(CompoundButtonOnCheckedChangeListener)
        _radioButtonList = arrayOf(_radioButton_USB,_radioButton_UART,_radioButton_SPI,_radioButton_I2C,_radioButton_RS485,_radioButton_CAN,_radioButton_WiFi,_radioButton_BLE)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)

        }

        if(BluetoothLeCmdManager.BLE_DATA != null && BluetoothLeCmdManager.BLE_DATA!!.isConnect){
            BluetoothLeCmdManager.BLE_DATA!!.setDisClose()
        }

        this.initUSBHost()
        this.initChipInfoData()
        ISPManager.interfaceType = NulinkInterfaceType.USB
        BluetoothLeDataManager.context = this
        setPermission()
    }

    override fun onResume() {
        super.onResume()

        //註冊離線監聽
        OTGManager.setIsOnlineListener {
            if (it == false) { //裝置離線
                backToTop()
            }
        }

        SocketManager.setIsOnlineListener {
            if (it == false) { //裝置離線
                backToTop()
            }
        }

    }

    private fun backToTop(){
        runOnUiThread {

            DialogTool.showAlertDialog(
                this,
                "Device is Disconnection",
                true,
                false,
                callback = { isOk, isNo ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    this.startActivity(intent)
                })

        }
    }

    fun initChipInfoData() {
        //讀取ＪＳＯＮ檔產生列表
        val infoJson = FileManager.loadChipInfoFile(this)
        val pdidJson = FileManager.loadChipPdidFile(this)
        FileManager.saveFile(this, infoJson, pdidJson)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun initUSBHost() {
        //todo OTGManager USB HOST init
        OTGManager.init(this)//初始化
        OTGManager.start(this)//監聽
        OTGManager.setGetUsbDeviceListener {
            //取得裝置 USB device
            _USBDevice = it

            //todo 判斷是否為可連接 PID 16128 = ISP_USB 16144 = Nulink2 , 20994 = Nulink2_me
            if(ISPManager.interfaceType == NulinkInterfaceType.USB && _USBDevice!!.productId != 16128){
                this.runOnUiThread {
                    DialogTool.showAlertDialog(this,"IS Not ISP Device",true,false,null)
                }
                return@setGetUsbDeviceListener
            }else if (ISPManager.interfaceType != NulinkInterfaceType.USB && !(_USBDevice!!.productId == 16144 || _USBDevice!!.productId == 20993 ||  _USBDevice!!.productId == 20995)) {
                this.runOnUiThread {
                    DialogTool.showAlertDialog(this,"IS Not NuLink2 Pro Device",true,false,null)
                }
                return@setGetUsbDeviceListener
            }

            this.doConnect()
        }
    }

    private fun doConnect(){

        //等待目標版進入 ISP MODE
        if(ISPManager.interfaceType != NulinkInterfaceType.USB){
            this.runOnUiThread {
                DialogTool.showProgressDialog(this,"Search Device","Please reset to ISP mode",false)
            }
        }

        //如果是CAN
        if(ISPManager.interfaceType == NulinkInterfaceType.CAN){
            ISPManager.sendCMD_CAN_GET_DEVICE( callback = {
                runOnUiThread {
                    DialogTool.dismissDialog()

                    if (it == null) {
                        Log.i(TAG, "sendCMD_CONNECT ---- is Not CAN InterFace")
                        runOnUiThread {
                            DialogTool.showAlertDialog(
                                this,
                                "is Not CAN InterFace",
                                true,
                                false,
                                null
                            )
                        }
                        return@runOnUiThread
                    }
                    _deviceID = ISPCommandTool.toCAN_DeviceID(it)
                    Log.i(TAG, "toCAN_DeviceID ---- Device:$_deviceID")
                    if (FileManager.getChipInfoByPDID(_deviceID!!) == null) {
                        _mainMessageText.setText("Find Device: unknown Device")
                        _connectDeviceButton.isEnabled = true
                        _connectDeviceButton.setBackgroundColor(Color.RED)
                        return@runOnUiThread
                    }
                    _mainMessageText.setText("Find Device: " + FileManager.CHIP_DATA.chipPdid.name)
                    _connectDeviceButton.isEnabled = true
                    _connectDeviceButton.setBackgroundColor(Color.RED)
                }
            })
            return
        }

        //送出指令
        ISPManager.sendCMD_CONNECT( callback = { byteArray, isChecksum, isTimeout ->

            DialogTool.dismissDialog()

            if (isTimeout == true) {
                Log.i(TAG, "sendCMD_CONNECT ---- Search Device is time out.")
                runOnUiThread {
                    DialogTool.showAlertDialog(this,"Search Device is time out.",true,false,null)

                }
                return@sendCMD_CONNECT
            }

            if (isChecksum == false) {
                Log.i(TAG, "sendCMD_CONNECT ---- is Not USB InterFace")
                runOnUiThread {
                    DialogTool.showAlertDialog(this,"is Not USB InterFace",true,false,null)
                }
                return@sendCMD_CONNECT
            }

//            ISPManager.sendCMD_READ_CONFIG(_USBDevice!!, callback = {byteArray ->
//                if (isChecksum == false || byteArray == null) {
//                    Log.i(TAG, "sendCMD_READ_CONFIG ---- fail")
//                    return@sendCMD_READ_CONFIG
//                }
//            })

            ISPManager.sendCMD_GET_DEVICEID( callback = { byteArray, isChecksum ->
                runOnUiThread {
                    if (isChecksum == false || byteArray == null) {
                        Log.i(TAG, "sendCMD_GET_DEVICEID ---- fail")
                        return@runOnUiThread
                    }
                    _deviceID = ISPCommandTool.toDeviceID(byteArray)
                    Log.i(TAG, "sendCMD_GET_DEVICEID ---- Device:$_deviceID")

                    if (FileManager.getChipInfoByPDID(_deviceID!!) == null) {
                        _mainMessageText.setText("Find Device: unknown Device")
                        _connectDeviceButton.isEnabled = true
                        _connectDeviceButton.setBackgroundColor(Color.RED)
                        return@runOnUiThread
                    }
                    _mainMessageText.setText("Find Device: " + FileManager.CHIP_DATA.chipPdid.name)
                    _connectDeviceButton.isEnabled = true
                    _connectDeviceButton.setBackgroundColor(Color.RED)
                }
            })
        })
    }

    /**
     * Read Listener callback
     */
    private var ReadListenerCallback: (ByteArray) -> Unit = {

    }

    /**
     * on CompoundButtonOnCheckedChangeListener
     */
    private val CompoundButtonOnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { compoundButton, b ->
            if(b == false) return@OnCheckedChangeListener
            when (compoundButton) {
                _radioButton_USB -> {
                    Log.i(TAG, "onClickButton:_radioButton_USB")
                    ISPManager.interfaceType = NulinkInterfaceType.USB
                }
                _radioButton_UART -> {
                    Log.i(TAG, "onClickButton:_radioButton_UART")
                    ISPManager.interfaceType = NulinkInterfaceType.UART
                }
                _radioButton_SPI -> {
                    Log.i(TAG, "onClickButton:_radioButton_SPI")
                    ISPManager.interfaceType = NulinkInterfaceType.SPI
                }
                _radioButton_I2C -> {
                    Log.i(TAG, "onClickButton:_radioButton_I2C")
                    ISPManager.interfaceType = NulinkInterfaceType.I2C
                }
                _radioButton_RS485 -> {
                    Log.i(TAG, "onClickButton:_radioButton_RS485")
                    ISPManager.interfaceType = NulinkInterfaceType.RS485
                }
                _radioButton_CAN -> {
                    Log.i(TAG, "onClickButton:_radioButton_CAN")
                    ISPManager.interfaceType = NulinkInterfaceType.CAN
                }
                _radioButton_WiFi -> {
                    Log.i(TAG, "onClickButton:_radioButton_WiFi")
                    ISPManager.interfaceType = NulinkInterfaceType.WiFi
                }
                _radioButton_BLE -> {
                    Log.i(TAG, "onClickButton:_radioButton_BLE")
                    ISPManager.interfaceType = NulinkInterfaceType.BLE
                }
            }

            for(rb in _radioButtonList){
                if(compoundButton != rb){
                    rb.isChecked = false
                }
            }
        }


    /**
     * on Click Button
     */
    private val onClickButton = View.OnClickListener {
        Log.i(TAG, "onClickButton")

        if(BluetoothLeCmdManager.BLE_DATA != null && BluetoothLeCmdManager.BLE_DATA!!.isConnect){
            BluetoothLeCmdManager.BLE_DATA!!.setDisClose()
            _findDeviceButton.setText("Find Device")
            _mainMessageText.setText("BLE Status: DisConnected")
            _connectDeviceButton.isEnabled = false
            _connectDeviceButton.setBackgroundColor(Color.GRAY)
            return@OnClickListener
        }

        if(ISPManager.interfaceType == NulinkInterfaceType.BLE){
            if(_bdm.isBluetoothEnabled(this) == false){
                Toast.makeText(this, "R.string.ble_not_supported", Toast.LENGTH_SHORT).show();
                return@OnClickListener
            }
            if(_bdm.isGPSEnabled(this) == false){
                Toast.makeText(this, "R.string.GPS_not_supported", Toast.LENGTH_SHORT).show();
                return@OnClickListener
            }
            ScanBleDevice()
            return@OnClickListener
        }

        if(ISPManager.interfaceType == NulinkInterfaceType.WiFi){

            SocketManager.funTCPClientClose()

            this.runOnUiThread {
                DialogTool.showProgressDialog(this,"","Connecting Wifi Module...",false)
            }

            thread {
                SocketManager.funTCPClientConnect { isConnect, exception ->

                    if(isConnect != true){
                        when (exception) {
                            is SocketTimeoutException -> {
                                this.runOnUiThread {
                                    DialogTool.showAlertDialog(this,"Connect SocketTimeout.",true,false,null)
                                }
                                Log.e("SocketManager","连接超时");
                            }
                            is NoRouteToHostException -> {
                                this.runOnUiThread {
                                    DialogTool.showAlertDialog(this,"Connect NoRouteToHost.",true,false,null)
                                }
                                Log.e("SocketManager","该地址不存在");
                            }
                            is ConnectException -> {
                                this.runOnUiThread {
                                    DialogTool.showAlertDialog(this,"Connect Socket Failed",true,false,null)
                                }
                                Log.e("SocketManager","连接异常或被拒绝");
                            }
                            else -> {
                                this.runOnUiThread {
                                    DialogTool.showAlertDialog(this,"Connect Socket Close.",true,false,null)
                                }
                                Log.e("SocketManager","连接结束")
                            }
                        }
                        return@funTCPClientConnect
                    }

                    this.runOnUiThread {
                        DialogTool.showProgressDialog(this,"Search Device","Please reset to ISP mode",false)
                    }

                    SocketCmdManager.sendCMD_CONNECT { readBF, isChecksum, isTimeout ->

                        DialogTool.dismissDialog()

                        if (isTimeout == true) {
                            Log.i(TAG, "sendCMD_CONNECT ---- Search Device is time out.")
                            runOnUiThread {
                                DialogTool.showAlertDialog(this,"Search Device is time out.",true,false,null)
                                SocketManager.funTCPClientClose()
                                _mainMessageText.setText("WiFi Status: DisConnected")
                            }
                            return@sendCMD_CONNECT
                        }

                        if (isChecksum == false) {
                            Log.i(TAG, "sendCMD_CONNECT ---- is Not ISP InterFace")
                            runOnUiThread {
                                DialogTool.showAlertDialog(this,"is Not ISP InterFace",true,false,null)
                                SocketManager.funTCPClientClose()
                                _mainMessageText.setText("WiFi Status: DisConnected")
                            }
                            return@sendCMD_CONNECT
                        }

                        SocketCmdManager.sendCMD_GET_DEVICEID { readBF, isChecksum ->
                            runOnUiThread {
                                if (isChecksum == false || readBF == null) {
                                    Log.i(TAG, "sendCMD_GET_DEVICEID ---- fail")
                                    SocketManager.funTCPClientClose()
                                    return@runOnUiThread
                                }
                                _deviceID = ISPCommandTool.toDeviceID(readBF)
                                Log.i(TAG, "sendCMD_GET_DEVICEID ---- Device:$_deviceID")

                                if (FileManager.getChipInfoByPDID(_deviceID!!) == null) {
                                    _mainMessageText.setText("Find Device: unknown Device")
                                    _connectDeviceButton.isEnabled = true
                                    _connectDeviceButton.setBackgroundColor(Color.RED)
                                    _findDeviceButton.setText("DisConnect BLE")
                                    return@runOnUiThread
                                }
                                _mainMessageText.setText("Find Device: " + FileManager.CHIP_DATA.chipPdid.name)
                                _connectDeviceButton.isEnabled = true
                                _connectDeviceButton.setBackgroundColor(Color.RED)
                                _findDeviceButton.setText("DisConnect BLE")
                            }
                        }
                    }

                }
            }
            return@OnClickListener
        }

        OTGManager.startUsbConnecting(this)

    }

    /**
     * on connect Click Button
     */
    private val onConnectClickButton = View.OnClickListener {
        Log.i(TAG, "onConnectClickButton open")

        if(ISPManager.interfaceType == NulinkInterfaceType.WiFi && _deviceID != null){
            val intent = Intent(applicationContext, ISPActivity::class.java).apply {
                putExtra("DeviceID", _deviceID)
            }
            startActivity(intent)
            return@OnClickListener
        }

        if(ISPManager.interfaceType == NulinkInterfaceType.BLE && BluetoothLeCmdManager.BLE_DATA != null){
            val intent = Intent(applicationContext, ISPActivity::class.java).apply {
                putExtra("DeviceID", _deviceID)
            }
            startActivity(intent)
            return@OnClickListener
        }

        if (_USBDevice == null ) {
            return@OnClickListener
        }

        if (_deviceID == null) {
            return@OnClickListener
        }

        val intent = Intent(applicationContext, ISPActivity::class.java).apply {
            putExtra("DeviceID", _deviceID)
        }
        startActivity(intent)
    }


    //BLE////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /***
     * 執行藍芽的無線write連接
     */
    private fun BleConnectToDevice(){

        while (BluetoothLeCmdManager.WRITE_BC == null){
            Thread.sleep(100)
        }
        BluetoothLeCmdManager.sendCMD_CONNECT( callback = { byteArray, isChecksum, isTimeout ->

            DialogTool.dismissDialog()

            if (isTimeout == true) {
                Log.i(TAG, "sendCMD_CONNECT ---- Search Device is time out.")
                runOnUiThread {
                    DialogTool.showAlertDialog(this,"Search Device is time out.",true,false,null)
                    BluetoothLeCmdManager.BLE_DATA!!.setDisClose()
                    _mainMessageText.setText("BLE Status: DisConnected")
                }
                return@sendCMD_CONNECT
            }

            if (isChecksum == false) {
                Log.i(TAG, "sendCMD_CONNECT ---- is Not ISP InterFace")
                runOnUiThread {
                    DialogTool.showAlertDialog(this,"is Not ISP InterFace",true,false,null)
                    BluetoothLeCmdManager.BLE_DATA!!.setDisClose()
                    _mainMessageText.setText("BLE Status: DisConnected")
                }
                return@sendCMD_CONNECT
            }

            BluetoothLeCmdManager.sendCMD_GET_DEVICEID { byteArray, isChecksum ->
                runOnUiThread {
                    if (isChecksum == false || byteArray == null) {
                        Log.i(TAG, "sendCMD_GET_DEVICEID ---- fail")
                        BluetoothLeCmdManager.BLE_DATA!!.setDisClose()
                        return@runOnUiThread
                    }
                    _deviceID = ISPCommandTool.toDeviceID(byteArray)
                    Log.i(TAG, "sendCMD_GET_DEVICEID ---- Device:$_deviceID")

                    if (FileManager.getChipInfoByPDID(_deviceID!!) == null) {
                        _mainMessageText.setText("Find Device: unknown Device")
                        _connectDeviceButton.isEnabled = true
                        _connectDeviceButton.setBackgroundColor(Color.RED)
                        _findDeviceButton.setText("DisConnect BLE")
                        return@runOnUiThread
                    }
                    _mainMessageText.setText("Find Device: " + FileManager.CHIP_DATA.chipPdid.name)
                    _connectDeviceButton.isEnabled = true
                    _connectDeviceButton.setBackgroundColor(Color.RED)
                    _findDeviceButton.setText("DisConnect BLE")
                }
            }
        })
    }

    private fun setPermission(): Boolean {

        val pm = PermissionManager(this)
        val permissionArray = ArrayList<PermissionManager.PermissionType>()
        permissionArray.add(PermissionManager.PermissionType.GPS)
        permissionArray.add(PermissionManager.PermissionType.READ_EXTERNAL_STORAGE)
        permissionArray.add(PermissionManager.PermissionType.WRITE_EXTERNAL_STORAGE)
        permissionArray.add(PermissionManager.PermissionType.BLUETOOTH)
        permissionArray.add(PermissionManager.PermissionType.BLUETOOTH_SCAN)
        permissionArray.add(PermissionManager.PermissionType.BLUETOOTH_CONNECT)
        permissionArray.add(PermissionManager.PermissionType.MANAGE_EXTERNAL_STORAGE)

        pm.selfPermission("權限", permissionArray)

        return false
    }

    /**
     *  打開ＢＬＥ搜尋
     */
    private fun ScanBleDevice() {

        if(_bdm.isBluetoothEnabled(BluetoothLeDataManager.context) == false){
            Toast.makeText(BluetoothLeDataManager.context, "ble not supported", Toast.LENGTH_SHORT).show();
            return
        }
        if(_bdm.isGPSEnabled(BluetoothLeDataManager.context) == false){
            Toast.makeText(BluetoothLeDataManager.context, "GPS not supported", Toast.LENGTH_SHORT).show();
            return
        }

        _scanResultDeviceArray.clear()
        _scanResultArray.clear()

        _alertDialog = MaterialDialog(BluetoothLeDataManager.context)
            .cancelOnTouchOutside(false)
            .cancelable(false)
            .title(text = "BLE Device Scan...")
            .listItems(items = _scanResultDeviceArray) { dialog, index, text ->
                //TODO:點擊ＢＬＥ裝置事件

                if(index >= _scanResultArray.size) return@listItems

//                _bleDevice!!.setText("BLE Device: " + _scanResultArray.get(index).scanRecord!!.deviceName)
                Log.d(TAG, "onOptionsItemSelected:" + _scanResultArray.get(index).scanRecord!!.deviceName + " " + _scanResultArray.get(index).device.uuids)
                BluetoothLeCmdManager.BLE_DATA = _bdm.getBluetoothLeData(BluetoothLeDataManager.context, _scanResultArray.get(index).device.address)
//                this!!.TempBleData_LED = _BleData // 提前存 不然返回會是空值 無法斷線

                _bdm.scanLeDevice(false, BluetoothLeDataManager.context, scanCallback) //停止搜尋
                this.connectBle(bleData = BluetoothLeCmdManager.BLE_DATA!!)//藍芽連線

                _alertDialog.dismiss()
            }
            .negativeButton(null, "cancel") { materialDialog: MaterialDialog? ->
                Log.d(TAG, "ScanBleDevice Cancel")
                _bdm.scanLeDevice(false, BluetoothLeDataManager.context, scanCallback) //停止搜尋
                _alertDialog.dismiss()

            }
        _alertDialog.show()

        _bdm.scanLeDevice(true, BluetoothLeDataManager.context, scanCallback)

//        Timer().schedule(10000){
//            if (getActivity() == null || !isAdded()) return@schedule
//            requireActivity().runOnUiThread {
//                    _scanResultDeviceArray.add("** End of Search, If the device is not found, please try again. **")
//                    _alertDialog.updateListItems(items = _scanResultDeviceArray)
//                    _bdm.scanLeDevice(false, ResourceManager.context, scanCallback) //停止搜尋
//            }
//        }
    }

    /**
     * ＢＬＥ搜尋結果
     */
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onScanResult  callbackType:$callbackType   result:$result")

            var displayName = result.scanRecord!!.deviceName +"\n"+result.device.address

            if(result.scanRecord == null) return

            if (!_scanResultArray.contains(result) && !_scanResultDeviceArray.contains(displayName)) {
                if (result.scanRecord!!.deviceName != null) {
                    _scanResultArray.add(result)
                    Log.d(TAG, "onScanResult  deviceName:" + result.scanRecord!!.deviceName)
                    _scanResultDeviceArray.add(displayName)
                    _alertDialog.updateListItems(items = _scanResultDeviceArray)
                }
            }

        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            Log.d(TAG, "results:$results")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "errorCode:$errorCode")
        }
    }


    /**
     * ＢＬＥ藍芽連線
     */
    private fun connectBle(bleData: BluetoothLeData) {

        if (bleData == null) {
            return
        }

        runOnUiThread {
//            _bleStatus!!.setText("BLE Status: CONNECTING")
//            _bleStatus!!.setTextColor(Color.BLUE)
        }

        bleData.setOnStateChangeListener(onStateChangeListener)

        bleData.connectLeDevice {
            Log.i("MainActivity", "connectLeDevice:" + it)
            if (it != true) {
                onStateChangeListener.onStateChange(bleData.getbleMacAddress(), 0, 0)
                return@connectLeDevice
            }
            for (bs in bleData.servicesDataArray) {
                for (bc in bs.characteristicDataArray) {
                    Log.i("MainActivity", "characteristic:" + bc.uuid)

                    //專門用來寫入之特徵
                    if (bc.uuid.indexOf("0000abf1") > -1) {
                        BluetoothLeCmdManager.WRITE_BC = bc
                    }

                    //專門用來監聽之特徵
                    if (bc.uuid.indexOf("0000abf2") > -1) {
                        bc.setNotify(true, BluetoothLeCmdManager.BleNotifyListener)
                        Log.i("MainActivity", "setNotify:" + bc.uuid)
                    }

                }
            }

            thread {
                this.BleConnectToDevice()
            }
        }
    }

    /**
     * 監聽ＢＬＥ連線變化
     */
    private var onStateChangeListener = BluetoothLeData.onStateChange { MAC, status, newState ->

        Log.i("onStateChangeListener", "MAC:" + MAC + "  status:" + status + "  newState:" + newState)

//        if (getActivity() == null || !isAdded()) return@onStateChange

        runOnUiThread {
            if (newState == BluetoothProfile.STATE_CONNECTING) {
                runOnUiThread {
//                    _bleStatus!!.setText("BLE Status: CONNECTING")
//                    _bleStatus!!.setTextColor(Color.BLUE)
                    _mainMessageText.setText("BLE Status: Connecting")
                }
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    _mainMessageText.setText("BLE Status: Connected")
                    this.runOnUiThread {
                        DialogTool.showProgressDialog(this,"Search Device","Please reset to ISP mode",false)
                    }

                }
            }
        }

        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            BluetoothLeCmdManager.WRITE_BC = null
            runOnUiThread {
                _mainMessageText.setText("BLE Status: DisConnected")
            }
        }
    }


}