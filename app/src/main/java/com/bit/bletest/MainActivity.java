package com.bit.bletest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.bit.bletest.Consts.COMMAND_ID;
import static com.bit.bletest.Consts.DESCRIPTOR_UUID;
import static com.bit.bletest.Consts.OFFS_COMMAND_ID_H;
import static com.bit.bletest.Consts.OFFS_COMMAND_ID_L;
import static com.bit.bletest.Consts.OFFS_PAYLOAD;
import static com.bit.bletest.Consts.OFFS_VENDOR_ID_H;
import static com.bit.bletest.Consts.OFFS_VENDOR_ID_L;
import static com.bit.bletest.Consts.REQUEST_COARCH_LOCATION;
import static com.bit.bletest.Consts.REQUEST_ENABLE_BT;
import static com.bit.bletest.Consts.RESPONSE_ID;
import static com.bit.bletest.Consts.SCAN_PERIOD;
import static com.bit.bletest.Consts.SERVICE_ID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private BluetoothAdapter bluetoothAdapter;
    private Handler mHandler = new Handler();
    private boolean mScanning = false;
    private NormalDeviceReceiver mReceiver;
    private List<BluetoothDevice> devicesList = new ArrayList<>();

    private BluetoothGatt bluetoothGatt;
    private Button btnStartScan;
    private Button btnStopScan;
    private ListView deviceListView;

    private DeviceAdapter deviceAdapter;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic cmdCharacteristic;
    private Button btnSetLanguage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViewAndListener();

        initAdapter();
        //检查必要的权限
        checkAndRequestPermission();
        //检查设备是否支持蓝牙 若支持，开启蓝牙
        checkIfSupportBlueTooth();

        //执行设备发现前，必须查询已配对的设备集，确认所需要的设备是否处于已检测到的状态
        queryBondedDevices();
        //注册普通蓝牙扫描广播
        registerDeviceFoundReceiver();
    }



    private void initViewAndListener() {
        btnStartScan = findViewById(R.id.btnStartScan);
        btnStopScan = findViewById(R.id.btnStopScan);
        btnSetLanguage = findViewById(R.id.btnSetLanguage);
        btnStartScan.setOnClickListener(this);
        btnStopScan.setOnClickListener(this);
        btnSetLanguage.setOnClickListener(this);

        deviceListView = findViewById(R.id.listDevice);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectDevice(position);
                Log.d(TAG, "onItemClick: "+ devicesList.get(position).getName()
                        +"==="+devicesList.get(position).getAddress());
            }
        });
    }

    private void initAdapter() {
        deviceAdapter = new DeviceAdapter(getApplicationContext(), devicesList);
        deviceListView.setAdapter(deviceAdapter);
    }

    //注册发现设备的广播
    private void registerDeviceFoundReceiver() {
        mReceiver = new NormalDeviceReceiver(this);
        IntentFilter filter = new IntentFilter();
        registerReceiver(mReceiver, filter);
    }


    //预留@reactMethod
    private void getMyDevices() {

    }


    //预留@reactMethod
    private void getOtherDevices() {

    }

    //查询已经绑定的设备
    private void queryBondedDevices() {
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "queryBondedDevices: "
                        + device.getName() + "---" + device.getAddress());
            }
        }
    }

    /**
     * 检查并申请所需要的权限
     */
    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
                    || this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
            ) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_COARCH_LOCATION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_COARCH_LOCATION:{
                if (grantResults[0] == PERMISSION_GRANTED) {
                    Log.d(TAG, "request permission success");
                } else {
                    //申请失败
                }
            }
            break;
        }
    }

    /**
     * 检查手机是否支持蓝牙，  如果支持，检查是否蓝牙已经打开
     */
    private void checkIfSupportBlueTooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "don't support bluetooth");
        } else {
            Log.d(TAG, "support bluetooth");
            checkIfOpenBlueTooth();
        }

    }

    /**
     * 开启蓝牙
     */
    private void checkIfOpenBlueTooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopScanDevice();
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStartScan:
                startScanDevice();
                break;
            case R.id.btnStopScan:
                stopScanDevice();
                break;
            case R.id.btnSetMode:
                setMode();
                break;
            case R.id.btnGetMode:
                getMode();
                break;
            case R.id.btnSetLanguage:
                setLanguage();
                break;
            case R.id.btnCompareCheck:
                compareCheck();
                break;
            case R.id.btnHearMakeup:
                hearMakeup();
                break;
        }
    }

    //开始扫描设备
    private void startScanDevice() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startDiscovery();
            scanLeDevice(true);
        }
    }

    //停止扫描
    private void stopScanDevice() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
            scanLeDevice(false);
        }
    }


    //扫描Low Energy设备
    private void scanLeDevice(boolean enable) {
        if (enable) {
            Toast.makeText(getApplicationContext(), "开始扫描", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "结束扫描", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }, SCAN_PERIOD);
            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
            Toast.makeText(getApplicationContext(), "结束扫描", Toast.LENGTH_SHORT).show();
        }
    }


    //Low Energy 设备扫描回调
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "onLeScan: " + device.getName() +"===" + device.getAddress());
            if (device != null) {
                if (device.getName() != null && !devicesList.contains(device)) {
                    devicesList.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };


    public void refreshDeviceList(List<BluetoothDevice> list) {
        if (list != null && devicesList != null) {
            devicesList.addAll(list);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    //连接设备reactMethod
    private void connectDevice(int position) {
        //检查配对状态
        if (devicesList.get(position).getBondState() != BluetoothDevice.BOND_BONDED) {
            devicesList.get(position).createBond();
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            bluetoothGatt = devicesList.get(position).connectGatt(
                    getApplicationContext(),
                    false,
                    bluetoothGattCallback,
                    BluetoothDevice.TRANSPORT_LE
            );
        } else {
            bluetoothGatt = devicesList.get(position).connectGatt(
                    getApplicationContext(),
                    false,
                    bluetoothGattCallback
            );
        }
    }


    //连接LE蓝牙回调
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "状态改变:" + status + "----" + newState);
            if (status == 133) {
                Log.d(TAG, "设备初始连接为133，需要重新进行扫描");
                devicesList.clear();
                startScanDevice();
                return;
            }
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChange: 设备已连接 --- 开始发现服务");
                    bluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "onConnectionStateChange: 设备正在连接");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "onConnectionStateChange: 设备连接已断开");
                    break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + characteristic.getUuid()+"==="
                    + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid()+"==="
                    + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: "+descriptor.getUuid()+"===");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered: ====");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendBleNotification();
            }
        }
    };

    //注册蓝牙信息监听
    private void sendBleNotification() {
        if (bluetoothGatt == null) {
            Log.d(TAG, "sendBleNotification: bluetoothGatt is null");
            return;
        }

        BluetoothGattService gattService = bluetoothGatt.getService(UUID.fromString(SERVICE_ID));

        BluetoothGattCharacteristic responseCharacteristic = gattService
                .getCharacteristic(UUID.fromString(RESPONSE_ID));


        BluetoothGattDescriptor responseDescriptor = responseCharacteristic
                .getDescriptor(UUID.fromString(DESCRIPTOR_UUID));

        responseDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (bluetoothGatt.writeDescriptor(responseDescriptor)) {
            bluetoothGatt.setCharacteristicNotification(responseCharacteristic, true);
        }
    }


    //断开设备reactMethod
    private void disConnectDevice() {
        bluetoothGatt.disconnect();
    }

    /**
     * 设置模式 预留@ReactMethod
     */
    private void setMode() {
        byte[] setModePayload = new byte[]{0x01, 0x01};
        buildDataAndSend(setModePayload, setModePayload.length);
    }

    /**
     * 获取模式 预留@ReactMethod
     */
    private void getMode() {
        byte[] getModePayload = new byte[]{0x01, 0x01};
        buildDataAndSend(getModePayload, getModePayload.length);
    }


    /**
     * 语言设置 预留@ReactMethod
     */
    private void setLanguage() {
        byte[] setLanguagePayload = new byte[]{0x01, 0x42, 0x00};
        buildDataAndSend(setLanguagePayload, setLanguagePayload.length);
    }


    /**
     * 对比校验 预留@ReactMethod
     */
    private void compareCheck() {
        byte[] compareCheckPayload = new byte[]{0x01, 0x40, 0x01};
        buildDataAndSend(compareCheckPayload, compareCheckPayload.length);
    }

    /**
     * 听力补偿 预留@ReactMethod
     */
    private void hearMakeup() {
        byte[] hearMakeupPayload = new byte[]{0x01, 0x10, 0x00, 0x7d, -0x01, 0x08};
        buildDataAndSend(hearMakeupPayload, hearMakeupPayload.length);
    }

    private void buildDataAndSend(byte[] payload, int payloadLength) {
        int packetLength = payloadLength + OFFS_PAYLOAD;
        byte[] data = new byte[packetLength];

        data[OFFS_VENDOR_ID_H] = 0x00;
        data[OFFS_VENDOR_ID_L] = 0x0a;
        data[OFFS_COMMAND_ID_H] = 0x02;
        data[OFFS_COMMAND_ID_L] = 0x07;

        for (byte idx = 0; idx < payloadLength; idx++) {
            data[idx + OFFS_PAYLOAD] = payload[idx];
        }


        sendData(data);
    }

    private void sendData(final byte[] frame) {
        Log.d(TAG, "sendData: 拼装完成的frame ===" + Arrays.toString(frame));
        if (bluetoothGatt == null) {
            Log.d(TAG, "sendData: === -1");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                bluetoothGattService = bluetoothGatt.getService(UUID.fromString(SERVICE_ID));
                if (bluetoothGattService == null) {
                    Log.d(TAG, "sendData run: === -2");
                    return;
                }
                cmdCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(COMMAND_ID));
                if (cmdCharacteristic != null) {
                    Log.d(TAG, "sendData run: 发送数据");
                    cmdCharacteristic.setValue(frame);
                    cmdCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    bluetoothGatt.writeCharacteristic(cmdCharacteristic);
                }

            }
        }).start();
    }

}
