package cn.seekecho.hj_plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.pm.PackageManager;
import android.Manifest;

public class HJ580 extends CordovaPlugin {
    private HolloBluetooth mble;
    private Handler mHandler = new Handler();
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;

    private static final long SCAN_PERIOD = 5000;
    String[] permissions = {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private HolloBluetooth.OnHolloBluetoothCallBack bleCallBack= new HolloBluetooth.OnHolloBluetoothCallBack() {
        @Override
        public void OnHolloBluetoothState(int state) {
            if (state == HolloBluetooth.HOLLO_BLE_DISCONNECTED) {
                connected = false;
            }
        }

        @Override
        public void OnReceiveData(byte[] recvData) {
            String data = ConvertData.utf8ToUnicode(recvData);
            while(reading);
            writing = true;
            gotData += data;
            writing = false;
        }
    };
    private String gotData = "";
    private boolean reading = false;
    private boolean writing = false;
    private boolean connected = false;

    private final String keyStatus = "status";
    private final String keyMessage = "msg";
    private final String keyName = "name";
    private final String keyAddress = "address";
    private final String keyRssi = "rssi";

    private final String statusError = "error";
    private final String statusOk = "ok";
    private final String errorNotSupport = "BLE is not supported on the device";
    private final String errorNotInitialized = "BLE is not initialized";
    private final String errorInvalidAddress = "Invalid address";
    private final String errorInvalidMsg = "Invalid message";
    private final String errorDisconected = "Device is disconected";
    private final String errorConnectFailed = "Connect failed";
    private final String errorSendFailed = "send failed";
    private final String errorPermissionDenied = "Permission denied";

    CallbackContext context;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("chackPermissions")){
            context = callbackContext;
            if(hasPermisssion()) {
                JSONObject retObj = new JSONObject();
                addProperty(retObj, keyStatus, statusOk);
                callbackContext.success(retObj);
            }
            else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
        } else if (action.equals("initBLE")) {
            mble = HolloBluetooth.getInstance(cordova.getActivity().getApplicationContext());
            JSONObject retObj = new JSONObject();
            if (!mble.isBleSupported() || !mble.connectLocalDevice()) {
                addProperty(retObj, keyMessage, errorNotSupport);
                callbackContext.error(retObj);
                return true;
            } else {
                addProperty(retObj, keyStatus, statusOk);
                callbackContext.success(retObj);
            }
        } else if (action.equals("startScan")) {
            startScan(callbackContext);
        } else if (action.equals("stopScan")) {
            if (!isNotInitialized(callbackContext)) {
                if (mScanning) {
                    scanLeDevice(false);
                    JSONObject retObj = new JSONObject();
                    addProperty(retObj, keyStatus, statusOk);
                    callbackContext.success(retObj);
                }
            }

        } else if (action.equals("connect")) {
            if (!isNotInitialized(callbackContext)) {
                String address = args.getString(0);
                connectAction(address, callbackContext);
            }
        } else if (action.equals("disconnect")) {
            if (!isNotInitialized(callbackContext)) {
                disconnectAction(callbackContext);
            }
        } else if (action.equals("sendData")) {
            if (!isNotInitialized(callbackContext)) {
                final String data = args.getString(0);
                sendAction(data, callbackContext);
            }
        } else if (action.equals("readData")) {
            if(!isNotInitialized(callbackContext)){
                readAction(callbackContext);
            }
        } else {
            return false;
        }
        return true;
    }

    private void startScan(final CallbackContext callbackContext) {
        if (!mScanning) {
            scanLeDevice(true);
            // 扫描到设备回调函数
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject returnObj = new JSONObject();
                            String deviceName = device.getName();

                            if (deviceName == null || deviceName.length() <= 0)
                                deviceName = "unknow device";

                            // 02010603035869
                            if (scanRecord.length < 7 || scanRecord[0] != 0x02 || scanRecord[1] != 0x01
                                    || scanRecord[2] != 0x06 || scanRecord[3] != 0x03 || scanRecord[4] != 0x03
                                    || scanRecord[5] != 0x58 || scanRecord[6] != 0x69) {
                                if (scanRecord.length < 9 || scanRecord[0] != 0x02 || scanRecord[1] != 0x01
                                        || scanRecord[2] != 0x06 || scanRecord[3] != 0x05 || scanRecord[4] != 0x03
                                        || scanRecord[5] != 0x58 || scanRecord[6] != 0x69
                                        || scanRecord[7] != (byte) 0xE7 || scanRecord[8] != (byte) 0xFE) {
                                    if (scanRecord.length < 2 || scanRecord[0] != 0x1A || scanRecord[1] != 0xFF)
                                        return;
                                }
                            }
                            addProperty(returnObj, keyStatus, statusOk);
                            addProperty(returnObj, keyName, deviceName);
                            addProperty(returnObj, keyRssi, "" + Integer.valueOf(rssi));
                            addProperty(returnObj, keyAddress, device.getAddress());
                            callbackContext.success(returnObj);
                        }
                    }).start();
                }
            };
            mble.setScanCallBack(mLeScanCallback);
        }
    }

    private void connectAction(final String remoteAddress, final CallbackContext callbackContext) {
        if (remoteAddress.length() <= 0) {
            callbackContext.error(errorInvalidAddress);
            return;
        }
        if (mScanning) {
            scanLeDevice(false);
        }
        final JSONObject retObj = new JSONObject();

        
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i;
                // 尝试连接五次
                for (i = 0; i < 5; i++) {
                    if (mble.connectDevice(remoteAddress, bleCallBack)) // 连接蓝牙设备
                        break;

                    try {
                        Thread.sleep(500, 0);
                    } catch (Exception e) {

                    }
                }
                if (i == 5) {
                    callbackContext.error(errorConnectFailed);
                    return;
                }
                if (!mble.wakeUpBle()) {
                    callbackContext.error(errorConnectFailed);
                    return;
                }
                connected = true;
                addProperty(retObj, keyStatus, statusOk);
                callbackContext.success(retObj);
            }
        }).start();
    }

    private void disconnectAction(final CallbackContext callbackContext) {
        if (mScanning) {
            scanLeDevice(false);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final JSONObject retObj = new JSONObject();
                mble.disconnectDevice();
                addProperty(retObj, keyMessage, statusOk);
                callbackContext.success(retObj);
            }
        }).start();
    }

    private void sendAction(final String data, final CallbackContext callbackContext) {
        if(!connected){
            callbackContext.error(errorDisconected);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject retObj = new JSONObject();
                if (data == null || data.isEmpty()) {
                    addProperty(retObj, keyMessage, errorInvalidMsg);
                    callbackContext.error(retObj);
                    return;
                }

                byte[] bytes = ConvertData.unicodeToUtf8(data);
                if (bytes == null) {
                    addProperty(retObj, keyMessage, errorInvalidMsg);
                    callbackContext.error(retObj);
                    return;
                }

                if (!mble.sendData(bytes)) {
                    addProperty(retObj, keyMessage, errorSendFailed);
                    callbackContext.error(retObj);
                    return;
                }
                addProperty(retObj, keyStatus, statusOk);
                callbackContext.success(retObj);
            }
        }).start();
    }

    private void readAction(CallbackContext callbackContext){
        if(!connected){
            callbackContext.error(errorDisconected);
            return;
        }
        while(writing);
        reading = true;
        JSONObject retObj = new JSONObject();
        addProperty(retObj, keyStatus, statusOk);
        addProperty(retObj, keyMessage, gotData);
        callbackContext.success(retObj);
        gotData = "";
        reading = false;
    }

    Runnable cancelScan = new Runnable() {
        @Override
        public void run() {
            mble.stopLeScan();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mble.startLeScan();
            mHandler.postDelayed(cancelScan, SCAN_PERIOD);
        }
    };

    // enable = true表示蓝牙开始扫描，否则表示停止扫描
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // SCAN_PERIOD 秒后停止扫描
            mHandler.postDelayed(cancelScan, SCAN_PERIOD);

            mScanning = true;
            mble.startLeScan(); // 开始蓝牙扫描
        } else {
            // 取消停止扫描的线程
            mHandler.removeCallbacks(cancelScan);
            mScanning = false;
            mble.stopLeScan(); // 停止蓝牙扫描
        }
    }

    private boolean isNotInitialized(CallbackContext callbackContext) {
        if (mble == null) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, keyMessage, errorNotInitialized);
            callbackContext.error(returnObj);
            return true;
        }
        return false;
    }

    private void addProperty(JSONObject obj, String key, Object value) {
        // Believe exception only occurs when adding duplicate keys, so just ignore it
        try {
            if (value == null) {
                obj.put(key, JSONObject.NULL);
            } else {
                obj.put(key, value);
            }
        } catch (JSONException e) {
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        if(context != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    context.error(errorPermissionDenied);
                    return;
                }

            }
            JSONObject retObj = new JSONObject();
            addProperty(retObj, keyStatus, statusOk);
            context.success(retObj);
        }
    }

    public boolean hasPermisssion() {
        for(String p : permissions) {
            if(!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }
}

