package com.nuvoton.nuisptool_android.ISPTool

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import com.hoho.android.usbserial.driver.*
import com.nuvoton.nuisptool_android.Bluetooth.BluetoothLeCmdManager
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import com.nuvoton.nuisptool_android.WiFi.SocketCmdManager
import kotlin.concurrent.thread
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.lang.Exception


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object SerialManager {

    public lateinit var SerialDriver: UsbSerialDriver
    public lateinit var SerialPort: UsbSerialPort
    private val baudRate = 115200
    private val dataBits = 8
    private val stopBits = UsbSerialPort.STOPBITS_1
    private val parity = UsbSerialPort.PARITY_NONE
    private var bufferSize = 64
    private var _responseBuffer:ByteArray = byteArrayOf()
    private var mSerialIoManager : SerialInputOutputManager? = null//输入输出管理器（本质是一个Runnable）


    fun doSerialConnect(usbDevice: UsbDevice):Boolean {
        // Probe for our custom CDC devices, which use VID 0x1234
        // and PIDS 0x0001 and 0x0002.

        val customTable = ProbeTable()
        customTable.addProduct(
            usbDevice.vendorId,
            usbDevice.productId,
            CdcAcmSerialDriver::class.java)

        val prober = UsbSerialProber(customTable)
        val drivers = prober.findAllDrivers(OTGManager.USBManager)

        // Open a connection to the first available driver.
        val driver = drivers[0]

        val connection = OTGManager.USBManager.openDevice(driver.device)
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return false
        }

        SerialPort  = driver.ports[0] // Most devices have just one port (port 0)

        SerialPort.open(connection)
        SerialPort.setParameters(baudRate,dataBits,stopBits,parity)
        if(SerialPort.isOpen){
            mSerialIoManager = SerialInputOutputManager(SerialPort, mListener)
            mSerialIoManager!!.start()
            return true
        }
        return false
    }

    private val mListener: SerialInputOutputManager.Listener =
        object : SerialInputOutputManager.Listener {
            override fun onRunError(e: Exception) {
                Log.d("SerialManager", "Runner stopped.")
            }

            override fun onNewData(data: ByteArray) {

                var readBufferStrring = HEXTool.toHexString(data)
                var display = HEXTool.toDisPlayString(readBufferStrring)
                Log.d("SerialManager", "notif:" + display )

                if(data.size<64){

                    _responseBuffer = _responseBuffer + data
                    return
                }

               _responseBuffer = data

            }
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
        Log.i("SerialManager", "CMD_UPDATE   CMD:"+cmd.toString()+"  size:"+sendByteArray.size+"  allPackNum:"+dataArray.size+1)
        var sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd,
            ISPManager.packetNumber, startAddress , sendByteArray.size , firstData , true)
        val readBuffer = this.write( sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, 0) //5% 起跳

        if(isChecksum != true){
            callback.invoke(readBuffer, -1)
            return
        }

        for (i in 0..remainDataList.size-1){
            sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd,
                ISPManager.packetNumber, startAddress , sendByteArray.size , remainDataList[i] , false)
            val readBuffer = this.write( sendBuffer)
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

        val readBuffer = this.write( sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_READ_CONFIG( callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_READ_CONFIG
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write( sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer)
    }

    fun sendCMD_GET_FWVER(callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_FWVER
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write( sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_RUN_APROM( callback: ((Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_RUN_APROM
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        SerialPort.write(sendBuffer, 0)

        callback.invoke(true)
    }

    fun sendCMD_UPDATE_CONFIG(config0: UInt,config1: UInt,config2: UInt,config3: UInt, callback: ((ByteArray?) -> Unit)) {

        sendCMD_ERASE_ALL() { readArray, isChackSum ->

            if (isChackSum != true) return@sendCMD_ERASE_ALL

            //config＿1  先寫死
            val cmd = ISPCommands.CMD_UPDATE_CONFIG
            val sendBuffer = ISPCommandTool.toUpdataCongigeCMD(config0, config1, config2,config3,
                ISPManager.packetNumber
            )
            val readBuffer = this.write( sendBuffer)
            var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

            callback.invoke(readBuffer)
        }

    }

    fun sendCMD_SYNC_PACKNO( callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_SYNC_PACKNO
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write( sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)
    }

    fun sendCMD_CONNECT( callback: ((ByteArray?, Boolean,isTimeout:Boolean) -> Unit)) {

//        ISPManager.packetNumber = (0x00000001).toUInt()
//        val cmd = ISPCommands.CMD_CONNECT
//        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
////        this.write(usbDevice, sendBuffer)
////        val readBuffer = this.read(usbDevice)
//        thread {
//            this.executeWriteRead(sendBuffer,100, callback = { readBuffer,isTimeout ->
//                var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)
//                callback.invoke(readBuffer, isChecksum,isTimeout)
//            })
//        }



        ISPManager.packetNumber = (0x00000001).toUInt()

        val cmd = ISPCommands.CMD_CONNECT
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        var readBufferStrring = HEXTool.toHexString(sendBuffer)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.i("SerialManager", "write   CMD:"+display)
        var timeOutIndex = 0
        var isTimeOut = false

        _responseBuffer = byteArrayOf()
        SerialPort.write(sendBuffer, 0)

        while (_responseBuffer.isEmpty()){
            Thread.sleep(300)
            SerialPort.write(sendBuffer, 0)

            if(timeOutIndex > 60){
                callback.invoke(_responseBuffer,false,true)
                isTimeOut = true
                return
            }
            timeOutIndex = timeOutIndex + 1
            Log.i("SerialManager", "timeOutIndex :"+timeOutIndex + "   isTimeOut:"+isTimeOut)
        }

        Thread.sleep(500)

        val isChecksum = ISPManager.isChecksum_PackNo(sendBuffer,_responseBuffer)
        callback.invoke(_responseBuffer,isChecksum,isTimeOut)
    }

    fun sendCMD_GET_DEVICEID( callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_DEVICEID
        val sendBuffer = ISPCommandTool.toCMD(cmd, ISPManager.packetNumber)
        val readBuffer = this.write( sendBuffer)
        var isChecksum = ISPManager.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer,isChecksum)
    }

    fun write(cmdArray: ByteArray):ByteArray{

        _responseBuffer = byteArrayOf()
        SerialPort.write(cmdArray, 0)

        var readBufferStrring = HEXTool.toHexString(cmdArray)
        var display = HEXTool.toDisPlayString(readBufferStrring)
        Log.i("SerialManager", "write   CMD:"+display)

        while (_responseBuffer.size < 64){
            Thread.sleep(10)
        }

        return _responseBuffer
    }

//    fun read(): ByteArray?{
//        if(SerialPort == null){
//            return null
//        }
//        val readBuffer = ByteArray(bufferSize)
//        val len = SerialPort.read(readBuffer, 0)
////        SerialPort.read(readBuffer, 0)
//        var readBufferStrring = HEXTool.toHexString(readBuffer)
//        var display = HEXTool.toDisPlayString(readBufferStrring)
//        Log.i("SerialManager", "SerialPort.read len:"+len)
//        Log.i("SerialManager", "SerialPort.read readBuffer:"+display)
//
//        tempReadBuffer = readBuffer
//       return null
//    }

    @SuppressLint("NewApi")
    fun executeWriteRead( cmdArray: ByteArray,timeoutIndex:Int,callback: (ByteArray?,isTimeout:Boolean) -> Unit){

        if(SerialPort == null){
            return
        }

        if(ISPManager.interfaceType  != NulinkInterfaceType.UART){
            return
        }

        var index = 0
        var isRead = -1
        var readBuffer = ByteArray(64)

        while (isRead != 64) {

//            cmdArray.set(1, ISPManager.interfaceType.value)//NULINK

            val sendBuffer = cmdArray
            var readBufferStrring = HEXTool.toHexString(sendBuffer)
            var display = HEXTool.toDisPlayString(readBufferStrring)

            readBuffer = ByteArray(64)

            Thread.sleep(300)

            val isWrite = SerialPort.write(sendBuffer, 0)
            Log.i("SerialManager", "isWrite=" + isWrite + "    ,sendBuffer:  " + display)
            Thread.sleep(100)
            isRead = SerialPort.read(readBuffer, 100)
            readBufferStrring = HEXTool.toHexString(readBuffer)
            display = HEXTool.toDisPlayString(readBufferStrring)
            Log.i("SerialManager", "isRead=" + isRead + "    ,readBuffer:  " + display)

            if(index >= timeoutIndex){
                isRead = 64
                callback.invoke(null,true)
                return
            }else{
                index = index + 1
                Log.i("SerialManager", "index=" + index )
            }
        }
        callback.invoke(readBuffer,false)
    }
}