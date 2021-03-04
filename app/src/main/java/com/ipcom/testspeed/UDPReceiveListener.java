package com.ipcom.testspeed;

/**
 * @Description: []
 * @Author: skyHuang
 * @CreateDate: 2021/3/4 14:50
 * @UpdateUser: []
 * @UpdateDate: 2021/3/4 14:50
 * @UpdateRemark: []
 */
public interface UDPReceiveListener {

    void receiveSuccess(byte data);

    void receiveFailed(int error);

}
