package com.ipcom.testspeed;

import android.app.Application;

/**
 * @Description: []
 * @Author: skyHuang
 * @CreateDate: 2021/3/4 15:10
 * @UpdateUser: []
 * @UpdateDate: 2021/3/4 15:10
 * @UpdateRemark: []
 */
public class MyApplication extends Application {

    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
