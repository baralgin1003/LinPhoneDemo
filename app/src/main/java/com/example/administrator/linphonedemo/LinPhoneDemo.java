package com.example.administrator.linphonedemo;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import com.example.administrator.linphonedemo.linphone.BluetoothManager;
import com.example.administrator.linphonedemo.linphone.EmptyLauncher;
import com.example.administrator.linphonedemo.linphone.ILinphoneLauncher;
import com.example.administrator.linphonedemo.linphone.LinphoneLauncher;
import com.example.administrator.linphonedemo.linphone.LinphoneService;
import com.socks.library.KLog;

import org.linphone.mediastream.Version;

public class LinPhoneDemo extends Application {
    private static final String TAG = "LinPhoneDemo";
    ServiceConnection serviceConnection;
    ILinphoneLauncher linphoneLauncher = EmptyLauncher.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();
        KLog.i(TAG, "========== LinPhoneDemo.onCreate");

        initLinphone();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        KLog.i(TAG, "========== LinPhoneDemo.onTerminate");
    }

    private void initLinphone() {
        KLog.i(TAG, "========== BaseActivity.initLinphone");
        KLog.i(TAG, "========== LinphoneService.isReady() = " + LinphoneService.isReady());
        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            Intent intent = new Intent(this, LinphoneService.class);
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    KLog.i(TAG, "========== BaseActivity.onServiceConnected  " + "name = [" + name + "], service = [" + service + "]");
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    KLog.i(TAG, "========== BaseActivity.onServiceDisconnected  " + "name = [" + name + "]");
                }
            };
            bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);

            mThread = new ServiceWaitThread();
            mThread.start();
        }
    }

    private void onServiceReady() {
        // We need LinphoneService to start bluetoothManager    蓝牙适配 暂不需要
//        if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
//            BluetoothManager.getInstance().initBluetooth();
//        }


        linphoneLauncher = LinphoneLauncher.getInstance(this);
    }

    private Handler mainHandler = new Handler();
    private ServiceWaitThread mThread;

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }
}
