package com.nuvoton.nuisptool_android.ISPTool

import android.annotation.SuppressLint
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.Util.DialogTool
import com.nuvoton.nuisptool_android.Util.Log
import com.nuvoton.nuisptool_android.Util.HEXTool
import kotlin.concurrent.thread

enum class NulinkInterfaceType constructor(val value: Byte) {

    USB    (0x00),
//    INTF_HID    (0x01),
    UART   (0x02),
    SPI    (0x03),
    I2C    (0x04),
    RS485  (0x05),
    CAN    (0x06),
}

object ISPManager {
    /**
     * Config 設定值
     */
    private var read_endpoint_index = 0
    private var write_endpoint_index = 1
    private var connect_interface_index = 0
    private var byteSize = 64
    private val forceClaim = true
    private val timeOut = 100
    private val isSearchLoop = false

    private var packetNumber: UInt = (0x00000005).toUInt()
    public var interfaceType : NulinkInterfaceType = NulinkInterfaceType.USB

    private var _readListener: ((ByteArray) -> Unit)? = null
    private var _byteArrayResultListener: ((ByteArray) -> Unit)? = null
//    fun setByteArrayRequestListener(callbacks: (ByteArray)->Unit){
//        _byteArrayResultListener = callbacks
//    }

    fun sendCMD_UPDATE_BIN(cmd: ISPCommands ,usbDevice: UsbDevice,sendByteArray:ByteArray,startAddress:UInt, callback: ((ByteArray?, Int) -> Unit)) {

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
        Log.i("ISPManager", "CMD_UPDATE   CMD:"+cmd.toString()+"  size:"+sendByteArray.size+"  allPackNum:"+dataArray.size+1)
        var sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd, packetNumber , startAddress , sendByteArray.size , firstData , true)
        this.write(usbDevice, sendBuffer)
        var readBuffer = this.read(usbDevice)
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, 0) //5% 起跳

        if(isChecksum != true){
            callback.invoke(readBuffer, -1)
            return
        }

        for (i in 0..remainDataList.size-1){
            sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd, packetNumber , startAddress , sendByteArray.size , remainDataList[i] , false)
            this.write(usbDevice, sendBuffer)
            readBuffer = this.read(usbDevice)
            isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)
            if(isChecksum != true){
                callback.invoke(readBuffer, -1)
                return
            }

            callback.invoke(readBuffer, (i.toDouble() / remainDataList.size * 100).toInt())
        }
        callback.invoke(readBuffer, 100)
    }

    fun sendCMD_ERASE_ALL(usbDevice: UsbDevice, callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_ERASE_ALL
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write(usbDevice, sendBuffer)
        val readBuffer = this.read(usbDevice)
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_READ_CONFIG(usbDevice: UsbDevice, callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_READ_CONFIG
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write(usbDevice, sendBuffer)
        val readBuffer = this.read(usbDevice)
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer)
    }

    fun sendCMD_GET_FWVER(usbDevice: UsbDevice, callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_FWVER
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write(usbDevice, sendBuffer)
        val readBuffer = this.read(usbDevice)
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_RUN_APROM(usbDevice: UsbDevice, callback: ((Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_RUN_APROM
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write(usbDevice, sendBuffer)

        callback.invoke(true)
    }

    fun sendCMD_UPDATE_CONFIG(usbDevice: UsbDevice,config0: UInt,config1: UInt,config2: UInt,config3: UInt, callback: ((ByteArray?) -> Unit)) {

        sendCMD_ERASE_ALL(usbDevice) { readArray, isChackSum ->
            if (isChackSum != true) return@sendCMD_ERASE_ALL

            //config＿1  先寫死
            val cmd = ISPCommands.CMD_UPDATE_CONFIG
            val sendBuffer = ISPCommandTool.toUpdataCongigeCMD(config0, config1, config2,config3, packetNumber)
            this.write(usbDevice, sendBuffer)

            val readBuffer = this.read(usbDevice)
            var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

            callback.invoke(readBuffer)
        }

    }

    fun sendCMD_SYNC_PACKNO(usbDevice: UsbDevice, callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_SYNC_PACKNO
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write(usbDevice, sendBuffer)
        val readBuffer = this.read(usbDevice)
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)
    }

    fun sendCMD_CONNECT(usbDevice: UsbDevice, callback: ((ByteArray?, Boolean,isTimeout:Boolean) -> Unit)) {

        this.packetNumber = (0x00000001).toUInt()
        val cmd = ISPCommands.CMD_CONNECT
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
//        this.write(usbDevice, sendBuffer)
//        val readBuffer = this.read(usbDevice)
        thread {
            this.executeWriteRead(usbDevice,sendBuffer,100, callback = { readBuffer,isTimeout ->
                var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)
                callback.invoke(readBuffer, isChecksum,isTimeout)
            })
        }

    }

    fun sendCMD_GET_DEVICEID(usbDevice: UsbDevice, callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_DEVICEID
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write(usbDevice, sendBuffer)
        val readBuffer = this.read(usbDevice)
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer,isChecksum)
    }

    private fun sendCMD(usbDevice: UsbDevice, cmd: ISPCommands) {

        thread {
            val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
            this.write(usbDevice, sendBuffer)
            val readBuffer = this.read(usbDevice)
            this.isChecksum_PackNo(sendBuffer, readBuffer)

        }
    }

    private fun isChecksum_PackNo(sendBuffer: ByteArray, readBuffer: ByteArray?): Boolean {

        if (readBuffer == null) {
            Log.i("isChecksum_PackNo", "readBuffer == null")
            return false
        }

        //checksum
        val checksum = ISPCommandTool.toChecksumBySendBuffer(sendBuffer)
        val resultChecksum = ISPCommandTool.toChecksumByReadBuffer(readBuffer)

        if (checksum != resultChecksum) {
            Log.i("isChecksum_PackNo", "checksum $checksum != resultChecksum $resultChecksum")
            return false
        }

        //checkPackNo
        val packNo = packetNumber + (0x00000001).toUInt()
        val resultPackNo = ISPCommandTool.toPackNo(readBuffer)

        if (packNo.toUInt() != resultPackNo) {
            Log.i("isChecksum_PackNo", "packNo $packNo != resultPackNo $resultPackNo")
            return false
        }
        packetNumber = packNo + (0x00000001).toUInt()
        Log.i(
            "isChecksum_PackNo",
            "packNo $packNo == resultPackNo $resultPackNo ,checksum $checksum == resultChecksum $resultChecksum"
        )
        return true
    }

    @SuppressLint("NewApi")
    private fun executeWriteRead(usbDevice: UsbDevice, cmdArray: ByteArray,timeoutIndex:Int,callback: (ByteArray?,isTimeout:Boolean) -> Unit){

        if(interfaceType != NulinkInterfaceType.USB){
            for( i in 0..usbDevice.interfaceCount - 1){
                if(usbDevice.getInterface(i).name != null && usbDevice.getInterface(i).name!!.indexOf("ISP")>-1){
                    connect_interface_index = i  //找到 ISP_HID InterFace
                }
            }
        }else{
            connect_interface_index = 0
        }

        var index = 0
        var isRead = -1
        val readBuffer = ByteArray(64)

        var intf = usbDevice.getInterface(connect_interface_index)
        var writePoint = intf.getEndpoint(write_endpoint_index)
        var readPoint = intf.getEndpoint(read_endpoint_index)
        var connection = OTGManager.USBManager.openDevice(usbDevice)
        connection.claimInterface(intf,forceClaim)

            while (isRead != 64) {

                cmdArray.set(1, interfaceType.value)//NULINK

                val sendBuffer = cmdArray
                var readBufferStrring = HEXTool.toHexString(sendBuffer)
                var display = HEXTool.toDisPlayString(readBufferStrring)

                val isWrite = connection.bulkTransfer(writePoint, sendBuffer, sendBuffer.size, 0)
                Log.i("ISPManager", "isWrite=" + isWrite + "    ,sendBuffer:  " + display)

                isRead = connection.bulkTransfer(readPoint, readBuffer,readBuffer.size,100)
                readBufferStrring = HEXTool.toHexString(readBuffer)
                display = HEXTool.toDisPlayString(readBufferStrring)
                Log.i("ISPManager", "isRead=" + isRead + "    ,readBuffer:  " + display)

                if(index >= timeoutIndex){
                    isRead = 64
                    callback.invoke(null,true)
                    return
                }else{
                    index = index + 1
                    Log.i("ISPManager", "index=" + index )
                }
            }

            callback.invoke(readBuffer,false)

    }

    @SuppressLint("NewApi")
    private fun read(usbDevice: UsbDevice): ByteArray? {

        if(interfaceType != NulinkInterfaceType.USB){
            for( i in 0..usbDevice.interfaceCount - 1){
                if(usbDevice.getInterface(i).name != null && usbDevice.getInterface(i).name!!.indexOf("ISP")>-1){
                    connect_interface_index = i
                }
            }
        }else{
            connect_interface_index = 0
        }


        usbDevice?.getInterface(connect_interface_index)?.also { intf ->
            intf.getEndpoint(read_endpoint_index)?.also { endpoint ->
                OTGManager.USBManager.openDevice(usbDevice)?.apply {
                    claimInterface(intf, forceClaim)

                    val readBuffer = ByteArray(64)

                    val i = bulkTransfer(
                        endpoint,
                        readBuffer,
                        readBuffer.size,
                        0
                    ) //do in another thread
                    val readBufferStrring = HEXTool.toHexString(readBuffer)
                    val display = HEXTool.toDisPlayString(readBufferStrring)
                    Log.i("ISPManager", "i="+i+"    ,readBuffer:  "+display)

                    return readBuffer
                }
            }
        }
        return null
    }

    @SuppressLint("NewApi")
    private fun write(usbDevice: UsbDevice, cmdArray: ByteArray) {


        //替換掉nu-link2的暗號
        cmdArray.set(1, interfaceType.value)
        if(interfaceType != NulinkInterfaceType.USB){
            for( i in 0..usbDevice.interfaceCount - 1){
                if(usbDevice.getInterface(i).name != null && usbDevice.getInterface(i).name!!.indexOf("ISP")>-1){
                    connect_interface_index = i  //找到 Nu-Link2 ISP
                }
            }
        }else{
            connect_interface_index = 0
        }

        usbDevice?.getInterface(connect_interface_index)?.also { intf ->

            intf.getEndpoint(write_endpoint_index)?.also { endpoint ->
                OTGManager.USBManager.openDevice(usbDevice)?.apply {
                    claimInterface(intf, forceClaim)
                    val sendBuffer = cmdArray
                    val readBufferStrring = HEXTool.toHexString(sendBuffer)
                    val display = HEXTool.toDisPlayString(readBufferStrring)

                    val i = bulkTransfer(
                        endpoint,
                        sendBuffer,
                        sendBuffer.size,
                        timeOut
                    ) //do in another thread

                    Log.i("ISPManager", "i="+i+"    ,writeBuffer: "+display)
                }
            }
        }
    }

}

