package com.nuvoton.nuisptool_android.ISPTool

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
@RequiresApi(Build.VERSION_CODES.KITKAT)
object ConfigManager {

    private var TAG = "ConfigManager"
    lateinit var CONFIG_JSON_DATA :IspConfig
    private var CONFIG_LIST = ArrayList<SubConfig>()
    var BIT_ARRAY_0 = arrayOfNulls<String>(32)
    var BIT_ARRAY_1 = arrayOfNulls<String>(32)
    var BIT_ARRAY_2 = arrayOfNulls<String>(32)
    var BIT_ARRAY_3 = arrayOfNulls<String>(32)




    fun readConfigFromFile(context: Context, series:String, jsonIndex: String?): Boolean {

        var binpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
        binpath += "/ISPTool/Config"
        val file = File(binpath, series + ".json")

        if(file.exists()){
            CONFIG_JSON_DATA = Json.decodeFromString(IspConfig.serializer(), file.readText())
            return true
        }

        if(jsonIndex == null){
            return false
        }

        val filename = jsonIndex.toLowerCase()
        val resId = context.resources.getIdentifier(filename, "raw", context.packageName)
        if(resId == 0 ) { return false }
        val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        CONFIG_JSON_DATA = Json.decodeFromString(IspConfig.serializer(), json)

        return true
    }

    fun getAllConfigList(): ArrayList<SubConfig> {

        CONFIG_LIST.clear()

        for ( configArray in CONFIG_JSON_DATA.subConfigSets){

            if(configArray.isEnable == true){
                for (c in configArray.subConfigs){
                    CONFIG_LIST.add(c)
                }
            }

        }
        return CONFIG_LIST
    }

    /**
     * 將readBuffer更新到 CONFIG_0123_LIST BIT_ARRAY_0123
     */
    fun initReadBufferToConfigData(readBuffer:ByteArray){
        val config_0_Array :  ByteArray = byteArrayOf(readBuffer[8])+byteArrayOf(readBuffer[9])+byteArrayOf(readBuffer[10])+byteArrayOf(readBuffer[11])
        val config_1_Array :  ByteArray = byteArrayOf(readBuffer[12])+byteArrayOf(readBuffer[13])+byteArrayOf(readBuffer[14])+byteArrayOf(readBuffer[15])
        val config_2_Array :  ByteArray = byteArrayOf(readBuffer[16])+byteArrayOf(readBuffer[17])+byteArrayOf(readBuffer[18])+byteArrayOf(readBuffer[19])
        val config_3_Array :  ByteArray = byteArrayOf(readBuffer[20])+byteArrayOf(readBuffer[21])+byteArrayOf(readBuffer[22])+byteArrayOf(readBuffer[23])

        //BIT_ARRAY_0 ------------------------------------------------------------------------------
        var config0UInt  = HEXTool.bytesToUInt(config_0_Array)
        var config0Binary = HEXTool.UIntTo32bitBinary(config0UInt)
        val config0Array = arrayOfNulls<String>(32)
        for ( i in 0..31){
            config0Array.set(i, config0Binary.get(i).toString())
        }
        BIT_ARRAY_0 = config0Array

        for (i in 0..31){ //更新到Data
            for( config in CONFIG_JSON_DATA.subConfigSets[0].subConfigs){
                if(config.offset == i){
                    var values = ""
                    for (l in 1..config.length){
                        values = values + config0Array[i+l-1].toString()
                    }
                    config.values = values
                }
            }
        }

        //BIT_ARRAY_1 ------------------------------------------------------------------------------
        var config1UInt  = HEXTool.bytesToUInt(config_1_Array)
        var config1Binary = HEXTool.UIntTo32bitBinary(config1UInt)
        val config1Array = arrayOfNulls<String>(32)
        for ( i in 0..31){
            config1Array.set(i, config1Binary.get(i).toString())
        }
        BIT_ARRAY_1 = config1Array

        for (i in 0..31){
            for( config in CONFIG_JSON_DATA.subConfigSets[1].subConfigs){
                if(config.offset == i){
                    var values = ""
                    for (l in 1..config.length){
                        values = values + config1Array[i+l-1].toString()
                    }
                    config.values = values
                }
            }
        }
        //BIT_ARRAY_2 ------------------------------------------------------------------------------
        var config2UInt  = HEXTool.bytesToUInt(config_2_Array)
        var config2Binary = HEXTool.UIntTo32bitBinary(config2UInt)
        val config2Array = arrayOfNulls<String>(32)
        for ( i in 0..31){
            config2Array.set(i, config2Binary.get(i).toString())
        }
        BIT_ARRAY_2 = config2Array

        for (i in 0..31){
            for( config in CONFIG_JSON_DATA.subConfigSets[2].subConfigs){
                if(config.offset == i){
                    var values = ""
                    for (l in 1..config.length){
                        values = values + config2Array[i+l-1].toString()
                    }
                    config.values = values
                }
            }
        }
        //BIT_ARRAY_3 ------------------------------------------------------------------------------
        var config3UInt  = HEXTool.bytesToUInt(config_3_Array)
        var config3Binary = HEXTool.UIntTo32bitBinary(config3UInt)
        val config3Array = arrayOfNulls<String>(32)
        for ( i in 0..31){
            config3Array.set(i, config3Binary.get(i).toString())
        }
        BIT_ARRAY_3 = config3Array

        for (i in 0..31){
            for( config in CONFIG_JSON_DATA.subConfigSets[3].subConfigs){
                if(config.offset == i){
                    var values = ""
                    for (l in 1..config.length){
                        values = values + config3Array[i+l-1].toString()
                    }
                    config.values = values
                }
            }
        }

        Log.i(TAG,"Read Config Binary:" + config0Binary+" , "+ config1Binary+" , "+ config2Binary+" , "+ config3Binary)
    }

    /**
     * 回傳 四個 config UInt
     */
    fun getConfigs(callback:(Config0:UInt,Config1:UInt,Config2:UInt,Config3:UInt) -> Unit){
        val array0 = BIT_ARRAY_0
        val array1 = BIT_ARRAY_1
        val array2 = BIT_ARRAY_2
        val array3 = BIT_ARRAY_3
        //config 0 ---------------------------------------------------------------------------------
        for (i in 0..31){
            for(config in CONFIG_JSON_DATA.subConfigSets[0].subConfigs){
                if(config.offset == i){
                    for(l in 1..config.length){
                        Log.i("IspConfig","config.length = "+config.length+" , l = " + l +" set> "+(i+l-1)+" values = "+config.values.substring(l-1,l))
                        array0.set(i+l-1,config.values.substring(l-1,l))
                    }
                }
            }
        }
        var config0Binary = ""
        for (a in array0){
            config0Binary = config0Binary + a
        }
        val config0Uint = HEXTool.bit32BinaryToUInt(config0Binary)

        //config 1 ---------------------------------------------------------------------------------
        for (i in 0..31){
            for(config in CONFIG_JSON_DATA.subConfigSets[1].subConfigs){
                if(config.offset == i){
                    for(l in 1..config.length){
                        Log.i("IspConfig","config.length = "+config.length+" , l = " + l +" set> "+(i+l-1)+" values = "+config.values.substring(l-1,l))
                        array1.set(i+l-1,config.values.substring(l-1,l))
                    }
                }
            }
        }
        var config1Binary = ""
        for (a in array1){
            config1Binary = config1Binary + a
        }
        val config1Uint = HEXTool.bit32BinaryToUInt(config1Binary)

        //config 2 ---------------------------------------------------------------------------------
        for (i in 0..31){
            for(config in CONFIG_JSON_DATA.subConfigSets[2].subConfigs){
                if(config.offset == i){
                    for(l in 1..config.length){
//                        Log.i("IspConfig","config.length = "+config.length+" , l = " + l +" set> "+(i+l-1)+" values = "+config.values.substring(l-1,l))
                        array2.set(i+l-1,config.values.substring(l-1,l))
                    }
                }
            }
        }
        var config2Binary = ""
        for (a in array2){
            config2Binary = config2Binary + a
        }
        val config2Uint = HEXTool.bit32BinaryToUInt(config2Binary)

        //config 3 ---------------------------------------------------------------------------------
        for (i in 0..31){
            for(config in CONFIG_JSON_DATA.subConfigSets[3].subConfigs){
                if(config.offset == i){
                    for(l in 1..config.length){
                        Log.i("IspConfig","config.length = "+config.length+" , l = " + l +" set> "+(i+l-1)+" values = "+config.values.substring(l-1,l))
                        array3.set(i+l-1,config.values.substring(l-1,l))
                    }
                }
            }
        }
        var config3Binary = ""
        for (a in array3){
            config3Binary = config3Binary + a
        }
        val config3Uint = HEXTool.bit32BinaryToUInt(config3Binary)

       // ---------------------------------------------------------------------------------
        Log.i(TAG,"Config Binary:" + config0Binary+" , "+ config1Binary+" , "+ config2Binary+" , "+ config3Binary)
        Log.i(TAG,"Config Uint:" + config0Uint+" , "+config1Uint+" , "+config2Uint+" , "+config3Uint)
        Log.i(TAG, "Config HexString:" + HEXTool.toHexString(HEXTool.UIntTo4Bytes(config0Uint))+" , "+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config1Uint))+" , "+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config2Uint))+" , "+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config3Uint)))
        Log.i(TAG, "Config DisplayHex:"
                + HEXTool.toHexString(HEXTool.UIntTo4Bytes(config0Uint)[3])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config0Uint)[2])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config0Uint)[1])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config0Uint)[0])+" , "
                + HEXTool.toHexString(HEXTool.UIntTo4Bytes(config1Uint)[3])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config1Uint)[2])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config1Uint)[1])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config1Uint)[0])+" , "
                + HEXTool.toHexString(HEXTool.UIntTo4Bytes(config2Uint)[3])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config2Uint)[2])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config2Uint)[1])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config2Uint)[0])+" , "
                + HEXTool.toHexString(HEXTool.UIntTo4Bytes(config3Uint)[3])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config3Uint)[2])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config3Uint)[1])+HEXTool.toHexString(HEXTool.UIntTo4Bytes(config3Uint)[0]))

        callback.invoke(config0Uint,config1Uint,config2Uint,config3Uint)
    }
}

