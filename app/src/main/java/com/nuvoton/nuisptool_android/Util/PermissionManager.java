package com.nuvoton.nuisptool_android.Util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by ChargeON WPHU on 2017/9/28.
 */

public class PermissionManager implements EasyPermissions.PermissionCallbacks {

    private onPermissionsListener _mListener;



    public interface onPermissionsListener {
        public void Granted();

        public void Denied();
    }

    private String TAG = getClass().getName();
    private Context _context;

    /**
     * if you want to selfPermission , remember set onPermissionsListener
     * 如果要開啟權限，記得 set onPermissionsListener
     *
     * @param contex
     */
    public PermissionManager(Context contex) {
        _context = contex;
    }


    /**
     * Permission's Type enum
     */
    public enum PermissionType {
        CAMERA(Manifest.permission.CAMERA, 1),
        GPS(Manifest.permission.ACCESS_FINE_LOCATION, 2),
        WIFI(Manifest.permission.CHANGE_WIFI_STATE, 3),
        //        BLUETOOTH(Manifest.permission.BLUETOOTH_PRIVILEGED, 4);
        BLUETOOTH(Manifest.permission.BLUETOOTH, 4),
        BLUETOOTH_ADMIN(Manifest.permission.BLUETOOTH_ADMIN, 7),
        READ_EXTERNAL_STORAGE(Manifest.permission.READ_EXTERNAL_STORAGE, 5),
        WRITE_EXTERNAL_STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE, 6),
        BLUETOOTH_SCAN(Manifest.permission.BLUETOOTH_SCAN, 9),
        BLUETOOTH_CONNECT(Manifest.permission.BLUETOOTH_CONNECT, 10),
        MANAGE_EXTERNAL_STORAGE(Manifest.permission.MANAGE_EXTERNAL_STORAGE, 8);



        private final String _name;
        private int _requestCode = 0;

        private PermissionType(String s, int rc) {
            this._name = s;
            this._requestCode = rc;
        }

        public String toString() {
            return this._name;
        }
    }

    /**
     * check mobile version is >=23 檢查手機版本是否大於23
     *
     * @return boolean
     */
    private boolean checkVersion() {
        //  if Mobile Ver >= 23 ( M )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true;
        }
        return false;
    }

    /**
     * check Permission has open    檢查是否有允許(該項)權限
     *
     * @param permission PermissionType
     * @return boolean
     */
    public boolean checkPermission(PermissionType permission) {
        if (EasyPermissions.hasPermissions(_context, permission.toString())) {
            return true;
        }
        return false;
    }

    /**
     * setting open the permission  詢問開啟權限
     *
     * @param msg        提醒文字字串
     * @param permission PermissionType
     * @return boolean
     */
    public void selfPermission(String msg, PermissionType permission) {
        EasyPermissions.requestPermissions((Activity) _context, msg, permission._requestCode, permission.toString());

    }

    @SuppressLint("Range")
    public void selfPermission(String msg, ArrayList<PermissionType> array) {
//        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        String[] perms = new String[array.size() - 1];
        for (int i = 0; i < array.size() - 1; i++) {
            perms[i] = array.get(i).toString();
        }
        //@NonNull Activity host, @NonNull String rationale,
        //            int requestCode, @Size(min = 1) @NonNull String... perms
//        EasyPermissions.requestPermissions();
        EasyPermissions.requestPermissions((Activity) _context, msg, 100, perms);
    }


    /**
     * set the Permission Listener  註冊監聽
     *
     * @param listener onPermissionsListener
     */
    public void setListener(onPermissionsListener listener) {
        _mListener = listener;
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        _mListener.Granted();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        _mListener.Denied();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }
}
