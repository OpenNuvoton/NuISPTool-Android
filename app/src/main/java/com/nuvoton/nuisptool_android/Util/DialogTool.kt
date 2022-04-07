package com.nuvoton.nuisptool_android.Util

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import com.nuvoton.nuisptool_android.R

object DialogTool {

    private var _progressDialog: ProgressDialog? = null

    fun showAlertDialog(context: Context,message: String,isOkEnable: Boolean,isCancelEnable: Boolean,callback:((isClickOk:Boolean,isClickNo:Boolean) -> Unit)?) {

        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        if (isOkEnable) {
            builder.setPositiveButton(R.string.ok,DialogInterface.OnClickListener { dialog, id ->
                if(callback!=null)
                    callback.invoke(true,false)
                })
        }
        if (isCancelEnable) {
            builder.setNegativeButton(R.string.cancel,DialogInterface.OnClickListener { dialog, id ->
                if(callback!=null)
                callback.invoke(false,true)
            })
        }

        builder.show()
    }

    fun showInputAlertDialog(context: Context,title: String,message: String,callback:((text:String) -> Unit)?) {

        val item = LayoutInflater.from(context).inflate(R.layout.dialog_layout_edittext, null)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(item)
            .setNeutralButton(R.string.execute) { _, _ ->
                val editText = item.findViewById(R.id.InputConfigText) as TextInputEditText
                val name = editText.text.toString()
                if (TextUtils.isEmpty(name)) {
                    if(callback != null) {
                        callback.invoke("")
                    }
                } else {
                    if(callback != null) {
                        callback.invoke(name)
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                if(callback != null) {
                    callback.invoke("")
                }
            }
            .show()
    }

    fun showProgressDialog(context: Context,title:String,message: String,isHorizontalStyle:Boolean) {
        _progressDialog = ProgressDialog(context)
        _progressDialog!!.setTitle(title)
        _progressDialog!!.setMessage(message)
        if(isHorizontalStyle){_progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);}
        _progressDialog!!.setProgress(0);
        _progressDialog!!.setCancelable(false);
        _progressDialog!!.show()
    }

    fun upDataProgressDialog(progress:Int) {
        if(_progressDialog == null){
            return
        }
        _progressDialog!!.setProgress(progress);
    }

    fun dismissProgressDialog(){
        if(_progressDialog == null){
            return
        }
        _progressDialog!!.dismiss()
    }
}