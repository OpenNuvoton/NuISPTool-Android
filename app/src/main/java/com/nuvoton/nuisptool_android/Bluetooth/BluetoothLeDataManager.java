package com.nuvoton.nuisptool_android.Bluetooth;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;

import com.nuvoton.nuisptool_android.Util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ChargeON kawa on 2017/10/23.
 * Singleton class
 */

/**
 * BluetoothLe Data Manager
 * 負責管理BluetoothLeData。所有的Data請從這裡取得，不要自己new。
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeDataManager {
    private BluetoothLeDataManager() { }

    private static class SmartyLoader {
        private static final BluetoothLeDataManager instance = new BluetoothLeDataManager();
    }

    public static BluetoothLeDataManager getInstance() {
        return SmartyLoader.instance;
    }
    public static Context context;

    private static HashMap<String, BluetoothLeData> _DataHasMap = new HashMap();

    private String TAG = getClass().getName();
    private BluetoothAdapter _mBluetoothAdapter;
    private boolean _mScanning;
    private Handler _handler = new Handler();
    private ArrayList<ScanResult> _scanResultArray = new ArrayList<>();
    private BluetoothLeScanner _mBluetoothLeScanner;


//    public void saveBluetoothLeData(BluetoothLeData bld) {
//        _DataHasMap.put(bld.getKey(), bld);
//    }

    public BluetoothLeData getBluetoothLeData(Context context, String deviceAddress) {
        if (_DataHasMap.containsKey(deviceAddress)) {
            return _DataHasMap.get(deviceAddress);
        }
        BluetoothLeData bld = new BluetoothLeData(context, deviceAddress);
        _DataHasMap.put(bld.getbleMacAddress(), bld);
        return bld;
    }

    public ArrayList<BluetoothLeData> getBluetoothLeDataArray() {
        ArrayList<BluetoothLeData> bldList = new ArrayList<>();
        for (HashMap.Entry<String, BluetoothLeData> entry : _DataHasMap.entrySet()) {
//            String key = entry.getKey();
//            BluetoothLeData value = entry.getValue();
            bldList.add(entry.getValue());
        }
        return bldList;
    }


    public Boolean isBluetoothEnabled(Context context){
        if (_mBluetoothAdapter == null || !_mBluetoothAdapter.isEnabled()) {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            _mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if(!_mBluetoothAdapter.isEnabled()){
            return false;
        }
        return true;
    }

    public boolean isGPSEnabled(Context context){
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    /**
     * 打開收尋
     * @param enable
     * @param context
     * @param callback ScanCallback 不可null
     */
    public void scanLeDeviceByPeriod(final boolean enable, int scanPeriod, Context context, ScanCallback callback) {
        final long SCAN_PERIOD = scanPeriod;

        if(callback == null){
            throw new IllegalArgumentException("BleScanCallback can not be Null!");
        }

        if (_mBluetoothAdapter == null || !_mBluetoothAdapter.isEnabled()) {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            _mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (_mBluetoothLeScanner == null ) {
            _mBluetoothLeScanner = _mBluetoothAdapter.getBluetoothLeScanner();
        }

        if (enable) {

            _mScanning = true;
            _mBluetoothLeScanner.startScan(callback);

            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for(int i = 1; i <= 10; i++) {
                        _mBluetoothLeScanner.stopScan(callback);
                    }
                    _mBluetoothLeScanner.stopScan(callback);
                    _mScanning = false;
                }
            }, SCAN_PERIOD);

        } else {
            _mScanning = false;
            _mBluetoothLeScanner.stopScan(callback);
            for(int i = 1; i <= 10; i++) {
                _mBluetoothLeScanner.stopScan(callback);
            }
        }
    }

    /**
     * 打開收尋
     * @param enable
     * @param context
     * @param callback ScanCallback 不可null
     */
    public void scanLeDevice(final boolean enable,Context context,ScanCallback callback) {

        if(callback == null){
            throw new IllegalArgumentException("BleScanCallback can not be Null!");
        }

        if (_mBluetoothAdapter == null || !_mBluetoothAdapter.isEnabled()) {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            _mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (_mBluetoothLeScanner == null ) {
            _mBluetoothLeScanner = _mBluetoothAdapter.getBluetoothLeScanner();
        }

        if (enable) {
            _mScanning = true;
            _mBluetoothLeScanner.startScan(callback);
        } else {
            _mScanning = false;
            _mBluetoothLeScanner.stopScan(callback);
            for(int i = 1; i <= 10; i++) {
                _mBluetoothLeScanner.stopScan(callback);
            }
        }
    }

    /**
     * Scan Call back
     * 呼叫搜尋的回傳
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult  callbackType:" + callbackType + "   result:" + result);
            if (!_scanResultArray.contains(result)) {
                _scanResultArray.add(result);
            }

//            if (result.toString().indexOf(_bleMacAddress) > -1) {
//                _mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
//            }
//
//            if (_isConnecting == true) {
//                _mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
//                return;
//            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "results:" + results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "errorCode:" + errorCode);
        }
    };

}
