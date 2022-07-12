package com.nuvoton.nuisptool_android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.android.material.textfield.TextInputEditText
import com.nuvoton.nuisptool_android.ISPTool.*
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import org.w3c.dom.Text
import java.math.BigInteger
import java.util.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ConfigActivity : AppCompatActivity() {

    private var TAG = "ConfigActivity"
    private lateinit var _recyclerView: RecyclerView
    private lateinit var _readConfigButton: Button
    private lateinit var _updataConfigButton: Button
    private lateinit var _backButton: Button
    private lateinit var _config0_hex_Text: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        getSupportActionBar()!!.setTitle("Nuvoton Android ISP Tool Setting Config")

        _config0_hex_Text = findViewById<View>(R.id.config_0_hex) as TextView
        _updataConfigButton = findViewById<View>(R.id.button_updata_config) as Button
        _updataConfigButton.setOnClickListener(onClickUpdataConfigButton)
        _readConfigButton = findViewById<View>(R.id.button_read_config) as Button
        _readConfigButton.setOnClickListener(onClickReadConfigButton)
        _backButton = findViewById<View>(R.id.button_back) as Button
        _backButton.setOnClickListener(onClickBackButton)
        _recyclerView = findViewById<View>(R.id.RecyclerView) as RecyclerView
        _recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        _recyclerView.adapter = recyclerAdapter(this)
        _recyclerView.setHasFixedSize(true)
        _recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        _recyclerView.adapter?.notifyDataSetChanged()

        this.readConfig() //進入先讀檔
    }

    override fun onResume() {
        super.onResume()

        OTGManager.setIsOnlineListener {
            if(it == false){ //裝置離線 回到首頁
                runOnUiThread {
                    val builder = android.app.AlertDialog.Builder(this)
                    builder.setMessage(R.string.Device_is_Disconnection)
                        .setNegativeButton(R.string.ok) { dialog, id ->

                            val intent = Intent(this, MainActivity::class.java)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)//銷燬目標Activity和它之上的所有Activity，重新建立目標Activity
                            this.startActivity(intent)

                        }
                    builder.show()
                }
            }
        }
    }

    /**
     * on Click onClickBackButton Button
     */
    private val onClickBackButton = View.OnClickListener {
        Log.i(TAG, "onClickBackButton")
       finish()
    }

    /**
     * on Click onClickReadConfig Button
     */
    private val onClickReadConfigButton = View.OnClickListener {
        Log.i(TAG, "onClickReadConfigButton")
        this.readConfig()
    }

    /**
     * on Click onClickUpdataConfig Button
     */
    private val onClickUpdataConfigButton = View.OnClickListener {
        Log.i(TAG, "onClickUpdataConfigButton")

        ConfigManager.getConfigs { Config0, Config1, Config2, Config3 ->

            runOnUiThread {

                val builder = android.app.AlertDialog.Builder(this)
                builder.setTitle("is implement the config burn ?")
                builder.setNeutralButton("Cancel") { _, _ -> }
                builder.setPositiveButton("ok") { _, _ ->

                    ISPManager.sendCMD_UPDATE_CONFIG(Config0,Config1,Config2,Config3,{

                    })

                }
                builder.show()
            }

        }

    }

    private fun readConfig() {
        Log.i(TAG, "fun - readConfig")

        ISPManager.sendCMD_READ_CONFIG( callback = {
            if (it == null) {
                return@sendCMD_READ_CONFIG
            }

            ConfigManager.initReadBufferToConfigData(it)
            _recyclerView.adapter?.notifyDataSetChanged()

        })
    }

    //----------------------------------------------------------------------------------------------
    /**
     * RecyclerView 用來動態顯示列表
     */
    class recyclerHolder(v: View) : RecyclerView.ViewHolder(v) {
        var text_name = v.findViewById<View>(R.id.text_name) as TextView
        var text_title = v.findViewById<View>(R.id.text_title) as TextView
        var text_info = v.findViewById<View>(R.id.text_info) as TextView
        var button_value = v.findViewById<View>(R.id.button_value) as Button
        var inputText = v.findViewById<View>(R.id.InputText) as TextInputEditText
    }

    class recyclerAdapter(context: Context) : RecyclerView.Adapter<recyclerHolder>() {

        var conText = context
        var allConfigList = ConfigManager.getAllConfigList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): recyclerHolder {
            return recyclerHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.config_row, parent, false)
            )//回傳 config_row Layout
        }

        override fun onBindViewHolder(holder: recyclerHolder, position: Int) {

            holder.inputText.visibility = View.GONE
            holder.button_value.visibility = View.VISIBLE

            var index = 0 //顯示與選擇
            val lunch: MutableList<String> = ArrayList()
            val list = allConfigList
            for (i in 0..list[position].options.count() - 1) {
                lunch.add( list[position].optionDescription[i])
                if (list[position].values.reversed() == list[position].options[i]) {
                    index = i
                }
            }

            //回傳每一個 row 的值
            holder.text_name.setText(list[position].name)
            holder.text_title.setText(list[position].description)
            holder.button_value.setText(list[position].options[index])
            holder.text_info.setText(list[position].optionDescription[index])

            holder.button_value.setOnClickListener {

                (conText as Activity).runOnUiThread {

                    val builder = android.app.AlertDialog.Builder(conText)
                    builder.setSingleChoiceItems(lunch.toTypedArray(), index, { _, which ->
                        index = which
                    })

                    builder.setPositiveButton("ok") { _, _ ->
                        list[position].values = list[position].options[index].reversed()
                        this.notifyDataSetChanged()
                    }
                    builder.show()
                }

            }

            if(list[position].options[0] == "input"){
                holder.inputText.visibility = View.VISIBLE
                holder.button_value.visibility = View.INVISIBLE
                holder.inputText.setOnEditorActionListener { textView, i, keyEvent ->
                    if(i == EditorInfo.IME_ACTION_DONE){
                        textView.clearFocus()
                        val imm = conText.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(textView.windowToken,0)
                        true
                    } else {
                        false
                    }
                }
            }

        }

        override fun getItemCount(): Int {
            return allConfigList.size
        }

    }
    //----------------------------------------------------------------------------------------------

}

