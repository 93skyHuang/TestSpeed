package com.ipcom.testspeed;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @Description: []
 * @Author: skyHuang
 * @CreateDate: 2021/3/4 10:04
 * @UpdateUser: []
 * @UpdateDate: 2021/3/4 10:04
 * @UpdateRemark: []
 */
public class UdpManager {
    private static final String TAG = "UdpManager";
    private static UdpManager instance;
    private static final int RECEIVE_PORT = 10007;
    private static final int SEND_PORT = 10008;
    private DatagramSocket datagramSocket;
    private Thread receiveThread;//接收线程
    private Thread sendThread;//发送线程
    private boolean isReceiveUdp;
    private boolean isSendUdp;

    public static UdpManager getInstance() {
        if (instance == null) {
            synchronized (UdpManager.class) {
                if (instance == null) {
                    instance = new UdpManager();
                }
            }
        }
        return instance;
    }

    private void initDatagramSocket() throws SocketException {
        if (datagramSocket == null || datagramSocket.isClosed()) {
            Log.i(TAG, "initDatagramSocket()");
            datagramSocket = new DatagramSocket(null);
            datagramSocket.setReuseAddress(true);
            datagramSocket.bind(new InetSocketAddress(RECEIVE_PORT));
            datagramSocket.setBroadcast(true);
        }
    }

    public void sendBroadcastData(final byte[] message) {
        try {
            Log.i(TAG, "sendBroadcastData" + Arrays.toString(message));
            final DatagramPacket packet = new DatagramPacket(message, message.length, getBroadcastAddress(), SEND_PORT);
            initDatagramSocket();
            datagramSocket.send(packet);
        } catch (Exception e) {
            Log.e(TAG, "" + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) MyApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


    public void startReceiveUdp() {
        isReceiveUdp = true;
        if (receiveThread == null || !receiveThread.isAlive()) {
            receiveThread = new Thread(() -> {
                while (isReceiveUdp) {
                    try {
                        receiveListener();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            receiveThread.start();
        }
    }

    //数据接收
    private void receiveListener() throws IOException {
        byte[] buf = new byte[1024];
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            if (datagramSocket == null) {
                stopReceiveUdp();
                return;
            }
            datagramSocket.receive(packet);

        } catch (SocketTimeoutException e) {
            Log.e(TAG, "listenForResponses Receive timed out");
        }
    }

    public void stopReceiveUdp() {
        isReceiveUdp = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        receiveThread = null;
    }


    public void startSendUdp() {
        if (sendThread != null && !sendThread.isAlive()) {
            sendThread = new Thread(() -> {

            });
            sendThread.start();
        }

    }

    public void stopSendUdp() {
        isSendUdp = false;
        if (sendThread != null) {
            sendThread.interrupt();
        }
        sendThread = null;
    }

}
