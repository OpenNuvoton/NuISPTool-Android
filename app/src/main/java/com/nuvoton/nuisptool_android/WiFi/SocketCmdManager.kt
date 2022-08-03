package com.nuvoton.nuisptool_android.WiFi

import android.os.Build
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.ISPTool.ISPCommandTool
import com.nuvoton.nuisptool_android.ISPTool.ISPCommands
import com.nuvoton.nuisptool_android.ISPTool.ISPManager
import com.nuvoton.nuisptool_android.ISPTool.OTGManager
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import java.io.IOException
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object SocketCmdManager {

    private var _responseBuffer:ByteArray = byteArrayOf()
    private var _tempBuffer:ByteArray = byteArrayOf()
    private var _thesameIndex = 0
    public var _isOnlineListener: ((Boolean) -> Unit)? = null



    public var ReadListener : ((ByteArray?) -> Unit) =  {


        if (it != null) {
            if(it.size<64){
                Log.d("SocketCmdManager", "read Value.size < 64  !!!"  )
                Log.d("SocketCmdManager", "read Value.size < 64  !!!"  )
                Log.d("SocketCmdManager", "read Value.size < 64  !!!"  )

                _responseBuffer = _responseBuffer + it
            }
            _responseBuffer = it
        }
        if(_tempBuffer.contentEquals(it)){
            _thesameIndex = _thesameIndex + 1
        }else{
            _thesameIndex = 0
        }
        _tempBuffer = it!!.clone()

        if(_thesameIndex > 5){
            Log.e("SocketCmdManager","斷線了")
            SocketManager.funTCPClientClose()
            if(_isOnlineListener!=null){
                _isOnlineListener!!.invoke(false)
            }
        }else{
            Log.e("read :", HEXTool.toHexString(it!!))
        }
    }

    fun initSocketStart(){

    }

    fun sendCMD_CONNECT( callback: ((ByteArray?, Boolean, isTimeout:Boolean) -> Unit)) {

        ISPManager.packetNumber = (0x00000001).toUInt()

        val cmd = ISPCommands.CMD_CONNECT
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        var readBufferStrring = HEXTool.toHexString(sendBuffer)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.i("SocketCmdManager", "write   CMD:"+display)
        var timeOutIndex = 0
        var isTimeOut = false
        _responseBuffer = byteArrayOf()

        SocketManager.funTCPClientSend(sendBuffer)
        while (_responseBuffer.isEmpty()){
            Thread.sleep(300)
            SocketManager.funTCPClientSend(sendBuffer)

            if(timeOutIndex > 60){
                callback.invoke(_responseBuffer,false,true)
                isTimeOut = true
                return
            }
            timeOutIndex = timeOutIndex + 1
            Log.i("SocketCmdManager", "timeOutIndex :"+timeOutIndex + "   isTimeOut:"+isTimeOut)
        }

        Thread.sleep(1000)

        val isChecksum = ISPManager.isChecksum_PackNo(sendBuffer,_responseBuffer)
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

            sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd,
                ISPManager.packetNumber, startAddress , sendByteArray.size , remainDataList[i] , false)
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
        thread { SocketManager.funTCPClientSend(sendBuffer) }
        thread { SocketManager.funTCPClientSend(sendBuffer) }

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

    private fun write(sendBuffer: ByteArray):ByteArray{


        this._responseBuffer = byteArrayOf()
        thread { SocketManager.funTCPClientSend(sendBuffer) }

        var readBufferStrring = HEXTool.toHexString(sendBuffer)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.i("SocketCmdManager", "write   CMD:"+display)

        while (this._responseBuffer.size < 64){
            Thread.sleep(10)
        }
        return this._responseBuffer
    }
}