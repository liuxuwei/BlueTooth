package com.bit.bletest;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class DeviceAdapter extends BaseAdapter {
    private Context context;
    private List<BluetoothDevice> deviceList;

    public DeviceAdapter(Context context, List<BluetoothDevice> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_device, parent, false);
            holder = new ViewHolder();
            holder.tvDeviceName = convertView.findViewById(R.id.tvDeviceName);
            holder.tvDeviceAddress = convertView.findViewById(R.id.tvDeviceAddress);
            holder.tvDeviceType = convertView.findViewById(R.id.tvDeviceType);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvDeviceName.setText("设备名称：\n"+deviceList.get(position).getName());
        holder.tvDeviceAddress.setText("设备地址：\n"+deviceList.get(position).getAddress());
        switch (deviceList.get(position).getType()) {
            case 1:
                holder.tvDeviceType.setText("设备类型：\nDEVICE_TYPE_CLASSIC");
                break;
            case 2:
                holder.tvDeviceType.setText("设备类型：\nDEVICE_TYPE_LE");
                break;
            case 3:
                holder.tvDeviceType.setText("设备类型：\nDEVICE_TYPE_DUAL");
                break;
        }


        return convertView;
    }

    public static class ViewHolder{
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        TextView tvDeviceType;
    }
}
