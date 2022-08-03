package com.nuvoton.nuisptool_android.Util;

/**
 * Created by ChargeON kawa on 2017/9/27.
 */

public class Log {
    private static boolean hasLog = false ;
    public static void v(String LOG_TAG, String log){
        if(hasLog){
            android.util.Log.v(LOG_TAG, log);
        }
    }
    public  static void d(String LOG_TAG,String log){
        if(hasLog){
            android.util.Log.d(LOG_TAG, log);
        }
    }
    public  static void i(String LOG_TAG,String log){
        if(hasLog){
            android.util.Log.i(LOG_TAG, log);
        }
    }
    public  static void w(String LOG_TAG,String log){
        if(hasLog){
            android.util.Log.w(LOG_TAG, log);
        }
    }
    public  static void e(String LOG_TAG,String log){
        if(hasLog){
            android.util.Log.e(LOG_TAG, log);
        }
    }
}
