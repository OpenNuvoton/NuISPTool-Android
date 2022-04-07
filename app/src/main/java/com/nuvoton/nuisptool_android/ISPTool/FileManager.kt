package com.nuvoton.nuisptool_android.ISPTool

import android.R.attr
import android.net.Uri
import android.provider.MediaStore
import android.R.attr.data
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.content.PackageManagerCompat
import com.nuvoton.nuisptool_android.Util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import androidx.core.content.PackageManagerCompat.LOG_TAG
import java.io.FileOutputStream
import java.lang.Exception


class FileData(){

    lateinit var uri:Uri
    lateinit var name:String
    lateinit var path:String
    lateinit var type:String
    lateinit var file: File
    lateinit var byteArray: ByteArray
}

class ChipData(){
    lateinit var chipInfo: ChipInfoData
    lateinit var chipPdid: ChipPdidData
}

@Serializable
data class ChipInfoData(
    var AP_size: String,
    var DF_size: String,
    val RAM_size: String,
    val DF_address: String,
    val LD_size: String,
    val PDID: String,
    val name: String,
    val note: String?) {

}

@Serializable
data class ChipPdidData(
    val name: String,
    val PID: String,
    val series: String,
    val note: String?,
    val jsonIndex: String?,
) {

}

object FileManager {

    private var TAG = "FileManager"

    var APROM_BIN:FileData? = null
    var DATAFLASH_BIN:FileData? = null
    private var _cid = ArrayList<ChipInfoData>()
    private var _cpd = ArrayList<ChipPdidData>()
    var CHIP_DATA :ChipData = ChipData()

    fun loadChipInfoFile(context: Context): String {

        var binpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        binpath += "/ISPTool"
        val file = File(binpath, "chip_info.json")

        if(file.exists()){ //如果有就讀取
            val json = file.readText()
            _cid = Json.decodeFromString<ArrayList<ChipInfoData>>(json)
            return json
        }

        val filename = "chip_info"
        val resId = context.resources.getIdentifier(filename, "raw", context.packageName)
        val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }

        _cid = Json.decodeFromString<ArrayList<ChipInfoData>>(json)

        return json
    }

    fun loadChipPdidFile(context: Context): String{

        var binpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        binpath += "/ISPTool"
        val file = File(binpath, "chip_pdid.json")

        if(file.exists()){ //如果有就讀取
            val json = file.readText()
            _cpd = Json.decodeFromString<ArrayList<ChipPdidData>>(json)
            return json
        }

        val filename = "chip_pdid"
        val resId = context.resources.getIdentifier(filename, "raw", context.packageName)
        val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }

        _cpd = Json.decodeFromString<ArrayList<ChipPdidData>>(json)

        return json
    }


    fun saveFile(context: Context,chipInfoFile : String,chipPdidFile : String){

        var binpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

        binpath += "/ISPTool"

        val file = File(binpath)
        if (!file.exists()) {
            file.mkdir()  //建立資料夾
        }

        //建立File
        val pdidFile = File(binpath, "chip_pdid.json")
        if (!pdidFile.exists()) { //如果檔案不存在，就寫入。
            var osw: FileOutputStream? = null
            try {
                osw = FileOutputStream(pdidFile, false)
                osw.write(chipPdidFile.toByteArray())
                osw!!.flush()
            } catch (e: Exception) {
            } finally {
                try {
                    osw!!.close()
                } catch (e: Exception) {
                }
            }
        }

        //建立File
        val infoFile = File(binpath, "chip_info.json")
        if (!infoFile.exists()) { //如果檔案不存在，就寫入。
            var osw: FileOutputStream? = null
            try {
                osw = FileOutputStream(infoFile, false)
                osw.write(chipInfoFile.toByteArray())
                osw!!.flush()
            } catch (e: Exception) {
            } finally {
                try {
                    osw!!.close()
                } catch (e: Exception) {
                }
            }
        }

        var configPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        configPath += "/ISPTool/Config"

        val configPathFile = File(configPath)

        if (!configPathFile.exists()) {
            configPathFile.mkdir()  //建立資料夾
        }
    }

    /**
     *  判斷外部資料夾是否可以寫入
     */
    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    fun getChipInfoByPDID(deviceID:String):ChipData?{
       val id = "0x"+ deviceID
        var hasInfo = false
        var hasPdid = false

        CHIP_DATA = ChipData()
        for (c in _cid){
           if( c.PDID == id){
               CHIP_DATA.chipInfo = c
               hasInfo = true
           }
        }
        for (c in _cpd){
            if( c.PID == id){
                CHIP_DATA.chipPdid = c
                hasPdid = true
            }
        }

        if(hasInfo == false || hasPdid == false){
            return null
        }
        return CHIP_DATA
    }


     fun getNameByUri(uri: Uri? , context:Context): String{

         if(uri == null){
             return "null"
         }

         var result: String = "N/A"

         var temp: Uri? = null
         //if uri is content
         if (uri.getScheme() != null && uri.getScheme().equals("content")) {
             val cursor =
                 context.getContentResolver().query(uri, null, null, null, null)
             cursor.use { cursor ->
                 if (cursor != null && cursor.moveToFirst()) {
                     //local filesystem
                     var index: Int = cursor.getColumnIndex("_data")
                     if (index == -1) //google drive
                         index = cursor.getColumnIndex("_display_name")
                     result = cursor.getString(index)
                     temp = Uri.parse(result)
                 }
             }
         }

         if (temp != null) {
             result = temp!!.path!!

             //get filename + ext of path
             val cut = result.lastIndexOf('/')
             if (cut != -1) result = result.substring(cut + 1)
         }
         return result
    }


}