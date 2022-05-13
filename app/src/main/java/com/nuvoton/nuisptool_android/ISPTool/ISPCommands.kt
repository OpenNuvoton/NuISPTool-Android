package com.nuvoton.nuisptool_android.ISPTool

import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import java.math.BigInteger
enum class ISPCommands constructor(val value: UInt){
    //value 為 Int
    CMD_REMAIN_PACKET    ((0x00000000).toUInt()),
    CMD_UPDATE_APROM	 ((0x000000A0).toUInt()),
    CMD_UPDATE_CONFIG    ((0x000000A1).toUInt()),
    CMD_READ_CONFIG      ((0x000000A2).toUInt()),
    CMD_ERASE_ALL 	     ((0x000000A3).toUInt()),
    CMD_SYNC_PACKNO		 ((0x000000A4).toUInt()),
    CMD_GET_FWVER        ((0x000000A6).toUInt()),
    CMD_GET_DEVICEID     ((0x000000B1).toUInt()),
    CMD_UPDATE_DATAFLASH ((0x000000C3).toUInt()),
    CMD_RUN_APROM		 ((0x000000AB).toUInt()),
    CMD_RUN_LDROM		 ((0x000000AC).toUInt()),
    CMD_RESET			 ((0x000000AD).toUInt()),
    CMD_CONNECT			 ((0x000000AE).toUInt()),
    CMD_RESEND_PACKET    ((0x000000FF).toUInt()),
    // Support SPI Flash
    CMD_ERASE_SPIFLASH   ((0x000000D0).toUInt()),
    CMD_UPDATE_SPIFLASH  ((0x000000D1).toUInt()),
}

object ISPCommandTool {

    private var TAG = "ISPCommandTool"

    fun toCMD(CMD:ISPCommands,packetNumber:UInt):ByteArray{

        val cmdBytes = HEXTool.UIntTo4Bytes(CMD.value)
        //= byteArrayOf((CMD.value shr 0).toByte(),(CMD.value shr 8).toByte(),(CMD.value shr 16).toByte(),(CMD.value shr 24).toByte())
        val packetNumberBytes = HEXTool.UIntTo4Bytes(packetNumber)
            //byteArrayOf((packetNumber shr 0).toByte(),(packetNumber shr 8).toByte(),(packetNumber shr 16).toByte(),(packetNumber shr 24).toByte())
        val noneBytes =
            byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+
                    byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+
                    byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+
                    byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)

        var sendBytes = byteArrayOf()
        sendBytes = cmdBytes + packetNumberBytes + noneBytes

        return sendBytes
    }

    /**
     * 燒錄Bin 支援 APROM ＆ DataFlash
     */
    fun toUpdataBin_CMD(cmd: ISPCommands, packetNumber: UInt, startAddress: UInt, size: Int, data: ByteArray,isFirst:Boolean): ByteArray {

        var sendBytes = byteArrayOf()
        if(isFirst == true){
            //第一次CMD
            val cmdBytes = HEXTool.UIntTo4Bytes(cmd.value)
            val packetNumberBytes = HEXTool.UIntTo4Bytes(packetNumber)
            val address = HEXTool.UIntTo4Bytes(startAddress)
            val totalSize = HEXTool.UIntTo4Bytes( size.toUInt() )

            sendBytes = cmdBytes + packetNumberBytes + address + totalSize + data
            return sendBytes

        }

        //剩下的CMD
        val cmdBytes = HEXTool.UIntTo4Bytes((0x00000000).toUByte().toUInt())
        val packetNumberBytes = HEXTool.UIntTo4Bytes(packetNumber)
        sendBytes = cmdBytes + packetNumberBytes + data

        return sendBytes
    }

    /**
     * 燒錄 Congige 的 CMD
     */
    fun toUpdataCongigeCMD(config_0:UInt,config_1:UInt,config_2:UInt,config_3:UInt,packetNumber:UInt):ByteArray{

        val cmdBytes = HEXTool.UIntTo4Bytes(ISPCommands.CMD_UPDATE_CONFIG.value)
        val packetNumberBytes = HEXTool.UIntTo4Bytes(packetNumber)
        val config0 = HEXTool.UIntTo4Bytes(config_0)
        val config1 = HEXTool.UIntTo4Bytes(config_1)
        val config2 = HEXTool.UIntTo4Bytes(config_2)
        val config3 = HEXTool.UIntTo4Bytes(config_3)
        val noneBytes =
            byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+
                    byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)+
                    byteArrayOf(0x00,0x00,0x00,0x00)+byteArrayOf(0x00,0x00,0x00,0x00)

        var sendBytes = byteArrayOf()
        sendBytes = cmdBytes + packetNumberBytes + config0 + config1 +config2 + config3 + noneBytes
        return sendBytes
    }

    /**
     * 計算Checksum應該為？
     */
    fun toChecksumBySendBuffer(sendBuffer:ByteArray):UInt{

        sendBuffer.set(1,0x00) //將不同interface所偷改的修正回來
        var uint = 0u
        for(byte in sendBuffer) {
            uint = uint + byte.toUByte().toUInt()
        }
        return uint
    }

    fun toChecksumByReadBuffer(readBuffer:ByteArray):UInt{

       var bytes = byteArrayOf(readBuffer[0])+byteArrayOf(readBuffer[1])+byteArrayOf(readBuffer[2])+byteArrayOf(readBuffer[3])

        return HEXTool.bytesToUInt(bytes)
    }

    fun toPackNo(readBuffer:ByteArray):UInt{
        val resultPackNoByteArray :  ByteArray = byteArrayOf(readBuffer[4])+byteArrayOf(readBuffer[5])+byteArrayOf(readBuffer[6])+byteArrayOf(readBuffer[7])
        return HEXTool.bytesToUInt(resultPackNoByteArray)
    }

    fun toDeviceID(readBuffer:ByteArray):String{
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[11])+byteArrayOf(readBuffer[10])+byteArrayOf(readBuffer[9])+byteArrayOf(readBuffer[8])
        return  HEXTool.toHexString(deviceIDArray)
    }

    fun toFirmwareVersion(readBuffer:ByteArray):String{
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[11])+byteArrayOf(readBuffer[10])+byteArrayOf(readBuffer[9])+byteArrayOf(readBuffer[8])
        return  HEXTool.toHexString(deviceIDArray)
    }

    fun toDisplayComfig0(readBuffer:ByteArray):String{
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[11])+byteArrayOf(readBuffer[10])+byteArrayOf(readBuffer[9])+byteArrayOf(readBuffer[8])
        return  HEXTool.toHexString(deviceIDArray)
    }

    fun toDisplayComfig1(readBuffer:ByteArray):String{
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[15])+byteArrayOf(readBuffer[14])+byteArrayOf(readBuffer[13])+byteArrayOf(readBuffer[12])
        return  HEXTool.toHexString(deviceIDArray)
    }

    fun toDisplayComfig2(readBuffer:ByteArray):String{
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[19])+byteArrayOf(readBuffer[18])+byteArrayOf(readBuffer[17])+byteArrayOf(readBuffer[16])
        return  HEXTool.toHexString(deviceIDArray)
    }

    fun toDisplayComfig3(readBuffer:ByteArray):String{
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[23])+byteArrayOf(readBuffer[22])+byteArrayOf(readBuffer[21])+byteArrayOf(readBuffer[20])
        return  HEXTool.toHexString(deviceIDArray)
    }

    fun toAPROMSize(readBuffer:ByteArray):Int {
        val deviceIDArray :  ByteArray = byteArrayOf(readBuffer[12])+byteArrayOf(readBuffer[13])+byteArrayOf(readBuffer[14])+byteArrayOf(readBuffer[15])
//        HEXTool.bytesToUInt(deviceIDArray).toInt()

        return  HEXTool.bytesToInt(deviceIDArray)
    }
}