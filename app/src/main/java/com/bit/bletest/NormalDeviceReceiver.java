package com.bit.bletest;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class NormalDeviceReceiver extends BroadcastReceiver {
    private List<BluetoothDevice> deviceList;
    private MainActivity mObserver;

    public NormalDeviceReceiver(MainActivity observer) {
        mObserver = observer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        deviceList = new ArrayList<>();
        String action = intent.getAction();
        if (action.equals(BluetoothDevice.ACTION_FOUND)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (!device.getName().equals("")) {
                deviceList.add(device);
                mObserver.refreshDeviceList(deviceList);
            }
        }
    }

}
