package com.example.administrator.linphonedemo.linphone;

import android.content.Context;
import android.widget.Toast;

import org.linphone.core.LinphoneAddress;

public class EmptyLauncher implements ILinphoneLauncher {

    private static EmptyLauncher emptyLauncher;

    public static EmptyLauncher getInstance() {
        if (emptyLauncher == null) {
            synchronized (EmptyLauncher.class) {
                if (emptyLauncher == null) {
                    emptyLauncher = new EmptyLauncher();
                }
            }
        }
        return emptyLauncher;
    }

    private EmptyLauncher() {
    }

    @Override
    public void signUp(Context context, String username, String domain, String displayName, String ha1, String password, String prefix, LinphoneAddress.TransportType transport, DataFetcher<String> dataFetcher) {
        showEmptyToast(context);
    }

    private void showEmptyToast(Context context) {
        Toast.makeText(context, "服务启动中，请稍候...", Toast.LENGTH_SHORT).show();
    }
}
