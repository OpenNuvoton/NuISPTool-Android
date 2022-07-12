package com.nuvoton.nuisptool_android.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Handler;


import androidx.annotation.RequiresApi;

import com.nuvoton.nuisptool_android.Util.HEXTool;
import com.nuvoton.nuisptool_android.Util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Created by ChargeON kawa on 2017/10/18.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeData {

    private String TAG = getClass().getName();
    private BluetoothLeData() {}

    private Context _context;
    private BluetoothAdapter _mBluetoothAdapter;
    private String _bleMacAddress = "B8:27:EB:3F:D6:49";
    private String _bleDeviceName = "";
    private BluetoothDevice _mBluetoothDevice;

    private ScanFilter _scanFilter;
    private boolean _isConnecting = false;
    private int _connectState = -1;

    private int _MTUSize = 131;

    //搜尋用執行緒
    private Handler _mHandler = new Handler();
    private boolean _mScanning;

    private ArrayList<ScanResult> _scanResultArray = new ArrayList<>();
    private ArrayList<BleServicesData> _servicesDataArray = new ArrayList<>();
    private BluetoothGatt _mBluetoothGatt;



    public BluetoothLeData(Context context, String bleMacAddress) {
        this._bleMacAddress = bleMacAddress;
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        _mBluetoothAdapter = bluetoothManager.getAdapter();
        _mBluetoothDevice = _mBluetoothAdapter.getRemoteDevice(bleMacAddress);
        _bleDeviceName = _mBluetoothDevice.getName();
        this._context = context;
    }

    public class BleServicesData {
        private UUID _servicesUUID;
        private ArrayList<CharacteristicData> _characteristicDataArray = new ArrayList<>();
        private BluetoothGattService _bluetoothGattService;

        public ArrayList<CharacteristicData> getCharacteristicDataArray() {
            return _characteristicDataArray;
        }
    }

    public class CharacteristicData {
        private UUID _characteristicUUID;
        private byte[] _value = "CharacteristicValue".getBytes();
        private boolean _hasWrite = false;
        private boolean _hasNotify = false;
        private boolean _hasIndication = false;
        private boolean _hasRead = false;
        private BluetoothGattCharacteristic _bluetoothGattCharacteristic;

        public String getUUID() {
            return _characteristicUUID.toString();
        }
        public boolean write(String value)  {
            if (this._hasWrite != true) {
                return false;
            }
            this._bluetoothGattCharacteristic.setValue(value);
            _mBluetoothGatt.writeCharacteristic(this._bluetoothGattCharacteristic);
            return false;
        }
        public boolean write(byte[] value)  {
            if (this._hasWrite != true) {
                return false;
            }
            this._bluetoothGattCharacteristic.setValue(value);
            return _mBluetoothGatt.writeCharacteristic(this._bluetoothGattCharacteristic);
        }
        public boolean write(String value,writeCallBack listener)  {
            if (this._hasWrite != true) {
                return false;
            }
            _writeListener = listener;

            this._bluetoothGattCharacteristic.setValue(value);
            _mBluetoothGatt.writeCharacteristic(this._bluetoothGattCharacteristic);
            return false;
        }
        public boolean write(byte[] value,writeCallBack listener)  {
            Log.d(TAG, "BLE write:"+  HEXTool.INSTANCE.toHexString(value));

            if (this._hasWrite != true) {
                return false;
            }
            _writeListener = listener;
            this._bluetoothGattCharacteristic.setValue(value);
            return _mBluetoothGatt.writeCharacteristic(this._bluetoothGattCharacteristic);
        }

        public boolean setNotify(boolean enable ,notifCallBack listener) {
            if (this._hasNotify != true) {
                return false;
            }
//            if(_notifListener == listener){
//                return false;
//            }

            _mBluetoothGatt.setCharacteristicNotification(this._bluetoothGattCharacteristic, true);
            boolean success = _mBluetoothGatt.setCharacteristicNotification(this._bluetoothGattCharacteristic, enable);
            Log.d(TAG, "setCharacteristicNotification: " + enable+ "\nsuccess: " + success+ "\ncharacteristic.getUuid(): " + this._bluetoothGattCharacteristic.getUuid());

            BluetoothGattDescriptor descriptor = this._bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BluetoothLeGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                if(enable){
                    descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                boolean success2 = _mBluetoothGatt.writeDescriptor(descriptor);

                if(success2 == true){ //設定成功才接通監聽
                    _notifListener = listener;
                }
                return success2;
            }
            return false;
        }
        public boolean setIndication(boolean enable ,indicationCallBack listener) {
            if (this._hasIndication != true) {
                return false;
            }
            _indicationListener = listener;
            _mBluetoothGatt.setCharacteristicNotification(this._bluetoothGattCharacteristic, true);
            boolean success = _mBluetoothGatt.setCharacteristicNotification(this._bluetoothGattCharacteristic, enable);
            Log.d(TAG, "setCharacteristicNotification: " + enable+ "\nsuccess: " + success+ "\ncharacteristic.getUuid(): " + this._bluetoothGattCharacteristic.getUuid());

            BluetoothGattDescriptor descriptor = this._bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BluetoothLeGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                if(enable){
                    descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                boolean success2 = _mBluetoothGatt.writeDescriptor(descriptor);
                return success2;
            }
            return false;
        }
        public void read(readCallBack listener) throws RuntimeException {
            if (this._hasRead != true) {
                return ;
            }
            _readListener = listener;
            _mBluetoothGatt.readCharacteristic(this._bluetoothGattCharacteristic);

        }

        public void readHex(readHexCallBack listener) throws RuntimeException {
            if (this._hasRead != true) {
                return ;
            }
            _readHexListener = listener;
            _mBluetoothGatt.readCharacteristic(this._bluetoothGattCharacteristic);
        }
    }

    public String getbleMacAddress() {return _bleMacAddress; }

    public String getDeviceName() {return _bleDeviceName;}

    public ArrayList<BleServicesData> getServicesDataArray() {
        return _servicesDataArray;
    }

    /**
     * 檢查此裝置是否BLE連線
     */
    public boolean isConnect() {
        if (_connectState == BluetoothProfile.STATE_CONNECTED)
            return true;
        return false;
    }
    /**
     * 將藍芽斷線
     */
    public void setDisConnect() {
//        _connectState = BluetoothProfile.STATE_DISCONNECTED;
        if(_mBluetoothGatt==null){
            return;
        }
        _mBluetoothGatt.disconnect();

    }
    /**
     * 將藍芽斷線
     */
    public void setDisClose() {
        if(_mBluetoothGatt==null){
            return;
        }
        _mBluetoothGatt.close();
        _connectState = BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * 監聽藍芽狀態
     * @param listener
     */
    public void setOnStateChangeListener(onStateChange listener){
        _onStateChange = listener;
    }

    /**
     * scan BLeDevice
     * 打開搜尋 BLeDevice
     *
     * @param enable
     */
    public void scanLeDevice(final boolean enable) {
        final long SCAN_PERIOD = 2000;
        if (enable) {
            _mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _mScanning = false;
                    _mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                }
            }, SCAN_PERIOD);

            _mScanning = true;
            _mBluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        } else {
            _mScanning = false;
            _mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    /**
     * connect BLe Device
     * 連線到 BLe 裝置
     *
     * @param listener
     */
    public void connectLeDevice(connectCallBack listener) {
        Log.d(TAG, "connectLeDevice");

        _connectListener = listener;

        if (_mBluetoothDevice == null) {
            android.util.Log.w(TAG, "Device not found.  Unable to connect.");
            _connectListener.connectCallBack(false);
        }
        if (_mBluetoothAdapter.isEnabled() == false) {
            android.util.Log.w(TAG, "BluetoothAdapter not Enabled.  Unable to connect.");
            _connectListener.connectCallBack(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            _isConnecting = false;
            _mBluetoothGatt = _mBluetoothDevice.connectGatt(_context, false, bluetoothGattCallback);
        }
//        throw new Exception("Build.VERSION.SDK_INT < Build.VERSION_CODES.M");
    }

    /**
     * Scan Call back
     * 呼叫搜尋的回傳
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult  callbackType:" + callbackType + "   result:" + result +" State:"+_mBluetoothAdapter.getState());
            if (!_scanResultArray.contains(result)) {
                _scanResultArray.add(result);
            }
            if (result.toString().indexOf(_bleMacAddress) > -1) {
                _mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            }

            if (_isConnecting == true) {
                _mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                return;
            }
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
    /**
     * Bluetooth Gatt Callback
     * 執行BLE Gatt 動作的回傳(Connect、ServicesDiscovered、Read、Write、Notify)
     */
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.d(TAG, "onPhyUpdate");
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.d(TAG, "onPhyRead");
        }

        /***
         * 狀態顯是
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            _connectState = newState;
            Log.d(TAG, "onConnectionStateChange");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                gatt.requestMtu(_MTUSize);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                _connectListener.connectCallBack(false);
            }

            if(_onStateChange!=null){
                _onStateChange.onStateChange(gatt.getDevice().getAddress(),status,newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                _servicesDataArray.clear();
                Log.d(TAG, "onServicesDiscovered:GATT_SUCCESS");
                List<BluetoothGattService> bgsList = gatt.getServices();
                for (BluetoothGattService bgs : bgsList) {
                    BleServicesData bsd = new BleServicesData();
                    bsd._bluetoothGattService = bgs;
                    bsd._servicesUUID = bgs.getUuid();

                    _servicesDataArray.add(bsd);

                    List<BluetoothGattCharacteristic> bgcList = bgs.getCharacteristics();
                    for (BluetoothGattCharacteristic bgc : bgcList) {
                        CharacteristicData cd = new CharacteristicData();
                        cd._bluetoothGattCharacteristic = bgc;
                        cd._characteristicUUID = bgc.getUuid();
                        cd._value = bgc.getValue();
                        int charaProp = bgc.getProperties();
                        if ((charaProp & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                            cd._hasWrite = true;
                        }
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            cd._hasRead = true;
                        }
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            cd._hasNotify = true;
                        }
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                            cd._hasIndication = true;
                        }
                        bsd._characteristicDataArray.add(cd);
                    }
                    Log.d(TAG, "getServices UUID:" + bsd._servicesUUID);
                }
                _connectState =  BluetoothProfile.STATE_CONNECTED;
                _connectListener.connectCallBack(true);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                _connectListener.connectCallBack(false);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");

            for (BleServicesData bsd : _servicesDataArray) {
                for (CharacteristicData cd : bsd._characteristicDataArray) {
                    if (cd._characteristicUUID.equals(characteristic.getUuid())) {
                        cd._value = characteristic.getValue();
                        String s = HEXTool.INSTANCE.toHexString(cd._value);
                        Log.d(TAG, "characteristic.getValue():" + s);
                        if(_readListener!=null)
                        _readListener.readCallBack((status == BluetoothGatt.GATT_SUCCESS), s);
                        if(_readHexListener!=null)
                        _readHexListener.readCallBack((status == BluetoothGatt.GATT_SUCCESS), cd._value);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite");

            for (BleServicesData bsd : _servicesDataArray) {
                for (CharacteristicData cd : bsd._characteristicDataArray) {
                    if (cd._characteristicUUID.equals(characteristic.getUuid())) {
                        if(_writeListener!=null){
                            _writeListener.writeCallBack(status==BluetoothGatt.GATT_SUCCESS);
                        }
                    }
                }
            }
        }

        /**
         * Notif 回來的地方
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.d(TAG, "Gatt:"+gatt+"   onCharacteristicChanged  :" + characteristic.getValue() + " , " +  HEXTool.INSTANCE.toHexString(characteristic.getValue()));
            _notifListener.notifCallBack(_bleMacAddress,characteristic.getUuid(),characteristic.getValue());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite");

            if(_indicationListener!=null){
                _indicationListener.indicationCallBack(status==BluetoothGatt.GATT_SUCCESS);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged");
            _mBluetoothGatt.discoverServices();
        }

//        @Override
//        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout,int status) {
//        }

    };


    public boolean isBleEnable() {
        if (_mBluetoothAdapter == null) {
            return false;
        }
        return _mBluetoothAdapter.isEnabled();

    }

    private connectCallBack _connectListener;
    public interface connectCallBack {
        public void connectCallBack(boolean status);
    }

    private onStateChange _onStateChange;
    public interface onStateChange {
        public void onStateChange(String address, int status, int newState);
    }


    private readCallBack _readListener;
    private readHexCallBack _readHexListener;

    public interface readCallBack {
        public void readCallBack(boolean status, String readString);
    }

    public interface readHexCallBack {
        public void readCallBack(boolean status, byte[] readbytes);
    }

    private writeCallBack _writeListener;

    public interface writeCallBack {
        public void writeCallBack(boolean status);
    }

    private indicationCallBack _indicationListener;

    public interface indicationCallBack {
        public void indicationCallBack(boolean status);
    }

    private notifCallBack _notifListener;

    public interface notifCallBack {
        public void notifCallBack(String bleMAC, UUID UUID, byte[] Value);
    }
}
