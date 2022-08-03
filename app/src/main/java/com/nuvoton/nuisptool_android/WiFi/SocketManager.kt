package com.nuvoton.nuisptool_android.WiFi

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.nuvoton.nuisptool_android.ISPTool.OTGManager
import com.nuvoton.nuisptool_android.Util.HEXTool
import com.nuvoton.nuisptool_android.Util.Log
import java.io.*
import java.net.*
import java.util.*
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object SocketManager {

    private var tcpClient = Socket()
    private val encodingFormat = "GBK"
    private var tcpClientConnectStatus = false
    private var tcpClientTargetServerIP = "192.168.4.1"
    private var tcpClientTargetServerPort = 520
    private var tcpClientOutputStream: OutputStream? = null
    private var tcpClientInputStreamReader: InputStreamReader? = null
    private val tcpClientReceiveBuffer = StringBuffer()

    fun setIsOnlineListener(callbacks: (Boolean) -> Unit) {
        SocketCmdManager._isOnlineListener = callbacks
    }

    //客户端连接
    //需要子线程
    public fun funTCPClientConnect(callback: ((Boolean, Exception?) -> Unit)) {
        if (tcpClientTargetServerIP.isEmpty()) {
            Log.e("目标服务端IP不能为空,否则无法连接", "")
            return
        }
        try {
            //一定要注意，每次连接必须是一个新的Socket对象，否则如果在其他地方关闭了socket对象，那么就无法
            //继续连接了，因为默认对象已经关闭了
            tcpClient = Socket()
            tcpClient.connect(
                InetSocketAddress(tcpClientTargetServerIP, tcpClientTargetServerPort),
                4000
            )

            //发送心跳包
            tcpClient.keepAlive = true
            //注意这里，不同的电脑PC端可能用到编码方式不同，通常会使用GBK格式而不是UTF-8格式
            val printWriter =
                PrintWriter(OutputStreamWriter(tcpClient.getOutputStream(), encodingFormat), true)
            //注意
            // 将缓冲区的数据强制输出，用于清空缓冲区，若直接调用close()方法，则可能会丢失缓冲区的数据。所以通俗来讲它起到的是刷新的作用。
            //printWriter.flush();
            // 用于关闭数据流
            ///printWriter.close();
            printWriter.write("app connect")
            printWriter.flush()
            tcpClientConnectStatus = true

            Log.e("连接服务端成功", "TCPClient")
            Log.e("开启客户端接收", "TCPClient")

            thread {
                funTCPClientReceive(SocketCmdManager.ReadListener)
            }

            callback.invoke(true, null)
        } catch (e: Exception) {
            callback.invoke(false, e)
            when (e) {
                is SocketTimeoutException -> {
                    Log.e("SocketManager","连接超时");
                    e.printStackTrace()
                }
                is NoRouteToHostException -> {
                    Log.e("SocketManager","该地址不存在");
                    e.printStackTrace()
                }
                is ConnectException -> {
                    Log.e("SocketManager","连接异常或被拒绝");
                    e.printStackTrace()
                }
                else -> {
                    e.printStackTrace()
                    Log.e("SocketManager","连接结束")
                }
            }
        }
    }

    public fun funTCPClientClose() {
        tcpClientConnectStatus = false
        tcpClientInputStreamReader?.close()
        tcpClientOutputStream?.close()
        tcpClient?.close()
    }
    //客户端发送
    //需要子线程
    public fun funTCPClientSend(msg: ByteArray) {
        if (msg.isNotEmpty() && tcpClientConnectStatus) {
            //这里要注意，只要曾经连接过，isConnected便一直返回true，无论现在是否正在连接
            if (tcpClient.isConnected) {
                try {
                    tcpClient.getOutputStream().write(msg)
                    Log.e("信息发送成功", HEXTool.toHexString(msg))

                } catch (e: IOException) {
                    Log.e("信息发送失败", HEXTool.toHexString(msg))
                    e.printStackTrace()
                    tcpClientInputStreamReader?.close()
                    tcpClientOutputStream?.close()
                    tcpClient.close()
                }
            }
        }
    }

    //客户端接收的消息
    //添加子线程
    private fun funTCPClientReceive(callback: ((ByteArray?) -> Unit)) {
        Log.e("开启客户端接收成功", "TCPClient")
        while (tcpClientConnectStatus) {
            if (tcpClient.isConnected) {

                try {
                    //將InputStream轉換為Byte
                    val inputStream = tcpClient!!.getInputStream()
                    val bao = ByteArrayOutputStream()
                    val buff = ByteArray(64)
                    var bytesRead = inputStream.read(buff)
                    if (buff.isNotEmpty()) {
                        callback.invoke(buff)
                    }
                }catch (e: Exception) {
                }


            } else {
                Log.e("开启客户端接收失败", "TCPClient")
                tcpClientInputStreamReader?.close()
                tcpClientOutputStream?.close()
                tcpClient.close()
                break
            }
        }
    }

    //获取设备局域网IP,没开wifi的情况下获取的会是内网ip
    private fun funGetLocalAddress(context: Context): String {
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        val localIP =
            (ipAddress and 0xff).toString() + "." + (ipAddress shr 8 and 0xff) + "." + (ipAddress shr 16 and 0xff) + "." + (ipAddress shr 24 and 0xff)
        Log.e("localIP", localIP)
        return localIP
    }

}