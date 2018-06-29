package com.example.administrator.linphonedemo.linphone;

import android.content.Context;

import org.linphone.core.LinphoneAddress;

public interface ILinphoneLauncher {
    /**
     *  用户登录
     *  @param context Context实例
     *  @param username 用户名
     * */
    void signUp(Context context, String username, String domain, String displayName, String ha1, String password, String prefix, LinphoneAddress.TransportType transport, DataFetcher<String> dataFetcher);
}
