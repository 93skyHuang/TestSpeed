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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

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
    private AtomicLong mAtomicLongOfReceiver;
    private long mAllLengthOfReceiver;
    private int testCount = 10;
    private int count = 0;
    private Disposable receiverDisposable;
    private UDPReceiveListener mUDPReceiveListener;

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
        Log.i(TAG, "getBroadcastAddress: " + Arrays.toString(quads));
        return InetAddress.getByAddress(quads);
    }

    public synchronized void startReceiveUdp() {
        isReceiveUdp = true;
        mAtomicLongOfReceiver = new AtomicLong(0);
        mAllLengthOfReceiver = 0;
        startTimer();
        Log.i(TAG, "startReceiveUdp: ");
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
            int packetLength = packet.getLength();
            mAtomicLongOfReceiver.addAndGet(packetLength);
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
        receiveThread = null;
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
            datagramSocket = null;
        }
        cancelTimer(receiverDisposable);
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

    //定时器
    private void startTimer() {
        if (receiverDisposable == null || receiverDisposable.isDisposed()) {
            count = 0;
            receiverDisposable = Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            long curLength = mAtomicLongOfReceiver.getAndSet(0);
                            mAllLengthOfReceiver = mAllLengthOfReceiver + curLength;
                            count++;
                            if (count == testCount) {
                                cancelTimer(receiverDisposable);
                                if (mUDPReceiveListener != null) {
                                    mUDPReceiveListener.receiveSuccess(curLength, mAllLengthOfReceiver);
                                }
                            } else {
                                if (mUDPReceiveListener != null) {
                                    mUDPReceiveListener.receiveSuccess(curLength, -1);
                                }
                            }
                        }
                    });
        }
    }

    /**
     * 取消时间订阅
     */
    private void cancelTimer(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public void setUDPReceiveListener(UDPReceiveListener mUDPReceiveListener) {
        this.mUDPReceiveListener = mUDPReceiveListener;
    }
}
