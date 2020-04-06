package com.bit.bletest;

public class Consts {
    public static final int REQUEST_COARCH_LOCATION = 0111;
    public static final int REQUEST_ENABLE_BT = 0112;
    //默认扫描时间 10s
    public static final long SCAN_PERIOD = 10000;

    private static String GATT_UUID = "-0000-1000-8000-00805f9b34fb";
    private static String CSR_UUID = "-D102-11E1-9B23-00025B00A5A5";
    //接受蓝牙端消息时需要用到descriptor uuid
    public static final String DESCRIPTOR_UUID = "00002902" + GATT_UUID;

    //服务id
    public static final String SERVICE_ID = "00001100" + CSR_UUID;

    //向蓝牙发送指令时用到的id
    public static final String COMMAND_ID = "00001101" + CSR_UUID;

    //接受蓝牙信息时的characteristic uuid
    public static final String RESPONSE_ID = "00001102" + CSR_UUID;


    public static final String DATA_ID = "00001103" + CSR_UUID;

    public static final int OFFS_VENDOR_ID = 0;
    public static final int OFFS_VENDOR_ID_H = OFFS_VENDOR_ID;
    public static final int OFFS_VENDOR_ID_L = OFFS_VENDOR_ID + 1;
    public static final int OFFS_COMMAND_ID = 2;
    public static final int OFFS_COMMAND_ID_H = OFFS_COMMAND_ID;
    public static final int OFFS_COMMAND_ID_L = OFFS_COMMAND_ID + 1;
    public static final int OFFS_PAYLOAD = 4;
    public static final int PROTOCOL_VERSION = 1;

}
