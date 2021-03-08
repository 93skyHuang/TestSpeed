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
    private static final int RECEIVE_PORT = 8888;
    private static final int SEND_PORT = 8888;//测速报文目的端口号为8888，后续会改为10008
    private static final int RECEIVE_TIME_OUT_MILLISECONDS = 30000;//接收超时异常
    private DatagramSocket datagramSocket;
    private Thread receiveThread;//接收线程
    private Thread sendThread;//发送线程
    private boolean isReceiveUdp;
    private boolean isSendUdp;
    private long receiveLength = 0;

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

    public void sendUdpData(final byte[] message) {
        try {
            InetAddress broadcastAddress = getBroadcastAddress();
            Log.i(TAG, "sendBroadcastData" + " broadcastAddress" + broadcastAddress.toString());
            final DatagramPacket packet = new DatagramPacket(message, message.length, broadcastAddress, SEND_PORT);
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

    public synchronized void startReceiveUdp() {
        isReceiveUdp = true;
        receiveLength = 0;
        Log.i(TAG, "startReceiveUdp: ");
        allowMulticast();
        if (receiveThread == null || !receiveThread.isAlive()) {
            receiveThread = new Thread(() -> {
                try {
                    initDatagramSocket();
                } catch (SocketException e) {
                    Log.e(TAG, "startReceiveUdp: " + e);
                    e.printStackTrace();
                }
                while (isReceiveUdp) {
                    try {
                        receiveListener();
                    } catch (IOException e) {
                        Log.e(TAG, "startReceiveUdp: " + e);
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
            receiveLength = receiveLength + packet.getLength();
            Log.i(TAG, "receiveListener: " + receiveLength + "packet.getLength()" + packet.getLength());
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "listenForResponses Receive timed out");
        }
    }

    public void stopReceiveUdp() {
        Log.i(TAG, "stopReceiveUdp: ");
        isReceiveUdp = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
            datagramSocket = null;
        }
        receiveThread = null;
        receiveLength = 0;
        releaseMulticastLock();
    }

    public synchronized void startSendUdp() {
        isSendUdp = true;
        Log.i(TAG, "startSendUdp: ");
        if (sendThread == null || !sendThread.isAlive()) {
            sendThread = new Thread(() -> {
                while (isSendUdp) {
                    byte[] bytes = new byte[1024];
                    Log.i(TAG, "send" + Arrays.toString(bytes));
                    sendUdpData(bytes);
                }
            });
            sendThread.start();
        }
    }

    public void stopSendUdp() {
        Log.i(TAG, "stopSendUdp: ");
        isSendUdp = false;
        if (sendThread != null) {
            sendThread.interrupt();
        }
        sendThread = null;
    }

    private WifiManager.MulticastLock multicastLock;

    //这个请求不能超过50个 所以打开后需要即是的关闭
    public void allowMulticast() {
        if (multicastLock == null) {
            WifiManager wifiManager = (WifiManager) MyApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("multicast.udp");
                Log.i(TAG, "allowMulticast  isHeld=" + multicastLock.isHeld());
                multicastLock.acquire();
            }
        }
    }

    public void releaseMulticastLock() {
        if (multicastLock != null) {
            Log.i(TAG, "releaseMulticastLock");
            multicastLock.release();
            multicastLock = null;
        }
    }

}
