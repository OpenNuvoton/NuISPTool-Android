package com.nuvoton.nuisptool_android.Util

import java.util.regex.Pattern

object HEXTool{

//    fun set4BytesToBuffer(buffer: ByteArray, offset: Int, data: Int) {
//        buffer[offset + 0] = (data shr 0).toByte()
//        buffer[offset + 1] = (data shr 8).toByte()
//        buffer[offset + 2] = (data shr 16).toByte()
//        buffer[offset + 3] = (data shr 24).toByte()
//    }
//
//    fun add4BytesToBuffer(buffer: ByteArray, data: Int): ByteArray {
//        val addBytes = byteArrayOf((data shr 0).toByte(),(data shr 8).toByte(),(data shr 16).toByte(),(data shr 24).toByte())
//        var sendBytes = byteArrayOf()
//        sendBytes = buffer + addBytes
//
//        return sendBytes
//    }

    fun UIntTo4Bytes(value: UInt): ByteArray {
        val addBytes = byteArrayOf((value shr 0).toByte(),(value shr 8).toByte(),(value shr 16).toByte(),(value shr 24).toByte())
        return addBytes
    }

    fun UIntTo32bitBinary(value: UInt): String {
        return value.toString(2).padStart(32, '0').reversed()
    }
    fun bit32BinaryToUInt(value: String): UInt {
        return value.reversed().toUInt(2)
    }

    fun bytesToUInt(bytes: ByteArray): UInt {
        var result:UInt = 0u
        var shift = 0
        for (byte in bytes) {
            result = result or ((byte.toUByte().toUInt() and 0xFFu)  shl shift)
            shift += 8
        }
        return result
    }

    fun bytesToInt(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() shl 8 * i)
        }
        return result
    }

    fun ByteArray.getUIntAt(idx: Int) =
        ((this[idx].toUInt() and 0xFFu) shl 24) or
                ((this[idx + 1].toUInt() and 0xFFu) shl 16) or
                ((this[idx + 2].toUInt() and 0xFFu) shl 8) or
                (this[idx + 3].toUInt() and 0xFFu)


    fun toHexString(byteArray: ByteArray) : String {
        return byteArray.joinToString("") {
            java.lang.String.format("%02X", it)
        }
    }

    fun toHexString(byte: Byte) : String {
        return String.format("%02X", byte)
    }

    fun toHex16String(int:Int): String {
        return int.toString(16).padStart(8, '0')
    }

    fun toDisPlayString(byteArrayString: String): String{
        val array = byteArrayString.split("")
        var display = ""
        for(i in 0..array.count()-1){

            display = display + array[i]
            if(i%8==0){
                display = display + " , "
            }
        }
        return display
    }

    fun ByteArray.to2HexString(): String = asUByteArray().joinToString(" ") {
        it.toString(radix = 16).toUpperCase().padStart(2, '0')
    }

    fun isHexString(inputText: String):Boolean{
        val r1 = Pattern.compile("^\\p{XDigit}+$")
        return r1.matcher(inputText).matches()
    }
}