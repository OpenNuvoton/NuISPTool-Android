package com.nuvoton.nuisptool_android.Bluetooth

import android.os.Build
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.Bluetooth.BluetoothLeData.CharacteristicData
import com.nuvoton.nuisptool_android.ISPTool.*
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object BluetoothLeCmdManager {

//    private var _any:Any = Any()
//    private var _nowCMD :ISPCommands = ISPCommands.CMD_CONNECT
//    private var _loopSend:Boolean = false
    private var _responseBuffer:ByteArray = byteArrayOf()
    public var WRITE_BC: CharacteristicData? = null
    public var BLE_DATA: BluetoothLeData? = null
    /***
     * 收通知的地方
     */
    public val BleNotifyListener = BluetoothLeData.notifCallBack { bleMAC, UUID, Value ->
        var readBufferStrring = HEXTool.toHexString(Value)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.d("BluetoothLeCmdManager", "notif:" + display )

        if(Value.size<64){
            Log.d("BluetoothLeCmdManager", "notif Value.size < 64  !!!"  )
            Log.d("BluetoothLeCmdManager", "notif Value.size < 64  !!!"  )
            Log.d("BluetoothLeCmdManager", "notif Value.size < 64  !!!"  )
            _responseBuffer = _responseBuffer + Value
            return@notifCallBack
        }

        _responseBuffer = Value
    }

    /**
     * 處理write與「等待」回傳
     */
    private fun write(sendBuffer:ByteArray):ByteArray{

        _responseBuffer = byteArrayOf()
        WRITE_BC!!.write( sendBuffer)
        var readBufferStrring = HEXTool.toHexString(sendBuffer)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.i("BluetoothLeCmdManager", "write   CMD:"+display)
        while (_responseBuffer.size < 64){
            Thread.sleep(10)
        }
        return _responseBuffer
    }

    fun sendCMD_CONNECT( callback: ((ByteArray?, Boolean, isTimeout:Boolean) -> Unit)) {

        if(WRITE_BC == null) return
        ISPManager.packetNumber = (0x00000001).toUInt()

        val cmd = ISPCommands.CMD_CONNECT
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        var readBufferStrring = HEXTool.toHexString(sendBuffer)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.i("BluetoothLeCmdManager", "write   CMD:"+display)
        var timeOutIndex = 0
        var isTimeOut = false
        _responseBuffer = byteArrayOf()
        WRITE_BC!!.write( sendBuffer)
        while (_responseBuffer.isEmpty()){
            Thread.sleep(300)
            WRITE_BC!!.write( sendBuffer)

            if(timeOutIndex > 60){
                callback.invoke(_responseBuffer,false,true)
                isTimeOut = true
                return
            }
            timeOutIndex = timeOutIndex + 1
            Log.i("BluetoothLeCmdManager", "timeOutIndex :"+timeOutIndex + "   isTimeOut:"+isTimeOut)
        }

        Thread.sleep(1000)

        val isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, _responseBuffer)
        callback.invoke(_responseBuffer,isChecksum,isTimeOut)
    }

    fun sendCMD_GET_DEVICEID( callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_DEVICEID
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write(sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(_responseBuffer,isChecksum)
    }

    fun sendCMD_UPDATE_BIN(cmd: ISPCommands, sendByteArray:ByteArray, startAddress:UInt, callback: ((ByteArray?, Int) -> Unit)) {

        if(cmd != ISPCommands.CMD_UPDATE_APROM && cmd != ISPCommands.CMD_UPDATE_DATAFLASH){
            return
        }

        var firstData = byteArrayOf()//第一個cmd 為 48 byte
        for (i in 0..47){
            firstData = firstData + sendByteArray[i]
        }
        var remainDataList: List<ByteArray> = listOf()
        val remainData = sendByteArray.copyOfRange(48,sendByteArray.lastIndex+1)//第一個cmd 為 56 byte
//        val remainData = ByteArray(sendByteArray.size - 48)//第一個cmd 為 56 byte
        var index = 0
        var dataArray = byteArrayOf()
        for (byte in remainData){
            dataArray = dataArray + byte
            index = index + 1

            if(index == 56){
                index = 0
                remainDataList = remainDataList + dataArray.clone()
                dataArray = byteArrayOf()
            }
        }
        if(dataArray.isNotEmpty()){
            //還有剩
            for(i in dataArray.size+1..56){
                dataArray = dataArray + 0x00
            }

            if(dataArray.size == 56){
                remainDataList = remainDataList + dataArray.clone()
            }

        }
        Log.i("ISPManager", "CMD_UPDATE   CMD:"+cmd.toString()+ "  startAddress:"+startAddress+"  size:"+sendByteArray.size+"  allPackNum:"+dataArray.size+1)
        var sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd,
            ISPManager.packetNumber, startAddress , sendByteArray.size , firstData , true)

        var readBuffer = this.write(sendBuffer)

        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, 0) //5% 起跳

        if(isChecksum != true){
            callback.invoke(readBuffer, -1)
            return
        }

        for (i in 0..remainDataList.size-1){

            sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd,ISPManager.packetNumber, startAddress , sendByteArray.size , remainDataList[i] , false)
            readBuffer = this.write(sendBuffer)
            isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

            if(isChecksum != true){
                callback.invoke(readBuffer, -1)
                return
            }

            callback.invoke(readBuffer, (i.toDouble() / remainDataList.size * 100).toInt())
        }
        callback.invoke(readBuffer, 100)
    }


    fun sendCMD_ERASE_ALL( callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_ERASE_ALL
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)

        val readBuffer = this.write(sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_READ_CONFIG( callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_READ_CONFIG
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write(sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer)
    }

    fun sendCMD_GET_FWVER( callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_FWVER
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write(sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_RUN_APROM( callback: ((Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_RUN_APROM
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        WRITE_BC!!.write( sendBuffer)
        WRITE_BC!!.write( sendBuffer)

        callback.invoke(true)
    }

    fun sendCMD_UPDATE_CONFIG( config0: UInt, config1: UInt, config2: UInt, config3: UInt, callback: ((ByteArray?) -> Unit)) {


            //config＿1  先寫死
            val cmd = ISPCommands.CMD_UPDATE_CONFIG
            val sendBuffer = ISPCommandTool.toUpdataCongigeCMD(config0, config1, config2,config3,
                ISPManager.packetNumber
            )
            val readBuffer = this.write(sendBuffer)
            var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

            callback.invoke(readBuffer)


    }

    fun sendCMD_SYNC_PACKNO( callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_SYNC_PACKNO
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write(sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)
    }





}