package com.ipcom.testspeed;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @Description: []
 * @Author: skyHuang
 * @CreateDate: 2021/3/4 15:51
 * @UpdateUser: []
 * @UpdateDate: 2021/3/4 15:51
 * @UpdateRemark: []
 */
public class UDPActivity extends AppCompatActivity {

    UdpManager udpManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp);
        udpManager = UdpManager.getInstance();
        findViewById(R.id.udp_send).setOnClickListener(v -> {
            TextView tv = (TextView) v;
            String str = tv.getText().toString();
            if (str.equals("发送UDP")) {
                tv.setText("停止UDP发送");
                udpManager.startSendUdp();
            } else {
                tv.setText("发送UDP");
                udpManager.stopSendUdp();
            }
        });
        findViewById(R.id.udp_receive).setOnClickListener(v -> {
            TextView tv = (TextView) v;
            String str = tv.getText().toString();
            if (str.equals("接收UDP")) {
                tv.setText("停止接收UDP");
                udpManager.startReceiveUdp();
            } else {
                tv.setText("接收UDP");
                udpManager.stopReceiveUdp();
            }
        });
        udpManager.setUDPReceiveListener(new UDPReceiveListener() {
            @Override
            public void receiveSuccess(long length, long allLength) {
                ((TextView) findViewById(R.id.tv_receive)).setText("接收长度" + length);
            }

            @Override
            public void receiveFailed(int error) {

            }
        });
    }
}
