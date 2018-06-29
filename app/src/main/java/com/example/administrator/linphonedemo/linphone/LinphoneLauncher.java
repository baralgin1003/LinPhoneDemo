package com.example.administrator.linphonedemo.linphone;

import android.content.Context;
import android.text.TextUtils;

import com.example.administrator.linphonedemo.R;
import com.socks.library.KLog;

import org.linphone.core.LinphoneAccountCreator;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.mediastream.Log;

public class LinphoneLauncher implements ILinphoneLauncher {

    private static LinphoneLauncher linphoneLauncher;
    private static final String TAG = "Linphone";
    private Context context;
    private LinphoneAddress address;
    private LinphonePreferences mPrefs;
    private LinphoneAccountCreator accountCreator;

    private LinphoneLauncher() {
        mPrefs = LinphonePreferences.instance();

    }

    public static <T extends Context> ILinphoneLauncher getInstance(T context) {
        if (linphoneLauncher == null) {
            synchronized (LinphoneLauncher.class){
                if (linphoneLauncher == null) {
                    linphoneLauncher = new LinphoneLauncher();
                }
            }
        }
        linphoneLauncher.context = context;
        linphoneLauncher.accountCreator = LinphoneCoreFactory.instance().createAccountCreator(LinphoneManager.getLc(), LinphonePreferences.instance().getXmlrpcUrl());
        linphoneLauncher.accountCreator.setDomain(context.getResources().getString(R.string.default_domain));
        linphoneLauncher.accountCreator.setListener(LinphoneManager.getInstance());
        return linphoneLauncher;
    }

    @Override
    public void signUp(Context context, String username, String domain, String displayName, String ha1, String password, String prefix, LinphoneAddress.TransportType transport, DataFetcher<String> dataFetcher){
        // TODO: 2018/6/28 判断是否已经注册
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

        boolean isMainAccountLinphoneDotOrg = domain.equals(this.context.getString(R.string.default_domain));
        LinphonePreferences.AccountBuilder builder = new LinphonePreferences.AccountBuilder(LinphoneManager.getLc())
                .setUsername(username)
                .setDomain(domain)
                .setDisplayName(displayName)
                .setHa1(ha1)
                .setPassword(password);

        if(prefix != null){
            builder.setPrefix(prefix);
        }

        if (isMainAccountLinphoneDotOrg) {
            if (this.context.getResources().getBoolean(R.bool.disable_all_security_features_for_markets)) {
                builder.setProxy(domain)
                        .setTransport(LinphoneAddress.TransportType.LinphoneTransportTcp);
            }
            else {
                builder.setProxy(domain)
                        .setTransport(LinphoneAddress.TransportType.LinphoneTransportTls);
            }

            builder
//                    .setExpires("604800") 默认的试用期7天删除掉
                    .setAvpfEnabled(true)
                    .setAvpfRRInterval(3)
                    .setQualityReportingCollector("sip:voip-metrics@sip.linphone.org")
                    .setQualityReportingEnabled(true)
                    .setQualityReportingInterval(180)
                    .setRealm("sip.linphone.org")
                    .setNoDefault(false);

            mPrefs.enabledFriendlistSubscription(this.context.getResources().getBoolean(R.bool.use_friendlist_subscription));

            mPrefs.setStunServer(this.context.getString(R.string.default_stun));
            mPrefs.setIceEnabled(true);

            accountCreator.setPassword(password);
            accountCreator.setHa1(ha1);
            accountCreator.setUsername(username);
            accountCreator.setDisplayName(username);
        } else {
            String forcedProxy = "";
            if (!TextUtils.isEmpty(forcedProxy)) {
                builder.setProxy(forcedProxy)
                        .setOutboundProxyEnabled(true)
                        .setAvpfRRInterval(5);
            }

            if(transport != null) {
                builder.setTransport(transport);
            }
        }

        if (this.context.getResources().getBoolean(R.bool.enable_push_id)) {
            String regId = mPrefs.getPushNotificationRegistrationID();
            String appId = this.context.getString(R.string.push_sender_id);
            if (regId != null && mPrefs.isPushNotificationEnabled()) {
                String contactInfos = "app-id=" + appId + ";pn-type=google;pn-tok=" + regId;
                builder.setContactParameters(contactInfos);
            }
        }

        try {
            builder.saveNewAccount();
//            if(!newAccount) {
//                displayRegistrationInProgressDialog();
//            }
//            accountCreated = true;
            dataFetcher.onSuccess(accountCreator.getDisplayName());
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
            dataFetcher.onException(e);
        }
    }
}
