package com.example.flutter_app;

import io.flutter.embedding.android.FlutterActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import com.vson.cupblelib.BleConnectHelper;
import com.vson.cupblelib.BleScanHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "samples.flutter.dev/battery";
    private List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private String bleName, bleAddress;//Name and address of connected Bluetooth device
    private Activity mActivity;
    private SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String mBtAddress;
    private BleScanHelper bleScanHelper;
    private BleConnectHelper mWpBleConnectHelper;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 3;

    private int batteryLevel;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        mActivity = this;
        bleScanHelper = new BleScanHelper(mActivity);
        bleScanHelper.setOnScanListener(mOnScanListener);

        mWpBleConnectHelper = new BleConnectHelper(mActivity);
        mWpBleConnectHelper.setBleConnectionListener(mBleConnectListener);
        mWpBleConnectHelper.setBleCupDataListener(mBleCupDataListener);

            new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            // Note: this method is invoked on the main thread.
                            if (call.method.equals("onScan")) {
                                System.out.println("onScan");
                                startScan();
                            } else if (call.method.equals("setConnect")) {
                                bleScanHelper.onDestroy();

                                System.out.println("setConnect");
                                bleAddress = bluetoothDevices.get(0).getAddress();
                                bleName = bluetoothDevices.get(0).getName().split("#")[2];
                                mBtAddress = bleAddress;

                            }else if(call.method.equals("startConnect")){
                                mWpBleConnectHelper.connect(mBtAddress);
                                result.success(batteryLevel);
                            }else if(call.method.equals("sendInit")){
                                System.out.println("send init");
                                mWpBleConnectHelper.initCup();
                            }else if(call.method.equals("sendTime")){
                                System.out.println("sendTime");
                                mWpBleConnectHelper.sendCurrentTimeToCup();
                                //scanAdapter.notifyDataSetChanged();
                            }
                        }
                );
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPermission();
                }
                break;
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPermission();
                }
                break;
            }
        }
        }

    private BleConnectHelper.BleConnectListener mBleConnectListener = new BleConnectHelper.BleConnectListener() {
        @Override
        public void onConnectedSuccess() {
            System.out.println("onConnectedSuccess--->>");
            bluetoothDevices.clear();
        }

        @Override
        public void onConnectedFail() {
            if (null != mWpBleConnectHelper && !mWpBleConnectHelper.isConnected()) {
                //You can also use this method to cancel the connection during the connection process
                mWpBleConnectHelper.cancelConnectBLe();
            }
            System.out.println("onConnectedFail--->>");
        }

        @Override
        public void onDisConnected() {
            System.out.println("onDisConnected--->>");
            if (TextUtils.isEmpty(mBtAddress)) {
                startScan();
            } else {
                if (mWpBleConnectHelper != null && !mWpBleConnectHelper.isConnected()) {
                    //Close the connection
                    mWpBleConnectHelper.closeConnection();
                    //Reconnect with 1 second delay
                    mWpBleConnectHelper.connect(mBtAddress);
                    System.out.println("onDisConnected--->>"+mBtAddress);
                }
            }
        }

        @Override
        public void onMtuChanged(int mtu) {
            System.out.println("onMtuChanged--->>");
        }

        @Override
        public void onPermissionFail() {
            initPermission();
            Log.e("onPermissionFail--->", "\n" +
                    "Please apply for Bluetooth permission： Manifest.permission.ACCESS_FINE_LOCATION,\n" +
                    "Check if the phone supports ble and if the GPS is turned on when the Android version is greater than or equal to 10");
        }
    };

    private BleConnectHelper.BleCupDataListener mBleCupDataListener=new BleConnectHelper.BleCupDataListener() {
        @Override
        public void onBatteryValueChanged(int batteryValue) {
            Log.e("Activity--", "onBatteryValueChanged:" + batteryValue);
            batteryLevel = batteryValue;
        }

        @Override
        public void onSetNameSuccessDoReconnectCup(String mMacAddress) {
            Log.e("Activity--", "onSetNameSuccessDoReconnectCup:Reconnect Bluetooth\n");
            if (null != mWpBleConnectHelper && !mWpBleConnectHelper.isConnected()) {
                mWpBleConnectHelper.connect(mMacAddress);
                System.out.println("onDisConnected--->>"+mBtAddress);

            }
        }

        @Override
        public void onInitCupSuccess() {
            Log.e("Activity--", "onInitCupSuccess:");
        }

        @Override
        public void onSetCupTimeSuccess() {
            Log.e("Activity--", "onSetCupTimeSuccess:");
        }

        @Override
        public void onDrinkValueChange(boolean isHistoryData, long drinkTime, int drinkValue) {
            Log.e("Activity--", "onDrinkValueChange:" + isHistoryData + "----" + formatDate.format(drinkTime) + "-----" + drinkValue);
            if (isHistoryData) {

            } else {

            }
        }
    };

    private BleScanHelper.OnScanListener mOnScanListener=new BleScanHelper.OnScanListener() {
        @Override
        public void onScanResult(BluetoothDevice device, int rssi, ScanRecord scanRecord) {
            String deviceName = device.getName();
            String deviceMac = device.getAddress();
            try {
                //If the device name is empty, don’t place it
                if (TextUtils.isEmpty(deviceName) || !deviceName.contains("VSON#WP") || deviceName.length() <= 10)
                    return;
                Log.e("onScanResult--->", "deviceName:" + deviceName);
                String[] ns = deviceName.split("#");
                if (ns.length == 3 && ns[1].startsWith("WP28") && !bluetoothDevices.contains(device)) {
                    bluetoothDevices.add(device);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onScanFinish() {
            if (bluetoothDevices.size() > 0) {
            } else {
                System.err.println("No device found！");
            }
        }

        @Override
        public void onPermissionFail() {
            Log.e("onPermissionFail--->", "Please apply for Bluetooth permission： Manifest.permission.ACCESS_FINE_LOCATION, Check if the phone supports ble and if the GPS is turned on when the Android version is greater than or equal to 10");
            initPermission();
        }
    };

    @TargetApi(23)
    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //showRequestPermissionWriteSettings();
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            } else if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            } else {
                startScan();
            }
        }
    }

    private void startScan() {
        if (null == bleScanHelper) {
            bleScanHelper = new BleScanHelper(mActivity);
            bleScanHelper.setOnScanListener(mOnScanListener);
        }
        mBtAddress = null;
        bluetoothDevices.clear();
        if (null != mWpBleConnectHelper) {
            mWpBleConnectHelper.disConnection();
        }
        bleScanHelper.startScanBle(10 * 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == BleConnectHelper.OPEN_BLUETOOTH_REQUEST_CODE) {
                //Make sure to turn on Bluetooth and reconnect the Bluetooth device
                if (mWpBleConnectHelper != null) {
                    System.out.println("onDisConnected--->>"+mBtAddress);
                    mWpBleConnectHelper.connect(mBtAddress);
                }
            } else if (requestCode == BleScanHelper.OPEN_BLUETOOTH_REQUEST_CODE) {
                if (bleScanHelper != null) {
                    startScan();
                }
            }

        } else if (resultCode == RESULT_CANCELED) {
            //Refuse to turn on Bluetooth
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Release Bluetooth connection resources
        if (mWpBleConnectHelper != null) {
            mWpBleConnectHelper.onDestroy();
            mWpBleConnectHelper = null;
        }
    }
}
