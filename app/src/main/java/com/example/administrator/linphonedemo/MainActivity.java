package com.example.administrator.linphonedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.administrator.linphonedemo.linphone.LinphoneManager;
import com.example.administrator.linphonedemo.linphone.LinphonePreferences;
import com.example.administrator.linphonedemo.linphone.LinphoneService;
import com.example.administrator.linphonedemo.linphone.LinphoneUtils;
import com.socks.library.KLog;

import org.linphone.core.LinphoneAccountCreator;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_num;
    private TextView dial;
    private TextView login;
    private static final String TAG = "MainActivity";
    private LinphoneAddress address;
    private LinphonePreferences mPrefs;
    private LinphoneAccountCreator accountCreator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_num = findViewById(R.id.et_num);
        dial = findViewById(R.id.dial);
        login = findViewById(R.id.login);

        dial.setOnClickListener(this);
        login.setOnClickListener(this);


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.login:
                initLinphone();
                break;

            case R.id.dial:
                terminateLinphone();
                break;
        }
    }


    public synchronized void initLinphone() {

        if (LinphoneService.isReady()) {
            onServiceReady("666006", "10.0.5.11", "1324");
        } else {
            startService(new Intent(this, LinphoneService.class));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!LinphoneService.isReady()) {
                        try {
                            sleep(300);
                        } catch (InterruptedException e) {
                            throw new RuntimeException("waiting thread sleep() has been interrupted");
                        }
                    }
                    onServiceReady("666006", "10.0.5.11", "1324");
                }
            }).start();
        }
    }

    public void terminateLinphone() {
        //linphoneLauncher.terminate();
    }

    private void onServiceReady(String username, String domain, String password) {
        mPrefs = LinphonePreferences.instance();

        accountCreator = LinphoneCoreFactory.instance().createAccountCreator(LinphoneManager.getLcIfManagerNotDestroyedOrNull(), LinphonePreferences.instance().getConfig().getString("assistant", "xmlrpc_url", null));
        accountCreator.setDomain(domain);
        accountCreator.setListener(LinphoneManager.getInstance());

        //удаление старых учеток
        for (LinphoneProxyConfig str : LinphoneManager.getLc().getProxyConfigList()) {
            try {
                LinphoneAddress addr = LinphoneCoreFactory.instance().createLinphoneAddress(str.getIdentity());
                LinphoneAuthInfo authInfo = LinphoneManager.getLc().findAuthInfo(addr.getUserName(), null, addr.getDomain());
                if (authInfo != null) {
                    LinphoneManager.getLc().removeAuthInfo(authInfo);
                }
            } catch (Exception e) {
            }

            if (str != null) {
                LinphoneManager.getLc().removeProxyConfig(str);
                LinphoneManager.getLc().setDefaultProxyConfig(null);
            }
        }

        LinphoneManager.getLc().refreshRegisters();


        username = LinphoneUtils.getDisplayableUsernameFromAddress(username);
        domain = LinphoneUtils.getDisplayableUsernameFromAddress(domain);

        String identity = "sip:" + username + "@" + domain;
        KLog.i(TAG, "identity = " + identity);
        try {
            address = LinphoneCoreFactory.instance().createLinphoneAddress(identity);
            KLog.i(TAG, "address = " + address);
        } catch (LinphoneCoreException e) {
            Log.e(e);
        }

        LinphonePreferences.AccountBuilder builder = new LinphonePreferences.AccountBuilder(LinphoneManager.getLc())
                .setUsername(username)
                .setDomain(domain)
                .setPassword(password);


        String forcedProxy = "";
        if (!TextUtils.isEmpty(forcedProxy)) {
            builder.setProxy(forcedProxy)
                    .setOutboundProxyEnabled(true)
                    .setAvpfRRInterval(5);
        }

        builder.setTransport(LinphoneAddress.TransportType.LinphoneTransportUdp);


        try {
            builder.saveNewAccount();
        } catch (LinphoneCoreException e) {
            e.printStackTrace();

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
