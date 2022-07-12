package com.nuvoton.nuisptool_android

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import java.io.File
import java.io.FileInputStream
import android.text.method.ScrollingMovementMethod
import android.view.Display
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.ISPTool.*
import com.nuvoton.nuisptool_android.Util.DialogTool
import com.nuvoton.nuisptool_android.Util.HEXTool.to2HexString
import kotlin.concurrent.thread
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ISPActivity : AppCompatActivity() {

    private var TAG = "ISPActivity"
    private lateinit var _progressBar : ProgressBar
    private lateinit var _selectAprom: ImageButton
    private lateinit var _selectDataFlash: ImageButton
    private lateinit var _radioButton_aprom: RadioButton
    private lateinit var _radioButton_dataflash: RadioButton
    private lateinit var _button_burn: Button
    private lateinit var _button_config: Button
    private lateinit var _button_disconnect: Button
    private lateinit var _text_devies_interface : TextView
    private lateinit var _text_devies_part_no : TextView
    private lateinit var _text_devies_aprom: TextView
    private lateinit var _text_devies_data: TextView
    private lateinit var _text_devies_fw_ver: TextView
    private lateinit var _text_devies_config: TextView
    private lateinit var _checkbox_aprom :CheckBox
    private lateinit var _checkbox_date_flash :CheckBox
    private lateinit var _checkbox_rest_and_run :CheckBox
    private lateinit var _checkbox_erase_all :CheckBox
    private lateinit var _text_message_display : TextView
    private lateinit var _text_message2_display : TextView
    private lateinit var _button_config_0: Button
    private lateinit var _button_config_1: Button
    private lateinit var _button_config_2: Button
    private lateinit var _button_config_3: Button
    private lateinit var _editTextAddress : EditText
    private lateinit var _checkbox_address :CheckBox

    private var apromBinDataText = ""
    private var _apromSize= 0
    private var flishBinDataText = ""
    private var _DataFlashSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ispactivity)
        getSupportActionBar()!!.setTitle("Nuvoton Android ISP Tool")
        _text_devies_interface = findViewById<View>(R.id.connection_interface) as TextView
        _progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        _text_message_display = findViewById<View>(R.id.MESSAGE_TEXT) as TextView
        _text_message_display.setMovementMethod(ScrollingMovementMethod())
        _text_message2_display = findViewById<View>(R.id.MESSAGE2_TEXT) as TextView
        _text_message2_display.setMovementMethod(ScrollingMovementMethod())
        _text_devies_part_no = findViewById<View>(R.id.devies_part_no) as TextView
        _text_devies_aprom = findViewById<View>(R.id.devies_aprom) as TextView
        _text_devies_data = findViewById<View>(R.id.devies_data) as TextView
        _text_devies_fw_ver = findViewById<View>(R.id.devies_fw_ver) as TextView
        _button_config_0 = findViewById<View>(R.id.button_setConfig0) as Button
        _button_config_1 = findViewById<View>(R.id.button_setConfig1) as Button
        _button_config_2 = findViewById<View>(R.id.button_setConfig2) as Button
        _button_config_3 = findViewById<View>(R.id.button_setConfig3) as Button
        _button_config_0.setOnClickListener(onConfigClickButton0)
        _button_config_1.setOnClickListener(onConfigClickButton1)
        _button_config_2.setOnClickListener(onConfigClickButton2)
        _button_config_3.setOnClickListener(onConfigClickButton3)
        _button_config = findViewById<View>(R.id.setting) as Button
        _button_config.setOnClickListener(onConfigClickButton)
        _button_burn = findViewById<View>(R.id.button_burn) as Button
        _button_burn.setOnClickListener(onBurnClickButton)
        _button_disconnect = findViewById<View>(R.id.disconnect) as Button
        _button_disconnect.setOnClickListener(ondisconnectClickButton)
        _selectAprom = findViewById<View>(R.id.select_aprom) as ImageButton
        _selectAprom.setOnClickListener(onSelectApromClickButton)
        _selectDataFlash = findViewById<View>(R.id.select_dateflash) as ImageButton
        _selectDataFlash.setOnClickListener(onSelectDataFlashClickButton)
        _checkbox_aprom = findViewById<View>(R.id.checkbox_aprom) as CheckBox
        _checkbox_date_flash = findViewById<View>(R.id.checkbox_date_flash) as CheckBox
        _checkbox_rest_and_run = findViewById<View>(R.id.checkbox_rest_and_run) as CheckBox
        _checkbox_erase_all = findViewById<View>(R.id.checkbox_erase_all) as CheckBox
        _checkbox_address = findViewById<View>(R.id.checkbox_address) as CheckBox
        _editTextAddress = findViewById<View>(R.id.textAddress) as EditText
        _radioButton_aprom = findViewById<View>(R.id.radioButton_aprom) as RadioButton
        _radioButton_dataflash = findViewById<View>(R.id.radioButton_dataflash) as RadioButton
        _radioButton_aprom.setOnCheckedChangeListener { compoundButton, b ->
            Log.i(TAG, "radioButton_aprom b:$b")
            _radioButton_dataflash.isChecked = !b
            if(b){
                _text_message_display.visibility = View.VISIBLE
                _text_message2_display.visibility = View.INVISIBLE
            }
        }
        _radioButton_dataflash.setOnCheckedChangeListener { compoundButton, b ->
            Log.i(TAG, "radioButton_dataflash b:$b")
            _radioButton_aprom.isChecked = !b
            if(b){
                _text_message2_display.visibility = View.VISIBLE
                _text_message_display.visibility = View.INVISIBLE
            }
        }
        _text_message_display.setText(apromBinDataText)

    }

    override fun onResume() {
        super.onResume()

        this.initUI()

        //註冊離線監聽
        OTGManager.setIsOnlineListener {
            if (it == false) { //裝置離線
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

    override fun onBackPressed() {
//        super.onBackPressed()
        Log.i(TAG, "onBackPressed")
        DialogTool.showAlertDialog(this,"want to Disconnect?",true,true , callback = { isOk , isNo ->
            if(isOk){
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                this.startActivity(intent)
            }
        })
        return
    }
    /**
     * 用來更新畫面
     */
    fun initUI(){

        _text_devies_interface.setText("Connection Interface："+ISPManager.interfaceType.name)

        ISPManager.sendCMD_GET_FWVER( callback = { readArray,isChecksum ->
            _text_devies_fw_ver.text = "Firmware：0x"+readArray?.let { ISPCommandTool.toFirmwareVersion(it) }
        })

        //讀取ＪＳＯＮ檔產生列表
        FileManager.loadChipInfoFile(this)
        FileManager.loadChipPdidFile(this)

        _text_devies_part_no.text = "Part No.：" + FileManager.CHIP_DATA.chipInfo.name
        _text_devies_aprom.text = "APROM：" + FileManager.CHIP_DATA.chipInfo.AP_size
        _text_devies_data.text = "DataFlash：" + FileManager.CHIP_DATA.chipInfo.DF_size
        _apromSize = FileManager.CHIP_DATA.chipInfo.AP_size.split("*")[0].toInt()
        _DataFlashSize = FileManager.CHIP_DATA.chipInfo.DF_size.split("*")[0].toInt()

        //讀取ＪＳＯＮ檔產生列表
        val series = FileManager.CHIP_DATA.chipPdid.series
        val index = FileManager.CHIP_DATA.chipPdid.jsonIndex
        val isLoadSuccess = ConfigManager.readConfigFromFile(this,series, index)
        if(isLoadSuccess != true){
            runOnUiThread {
                _button_config.isEnabled = false
                _button_config.setBackgroundColor(Color.GRAY)
                DialogTool.showAlertDialog(this,"Config json file not found!\nThe burning function can be used,\n" +
                        "but the correctness of the function is not guaranteed.",true,false,null)
            }

            ISPManager.sendCMD_READ_CONFIG( callback = {
                if (it == null) { return@sendCMD_READ_CONFIG}
                var configText = "Config 0,1,2,3：\n"
                val displayConfig0 = ISPCommandTool.toDisplayComfig0(it)
                val displayConfig1 = ISPCommandTool.toDisplayComfig1(it)
                val displayConfig2 = ISPCommandTool.toDisplayComfig2(it)
                val displayConfig3 = ISPCommandTool.toDisplayComfig3(it)
                _button_config_0.setText(displayConfig0)
                _button_config_1.setText(displayConfig1)
                _button_config_2.setText(displayConfig2)
                _button_config_3.setText(displayConfig3)
            })

            return
        }

        ISPManager.sendCMD_READ_CONFIG( callback = {
            if (it == null) { return@sendCMD_READ_CONFIG}

            var configText = "Config 0,1,2,3：\n"
            val displayConfig0 = ISPCommandTool.toDisplayComfig0(it)
            val displayConfig1 = ISPCommandTool.toDisplayComfig1(it)
            val displayConfig2 = ISPCommandTool.toDisplayComfig2(it)
            val displayConfig3 = ISPCommandTool.toDisplayComfig3(it)
            _button_config_0.setText(displayConfig0)
            _button_config_1.setText(displayConfig1)
            _button_config_2.setText(displayConfig2)
            _button_config_3.setText(displayConfig3)

            ConfigManager.initReadBufferToConfigData(it)

            _button_config_0.isEnabled = ConfigManager.CONFIG_JSON_DATA.subConfigSets[0].isEnable
            _button_config_1.isEnabled = ConfigManager.CONFIG_JSON_DATA.subConfigSets[1].isEnable
            _button_config_2.isEnabled = ConfigManager.CONFIG_JSON_DATA.subConfigSets[2].isEnable
            _button_config_3.isEnabled = ConfigManager.CONFIG_JSON_DATA.subConfigSets[3].isEnable

            val tempAPROMSize = FileManager.CHIP_DATA.chipInfo.AP_size.split("*")[0].toInt() * 1024
            _DataFlashSize = FileManager.CHIP_DATA.chipInfo.DF_size.split("*")[0].toInt() * 1024
            _apromSize = tempAPROMSize / 1024

            for ( configSet in ConfigManager.CONFIG_JSON_DATA.subConfigSets[0].subConfigs){
                if(configSet.offset == 0){
                    if(configSet.values == "0"){
                        //0 = Data Flash Enabled.

                        _apromSize = ISPCommandTool.toAPROMSize(it)/1024
                        _DataFlashSize = (tempAPROMSize - ISPCommandTool.toAPROMSize(it))/1024

                    }
                    if(configSet.values == "1"){
                        //1 = Data Flash Disabled.
                    }
                }
            }

        })

        _text_devies_part_no.text = "Part No.："+FileManager.CHIP_DATA.chipInfo.name
        _text_devies_aprom.text = "APROM："+_apromSize.toString() + "*1024"
        _text_devies_data.text = "DataFlash："+_DataFlashSize.toString() + "*1024"

        if(_DataFlashSize <= 0){
            _checkbox_date_flash.isEnabled = false
            _selectDataFlash.isEnabled = false
        }else{
            _checkbox_date_flash.isEnabled = true
            _selectDataFlash.isEnabled = true
        }

    }

    /**********************************************************************************************/
    //config set button
    private val onConfigClickButton0 = View.OnClickListener {
        Log.i(TAG, "onConfigClickButton0")
        DialogTool.showInputAlertDialog(this,"Set Config 0 Hex","Wrong setting may cause serious Impact.\n" +
                "Please confirm the write-in setting.",
            callback =  { inputHex  ->
                if(inputHex == "" ) {
                    return@showInputAlertDialog
                }
                if(HEXTool.isHexString(inputHex) == false) {
                    //TODO 提醒使用者
                        DialogTool.showAlertDialog(this,"incorrect Hex.",true,false,null)
                    return@showInputAlertDialog
                }
                val Config0 = inputHex.toUInt(16)
                val Config1 = _button_config_1.text.toString().toUInt(16)
                val Config2 = _button_config_2.text.toString().toUInt(16)
                val Config3 = _button_config_3.text.toString().toUInt(16)

                ISPManager.sendCMD_UPDATE_CONFIG(Config0,Config1,Config2,Config3,{
                        this.initUI()
                })

        })

    }
    private val onConfigClickButton1 = View.OnClickListener {
        Log.i(TAG, "onConfigClickButton1")
        DialogTool.showInputAlertDialog(this,"Set Config 1 Hex","Wrong setting may cause serious Impact.\n" +
                "Please confirm the write-in setting.",
            callback =  { inputHex  ->
                if(inputHex == "" ) {
                    return@showInputAlertDialog
                }
                if(HEXTool.isHexString(inputHex) == false) {
                    //TODO 提醒使用者
                    DialogTool.showAlertDialog(this,"incorrect Hex.",true,false,null)
                    return@showInputAlertDialog
                }

                val Config0 = _button_config_0.text.toString().toUInt(16)
                val Config1 = inputHex.toUInt(16)
                val Config2 = _button_config_2.text.toString().toUInt(16)
                val Config3 = _button_config_3.text.toString().toUInt(16)

                ISPManager.sendCMD_UPDATE_CONFIG(Config0,Config1,Config2,Config3,{
                    this.initUI()
                })
        })
    }
    private val onConfigClickButton2 = View.OnClickListener {
        Log.i(TAG, "onConfigClickButton2")
        DialogTool.showInputAlertDialog(this,"Set Config 2 Hex","Wrong setting may cause serious Impact.\n" +
                "Please confirm the write-in setting.",
            callback =  { inputHex  ->
                if(inputHex == "" ) {
                    return@showInputAlertDialog
                }
                if(HEXTool.isHexString(inputHex) == false) {
                    //TODO 提醒使用者
                    DialogTool.showAlertDialog(this,"incorrect Hex.",true,false,null)
                    return@showInputAlertDialog
                }
                val Config0 = _button_config_0.text.toString().toUInt(16)
                val Config1 = _button_config_1.text.toString().toUInt(16)
                val Config2 = inputHex.toUInt(16)
                val Config3 = _button_config_3.text.toString().toUInt(16)

                ISPManager.sendCMD_UPDATE_CONFIG(Config0,Config1,Config2,Config3,{
                    this.initUI()
                })
        })
    }
    private val onConfigClickButton3 = View.OnClickListener {
        Log.i(TAG, "onConfigClickButton3")
        DialogTool.showInputAlertDialog(this,"Set Config 3 Hex","Wrong setting may cause serious Impact.\n" +
                "Please confirm the write-in setting.",
            callback =  { inputHex  ->
                if(inputHex == "" ) {
                    return@showInputAlertDialog
                }
                if(HEXTool.isHexString(inputHex) == false) {
                    //TODO 提醒使用者
                    DialogTool.showAlertDialog(this,"incorrect Hex.",true,false,null)
                    return@showInputAlertDialog
                }

                val Config0 = _button_config_0.text.toString().toUInt(16)
                val Config1 = _button_config_1.text.toString().toUInt(16)
                val Config2 = _button_config_2.text.toString().toUInt(16)
                val Config3 = inputHex.toUInt(16)

                ISPManager.sendCMD_UPDATE_CONFIG(Config0,Config1,Config2,Config3,{
                    this.initUI()
                })
        })
    }
    /**********************************************************************************************/

    /**
     * on disconnect Click Button
     *
     */
    private val ondisconnectClickButton = View.OnClickListener {
        Log.i(TAG, "ondisconnectClickButton")
        DialogTool.showAlertDialog(this,"want to Disconnect?",true,true , callback = { isOk , isNo ->
            if(isOk){
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                this.startActivity(intent)
            }
        })
    }

    /**
     * on onConfig Click Button
     *
     */
    private val onConfigClickButton = View.OnClickListener {
        Log.i(TAG, "onConfigClickButton")
        val intent = Intent(applicationContext,ConfigActivity::class.java).apply {
//            putExtra(EXTRA_KEY, message)
        }
        startActivity(intent)
    }
    /**
     * on _selectAprom Click Button
     */
    private val onSelectApromClickButton = View.OnClickListener {
        Log.i(TAG, "onSelectApromClickButton")

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"

        val chooser = Intent.createChooser(intent, null)
        resultLauncher_APROM.launch(chooser)

    }
    /**
     * on _selectDataFlash Click Button
     */
    private val onSelectDataFlashClickButton = View.OnClickListener {
        Log.i(TAG, "onSelectDataFlashClickButton")

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"

        val chooser = Intent.createChooser(intent, null)
        resultLauncher_DataFlash.launch(chooser)
    }
    /**
     * 註冊「取得檔案路徑 APROM 監聽」
     */
    private var resultLauncher_APROM =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val myData: Intent? = result.data
                if (myData != null) {
                    val uri = myData.data

                    if (uri == null) {
                        return@registerForActivityResult
                    }

                    val bin = FileData()
                    bin.uri = uri
                    bin.name = FileManager.getNameByUri(uri, this)
                    bin.path = uri.path.toString()
                    bin.type = "APROM"
                    bin.file = File(uri.path);

                    if(bin.name.indexOf(".bin") <= -1){
                        runOnUiThread {
                            DialogTool.showAlertDialog(this,"con't open this type of file",true,false,null)
                        }
                        return@registerForActivityResult
                    }

                    FileManager.APROM_BIN = bin //存回

                    thread {
                        Log.i(TAG, "APROM_BIN PATH:" + bin.path)
                        val inputStream = this.contentResolver.openInputStream(uri)
                        bin.byteArray = inputStream!!.readBytes()

                        if(_apromSize * 1024 <= bin.byteArray.size){
                            runOnUiThread {
                                _checkbox_aprom.isChecked = false
                                _checkbox_aprom.isEnabled = false
                                DialogTool.showAlertDialog(this,"bin Size > flash Size !",true,false,null)
                            }
                            return@thread
                        }

                        runOnUiThread {
                            DialogTool.showProgressDialog(this,"Please Wait","Data Loading ...",false)
                            _text_message_display.setText("")
                        }

                        thread {
                            val tempText = bin.byteArray.to2HexString()
                            val cArray = tempText.chunked(16+16+16) // 空格16 字元16x2
                            for (i in 0..cArray.size-1){
                                runOnUiThread {
                                    _text_message_display.append(HEXTool.toHex16String(i * 16) + "：" + cArray[i] + "\n")
                                }
                            }

                            runOnUiThread {
                                _text_message_display.setMovementMethod(ScrollingMovementMethod())
                                _checkbox_aprom.setText("APROM："+bin.name)
                                _checkbox_aprom.isChecked = true
                                _checkbox_aprom.isEnabled = true
                                _progressBar.visibility = View.INVISIBLE
                                DialogTool.dismissDialog()
                            }
                        }

                    }


                }
            }
        }
    /**
     * 註冊「取得檔案路徑 DataFlash 監聽」
     */
    private var resultLauncher_DataFlash =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val myData: Intent? = result.data
                if (myData != null) {
                    val uri = myData.data

                    if (uri == null) {
                        return@registerForActivityResult
                    }

                    val bin = FileData()
                    bin.uri = uri
                    bin.name = FileManager.getNameByUri(uri, this)
                    bin.path = uri.path.toString()
                    bin.type = "DataFlash"
                    bin.file = File(uri.path);

                    if(bin.name.indexOf(".bin") <= -1){
                        runOnUiThread {
                            DialogTool.showAlertDialog(this,"con't open this type of file",true,false,null)
                        }
                        return@registerForActivityResult
                    }

                    FileManager.DATAFLASH_BIN = bin //存回

                    thread {
                        Log.i(TAG, "DATA_FLASH PATH:" + bin.path)
                        val inputStream = this.contentResolver.openInputStream(uri)
                        bin.byteArray = inputStream!!.readBytes()

                        if(_DataFlashSize * 1024 <= bin.byteArray.size){
                            runOnUiThread {
                                _checkbox_date_flash.isChecked = false
                                _checkbox_date_flash.isEnabled = false
                                DialogTool.showAlertDialog(this,"bin Size > flash Size !",true,false,null)
                            }
                            return@thread
                        }

                        runOnUiThread {
                            DialogTool.showProgressDialog(this,"Please Wait","Data Loading ...",false)
                            _text_message2_display.setText("")
                        }

                        thread {
                            val tempText = bin.byteArray.to2HexString()
                            val cArray = tempText.chunked(16+16+16) // 空格16 字元16x2
                            for (i in 0..cArray.size-1){
                                runOnUiThread {
                                    _text_message2_display.append(HEXTool.toHex16String(i * 16) + "：" + cArray[i] + "\n")
                                }
                            }

                            runOnUiThread {
                                _text_message2_display.setMovementMethod(ScrollingMovementMethod())
                                _checkbox_date_flash.setText("DATA FLASH："+bin.name)
                                _checkbox_date_flash.isChecked = true
                                _checkbox_date_flash.isEnabled = true
                                _progressBar.visibility = View.INVISIBLE
                                DialogTool.dismissDialog()
                            }
                        }

                    }
                }
            }
        }
    /**
     * on _button_burn Click Button
     * 開始燒入
     */
    private val onBurnClickButton = View.OnClickListener {
        Log.i(TAG, "onBurnClickButton")

        if(_checkbox_erase_all.isChecked == false && _checkbox_aprom.isChecked == false && _checkbox_date_flash.isChecked == false && _checkbox_rest_and_run.isChecked == false ){
            DialogTool.showAlertDialog(this,"Please choose a function",true,false,null)
            return@OnClickListener
        }

        if (_checkbox_aprom.isChecked == true) {
            if (FileManager.APROM_BIN == null || FileManager.APROM_BIN!!.byteArray.isEmpty()) {
                DialogTool.showAlertDialog(this,"APROM Bin isEmpty",true,false,null)
                return@OnClickListener
            }
        }

        if (_checkbox_date_flash.isChecked == true) {
            if (FileManager.DATAFLASH_BIN == null || FileManager.DATAFLASH_BIN!!.byteArray.isEmpty()) {
                DialogTool.showAlertDialog(this,"DataFlash Bin isEmpty",true,false,null)
                return@OnClickListener
            }
        }

        //需要照順序 EraseALL> Config bit > APROM > DATAFLASH > Reset Run
        var hasFaile = false
        thread {
            //  Erase All
            if (_checkbox_erase_all.isChecked == true) {
                runOnUiThread {
                    DialogTool.showProgressDialog(this, "Burn", "Erase All ing ...", false)
                }
                ISPManager.sendCMD_ERASE_ALL( callback = { readArray, isChackSum ->
                    runOnUiThread {
                        DialogTool.dismissDialog()
                    }
                    if(isChackSum == false){
                        hasFaile = true
                        runOnUiThread {
                            DialogTool.showAlertDialog(this,"Erase All Failed.",true,false,null)
                        }
                    }
                })
            }
            //  APROM
            if (_checkbox_aprom.isChecked == true && hasFaile == false) {

                runOnUiThread {
                    DialogTool.showProgressDialog(this, "Burn", "APROM is burning ...", true)
                }

                val dataArray = FileManager.APROM_BIN!!.byteArray

                var startAddress = (0x00000000).toUByte().toUInt()
                if(!_editTextAddress.text.isNullOrEmpty() && _checkbox_address.isChecked == true){
                    if(HEXTool.isHexString(_editTextAddress.text.toString()) == false) {
                        //TODO 提醒使用者
                        runOnUiThread {
                            DialogTool.showAlertDialog(this,"incorrect Hex.",true,false,null)
                        }
                        return@thread
                    }
                    startAddress = _editTextAddress.text.toString().toUInt(16)
                }

                ISPManager.sendCMD_UPDATE_BIN(ISPCommands.CMD_UPDATE_APROM,dataArray,startAddress,callback = { readArray, progress ->

                    Log.i(TAG, "sendCMD_UPDATE_APROM : " + progress + "%")

                    runOnUiThread {
                        DialogTool.upDataProgressDialog(progress)
                        if (progress == 100) {
                            DialogTool.dismissDialog()
                        }
                        if (progress == -1) {
                            DialogTool.dismissDialog()
                            hasFaile = true
                            DialogTool.showAlertDialog(this,"burn APROM Failed.",true,false,null)
                        }
                    }
                })
            }
            //DataFlash
            if (_checkbox_date_flash.isChecked == true && hasFaile == false) {

                runOnUiThread {
                    DialogTool.showProgressDialog(this, "Burn", "DataFlash is burning ...", true)
                }

                val dataArray = FileManager.DATAFLASH_BIN!!.byteArray
                val startAddress = (0x00000000).toUByte().toUInt() //特殊chip以後可以改

                ISPManager.sendCMD_UPDATE_BIN(ISPCommands.CMD_UPDATE_DATAFLASH,dataArray,startAddress,callback = { readArray, progress ->

                    Log.i(TAG, "CMD_UPDATE_DATAFLASH : " + progress + "%")

                    runOnUiThread {
                        DialogTool.upDataProgressDialog(progress)
                        if (progress == 100) {
                            DialogTool.dismissDialog()
                        }
                        if (progress == -1) {
                            DialogTool.dismissDialog()
                            hasFaile = true
                            DialogTool.showAlertDialog(this,"burn DataFlash Failed.",true,false,null)
                        }
                    }
                })
            }
            //Reset and Run now
            if (_checkbox_rest_and_run.isChecked == true && hasFaile == false) {
                runOnUiThread {
                    DialogTool.showProgressDialog(this, "Burn", "Reset and Run now.", true)
                }
                ISPManager.sendCMD_RUN_APROM( callback = {
                    runOnUiThread {
                        DialogTool.showAlertDialog(
                            this,
                            "Reset and Run finish,need re plugin to connect.",
                            true,
                            false,
                            callback = { isOk, isNo ->
                                val intent = Intent(this, MainActivity::class.java)
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                this.startActivity(intent)
                            })
                    }
                })
            }
        }



    }

//鍵盤處理////////////////////////////////////////////////////////////////////////////////////
    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (isShouldHideInput(v, motionEvent)) {
                hideSoftInput(v!!.windowToken)
                v.requestFocus()
            }
        }
        return super.dispatchTouchEvent(motionEvent)
    }
    private fun isShouldHideInput(v: View?, event: MotionEvent): Boolean {
        if (v != null && v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.getHeight()
            val right = (left
                    + v.getWidth())
            return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
        }
        return false
    }
    private fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val im: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow( token,  InputMethodManager.HIDE_NOT_ALWAYS )
        }
    }
}