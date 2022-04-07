package com.nuvoton.nuisptool_android


import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.ISPTool.*

import com.nuvoton.nuisptool_android.Util.Log
import com.nuvoton.nuisptool_android.Util.PermissionManager
import java.util.ArrayList


class MainActivity : AppCompatActivity() {

    private var TAG = "MainActivity"

    private lateinit var _findDeviceButton: Button
    private lateinit var _connectDeviceButton: Button
    private lateinit var _mainMessageText: TextView
//    private lateinit var _sp_interFace: Spinner
    private var _deviceID : String? = null
    private var _USBDevice: UsbDevice? = null
    private lateinit var _textVersion: TextView

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getSupportActionBar()!!.setTitle("Nuvoton Android ISP Tool")

        _findDeviceButton = findViewById<View>(R.id.button) as Button
        _findDeviceButton.setOnClickListener(onClickButton)
        _connectDeviceButton = findViewById<View>(R.id.connect) as Button
        _connectDeviceButton.setOnClickListener(onConnectClickButton)
        _mainMessageText = findViewById<View>(R.id.main_message) as TextView
        _textVersion= findViewById<View>(R.id.textVersion) as TextView
        _textVersion.text = "Version "+BuildConfig.VERSION_NAME
//        sp_interFace = findViewById<View>(R.id.spinner) as Spinner
//        val lunch = arrayListOf("USB", "UART", "SPI", "I2C", "RS485", "CAN")
//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lunch)
//        sp_interFace.setAdapter(adapter)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)

        }

        this.initUSBHost()
        this.initChipInfoData()


    }

    override fun onResume() {
        super.onResume()

        OTGManager.setIsOnlineListener {
            if (it == false) { //裝置離線 回到首頁
                runOnUiThread {
                    val builder = android.app.AlertDialog.Builder(this)
                    builder.setMessage(R.string.Device_is_Disconnection)
                        .setNegativeButton(R.string.ok) { dialog, id ->

                            _USBDevice = null
                            _mainMessageText.text = "no Device"
//                            val intent = Intent(this, MainActivity::class.java)
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
////                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)//銷燬目標Activity和它之上的所有Activity，重新建立目標Activity
//                            this.startActivity(intent)

                        }
                    builder.show()
                }
            }
        }
    }

    fun initChipInfoData(){
        //讀取ＪＳＯＮ檔產生列表
        val infoJson = FileManager.loadChipInfoFile(this)
        val pdidJson = FileManager.loadChipPdidFile(this)
        FileManager.saveFile(this,infoJson,pdidJson)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun initUSBHost(){
        //todo OTGManager USB HOST init
        OTGManager.init(this)//初始化
        OTGManager.start(this)//監聽
        OTGManager.setGetUsbDeviceListener {
            //取得裝置device
            _USBDevice = it
            //todo 判斷是否為可連接ＰＩＤ   16128為ISP_USB
            if (_USBDevice!!.productId != 16128) {

                this.runOnUiThread {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(R.string.IS_Not_ISP_Device)
                        .setNegativeButton(R.string.ok,
                            DialogInterface.OnClickListener { dialog, id ->
                                // User cancelled the dialog
                            })
                    builder.show()
                }
                return@setGetUsbDeviceListener
            }

            //送出指令
            ISPManager.sendCMD_CONNECT(_USBDevice!!, callback = { byteArray, isChecksum ->
                if (isChecksum == false) {
                    Log.i(TAG, "sendCMD_CONNECT ---- is Not USB InterFace")
                }
                ISPManager.sendCMD_GET_DEVICEID(_USBDevice!! ,callback = { byteArray, isChecksum ->
                    if (isChecksum == false || byteArray == null) {
                        Log.i(TAG, "sendCMD_GET_DEVICEID ---- fail")
                        return@sendCMD_GET_DEVICEID
                    }
                    _deviceID = ISPCommandTool.toDeviceID(byteArray)
                    Log.i(TAG, "sendCMD_GET_DEVICEID ---- Device:$_deviceID")

                    if(FileManager.getChipInfoByPDID(_deviceID!!) == null){
                        _mainMessageText.setText("Find Device: unknown Device" )
                        return@sendCMD_GET_DEVICEID
                    }
                    _mainMessageText.setText("Find Device: "+FileManager.CHIP_DATA.chipPdid.name )
                })
            })
        }
    }

    /**
     * Read Listener callback
     */
    private var ReadListenerCallback :(ByteArray)->Unit =   {

    }

    /**
     * on Click Button
     */
    private val onClickButton = View.OnClickListener {
        Log.i(TAG,"onClickButton")

        OTGManager.startUsbConnecting(this)

    }

    /**
     * on connect Click Button
     */
    private val onConnectClickButton = View.OnClickListener {
        Log.i(TAG,"onConnectClickButton")

        if(_USBDevice == null){
            return@OnClickListener
        }

        if(_deviceID == null){
            return@OnClickListener
        }
        val intent = Intent(applicationContext,ISPActivity::class.java).apply {
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