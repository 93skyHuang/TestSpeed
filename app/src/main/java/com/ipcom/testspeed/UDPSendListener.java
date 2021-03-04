package com.ipcom.testspeed;

/**
 * @Description: []
 * @Author: skyHuang
 * @CreateDate: 2021/3/4 14:51
 * @UpdateUser: []
 * @UpdateDate: 2021/3/4 14:51
 * @UpdateRemark: []
 */
public interface UDPSendListener {
    void sendSuccess(byte data);

    void sendFailed(int error);

}
