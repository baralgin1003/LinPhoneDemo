package com.example.administrator.linphonedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.linphonedemo.linphone.DataFetcher;
import com.example.administrator.linphonedemo.linphone.ILinphoneLauncher;
import com.example.administrator.linphonedemo.linphone.LinphoneLauncher;
import com.example.administrator.linphonedemo.linphone.LinphoneManager;
import com.example.administrator.linphonedemo.linphone.LinphonePreferences;
import com.socks.library.KLog;

import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneProxyConfig;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_num;
    private TextView dial;
    private TextView login;
    private static final String TAG = "MainActivity";
    private LinPhoneDemo application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_num = findViewById(R.id.et_num);
        dial = findViewById(R.id.dial);
        login = findViewById(R.id.login);
        application = (LinPhoneDemo) getApplication();

        dial.setOnClickListener(this);
        login.setOnClickListener(this);



    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dial:
                // TODO: 2018/6/29 开始拨号
                String trim = et_num.getText().toString().trim();
                int anInt = Integer.parseInt(trim);
                if (TextUtils.isEmpty(trim) || anInt < 1000 || anInt > 1020) {
                    Toast.makeText(this, "号码不可用", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    if (!LinphoneManager.getInstance().acceptCallIfIncomingPending()) {
                        KLog.i(TAG,"直接创建通话");
                        LinphoneManager.getInstance().newOutgoingCall(trim,trim);
                    } else {
                        KLog.i(TAG, "========== 无法直接创建通话 = " + "false");
                        if (LinphonePreferences.instance().isBisFeatureEnabled()) {//从未设置过，永远返回默认值true
                            LinphoneCallLog[] logs = LinphoneManager.getLc().getCallLogs();
                            LinphoneCallLog firstOutgoingCallLog = null;
                            for (LinphoneCallLog log : logs) {
                                if (log.getDirection() == CallDirection.Outgoing) {
                                    firstOutgoingCallLog = log;
                                    KLog.i(TAG, "========== 选中的日志 firstOutgoingCallLog = " + firstOutgoingCallLog);
                                    break;
                                }
                            }
                            if (firstOutgoingCallLog == null) {
                                return;
                            }

                            LinphoneProxyConfig defaultProxyConfig = LinphoneManager.getLc().getDefaultProxyConfig();
                            if (defaultProxyConfig != null && firstOutgoingCallLog.getTo().getDomain().equals(defaultProxyConfig.getDomain())) {
                                et_num.setText(firstOutgoingCallLog.getTo().getUserName());
                            } else {
                                et_num.setText(firstOutgoingCallLog.getTo().asStringUriOnly());
                            }
                            et_num.setSelection(et_num.getText().toString().length());
                        }
                    }
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                    LinphoneManager.getInstance().terminateCall();
                    Toast.makeText(application, "无法创建链接", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.login:
                application.linphoneLauncher.signUp(null, "1017", "172.16.12.234", "LinPhoneDemo", null, "1234", null, LinphoneAddress.TransportType.LinphoneTransportUdp, new DataFetcher<String>() {
                    @Override
                    public void onSuccess(String s) {
                        KLog.i(TAG, "========== BaseActivity.onSuccess 注册方法调用完毕 " + "s = [" + s + "]");
                        Toast.makeText(application, "登录成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        KLog.i(TAG, "========== BaseActivity.onException  " + "注册方法调用出错 = [" + throwable.getMessage() + "]");
                    }
                });
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(application.serviceConnection);
    }
}
