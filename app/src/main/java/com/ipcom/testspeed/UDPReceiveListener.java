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

    /**
     *  长度  byte
     * @param length
     */
    void receiveSuccess(long length,long allLength);

    void receiveFailed(int error);

}
