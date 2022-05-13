package com.nuvoton.nuisptool_android


import android.app.Activity
import android.app.AlertDialog
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
import com.nuvoton.nuisptool_android.Util.DialogTool


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
        _radioButtonList = arrayOf(_radioButton_USB,_radioButton_UART,_radioButton_SPI,_radioButton_I2C,_radioButton_RS485,_radioButton_CAN)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)

        }

        this.initUSBHost()
        this.initChipInfoData()
        ISPManager.interfaceType = NulinkInterfaceType.USB

    }

    override fun onResume() {
        super.onResume()

        OTGManager.setIsOnlineListener {
            if (it == false) { //裝置離線 回到首頁
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

        //送出指令
        ISPManager.sendCMD_CONNECT(_USBDevice!!, callback = { byteArray, isChecksum, isTimeout ->

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

            ISPManager.sendCMD_READ_CONFIG(_USBDevice!!, callback = {byteArray ->
                if (isChecksum == false || byteArray == null) {
                    Log.i(TAG, "sendCMD_READ_CONFIG ---- fail")
                    return@sendCMD_READ_CONFIG
                }
            })

            ISPManager.sendCMD_GET_DEVICEID(_USBDevice!!, callback = { byteArray, isChecksum ->
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

        OTGManager.startUsbConnecting(this)

    }

    /**
     * on connect Click Button
     */
    private val onConnectClickButton = View.OnClickListener {
        Log.i(TAG, "onConnectClickButton")

        if (_USBDevice == null) {
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

    private fun setPermission(): Boolean {

        val pm = PermissionManager(this)
        val permissionArray = ArrayList<PermissionManager.PermissionType>()
        permissionArray.add(PermissionManager.PermissionType.GPS)
        permissionArray.add(PermissionManager.PermissionType.READ_EXTERNAL_STORAGE)
        permissionArray.add(PermissionManager.PermissionType.WRITE_EXTERNAL_STORAGE)
        permissionArray.add(PermissionManager.PermissionType.BLUETOOTH)
        permissionArray.add(PermissionManager.PermissionType.MANAGE_EXTERNAL_STORAGE)

        pm.selfPermission("權限", permissionArray)

        return false
    }
}