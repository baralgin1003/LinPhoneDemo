package com.example.administrator.linphonedemo.linphone;

/*
LinphonePreferences.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import com.example.administrator.linphonedemo.R;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAddress.TransportType;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.AdaptiveRateAlgorithm;
import org.linphone.core.LinphoneCore.LinphoneLimeState;
import org.linphone.core.LinphoneCore.MediaEncryption;
import org.linphone.core.LinphoneCore.Transports;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneNatPolicy;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.LpConfig;
import org.linphone.core.TunnelConfig;
import org.linphone.mediastream.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LinphonePreferences {
    private static final int LINPHONE_CORE_RANDOM_PORT = -1;
    private static LinphonePreferences instance;
    private Context mContext;
    private String basePath;

    public static final synchronized LinphonePreferences instance() {
        if (instance == null) {
            instance = new LinphonePreferences();
        }
        return instance;
    }

    private LinphonePreferences() {

    }

    public void setContext(Context c) {
        mContext = c;
        basePath = mContext.getFilesDir().getAbsolutePath();
    }

    private String getString(int key) {
        if (mContext == null && LinphoneManager.isInstanciated()) {
            mContext = LinphoneManager.getInstance().getContext();
        }

        return mContext.getString(key);
    }

    private LinphoneCore getLc() {
        if (!LinphoneManager.isInstanciated())
            return null;

        return LinphoneManager.getLcIfManagerNotDestroyedOrNull();
    }

    public LpConfig getConfig() {
        LinphoneCore lc = getLc();
        if (lc != null) {
            return lc.getConfig();
        }

        if (!LinphoneManager.isInstanciated()) {
            File linphonerc = new File(basePath + "/.linphonerc");
            if (linphonerc.exists()) {
                return LinphoneCoreFactory.instance().createLpConfig(linphonerc.getAbsolutePath());
            } else if (mContext != null) {
                InputStream inputStream = mContext.getResources().openRawResource(R.raw.linphonerc_default);
                InputStreamReader inputreader = new InputStreamReader(inputStream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                StringBuilder text = new StringBuilder();
                String line;
                try {
                    while ((line = buffreader.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                } catch (IOException ioe) {
                    Log.e(ioe);
                }
                return LinphoneCoreFactory.instance().createLpConfigFromString(text.toString());
            }
        } else {
            return LinphoneCoreFactory.instance().createLpConfig(LinphoneManager.getInstance().mLinphoneConfigFile);
        }
        return null;
    }


    public String getRingtone(String defaultRingtone) {
        String ringtone = getConfig().getString("app", "ringtone", defaultRingtone);
        if (ringtone == null || ringtone.length() == 0)
            ringtone = defaultRingtone;
        return ringtone;
    }

    // Accounts settings
    private LinphoneProxyConfig getProxyConfig(int n) {
        LinphoneProxyConfig[] prxCfgs = getLc().getProxyConfigList();
        if (n < 0 || n >= prxCfgs.length)
            return null;
        return prxCfgs[n];
    }

    private LinphoneAuthInfo getAuthInfo(int n) {
        LinphoneProxyConfig prxCfg = getProxyConfig(n);
        if (prxCfg == null) return null;
        try {
            LinphoneAddress addr = LinphoneCoreFactory.instance().createLinphoneAddress(prxCfg.getIdentity());
            LinphoneAuthInfo authInfo = getLc().findAuthInfo(addr.getUserName(), null, addr.getDomain());
            return authInfo;
        } catch (LinphoneCoreException e) {
            Log.e(e);
        }

        return null;
    }


    /**
     * Saves a authInfo into the core.
     * Useful to save the changes made to a cloned authInfo.
     */
    private void saveAuthInfo(LinphoneAuthInfo authInfo) {
        getLc().addAuthInfo(authInfo);
    }

    public static class AccountBuilder {
        private LinphoneCore lc;
        private String tempUsername;
        private String tempDisplayName;
        private String tempUserId;
        private String tempPassword;
        private String tempHa1;
        private String tempDomain;
        private String tempProxy;
        private String tempRealm;
        private String tempPrefix;
        private boolean tempOutboundProxy;
        private String tempContactsParams;
        private String tempExpire;
        private TransportType tempTransport;
        private boolean tempAvpfEnabled = false;
        private int tempAvpfRRInterval = 0;
        private String tempQualityReportingCollector;
        private boolean tempQualityReportingEnabled = false;
        private int tempQualityReportingInterval = 0;
        private boolean tempEnabled = true;
        private boolean tempNoDefault = false;


        public AccountBuilder(LinphoneCore lc) {
            this.lc = lc;
        }

        public AccountBuilder setTransport(TransportType transport) {
            tempTransport = transport;
            return this;
        }

        public AccountBuilder setUsername(String username) {
            tempUsername = username;
            return this;
        }

        public AccountBuilder setDisplayName(String displayName) {
            tempDisplayName = displayName;
            return this;
        }

        public AccountBuilder setPassword(String password) {
            tempPassword = password;
            return this;
        }

        public AccountBuilder setHa1(String ha1) {
            tempHa1 = ha1;
            return this;
        }

        public AccountBuilder setDomain(String domain) {
            tempDomain = domain;
            return this;
        }

        public AccountBuilder setProxy(String proxy) {
            tempProxy = proxy;
            return this;
        }

        public AccountBuilder setOutboundProxyEnabled(boolean enabled) {
            tempOutboundProxy = enabled;
            return this;
        }

        public AccountBuilder setContactParameters(String contactParams) {
            tempContactsParams = contactParams;
            return this;
        }

        public AccountBuilder setExpires(String expire) {
            tempExpire = expire;
            return this;
        }

        public AccountBuilder setUserId(String userId) {
            tempUserId = userId;
            return this;
        }

        public AccountBuilder setAvpfEnabled(boolean enable) {
            tempAvpfEnabled = enable;
            return this;
        }

        public AccountBuilder setAvpfRRInterval(int interval) {
            tempAvpfRRInterval = interval;
            return this;
        }

        public AccountBuilder setRealm(String realm) {
            tempRealm = realm;
            return this;
        }

        public AccountBuilder setQualityReportingCollector(String collector) {
            tempQualityReportingCollector = collector;
            return this;
        }

        public AccountBuilder setPrefix(String prefix) {
            tempPrefix = prefix;
            return this;
        }

        public AccountBuilder setQualityReportingEnabled(boolean enable) {
            tempQualityReportingEnabled = enable;
            return this;
        }

        public AccountBuilder setQualityReportingInterval(int interval) {
            tempQualityReportingInterval = interval;
            return this;
        }

        public AccountBuilder setEnabled(boolean enable) {
            tempEnabled = enable;
            return this;
        }

        public AccountBuilder setNoDefault(boolean yesno) {
            tempNoDefault = yesno;
            return this;
        }

        /**
         * Creates a new account
         *
         * @throws LinphoneCoreException
         */
        public void saveNewAccount() throws LinphoneCoreException {

            if (tempUsername == null || tempUsername.length() < 1 || tempDomain == null || tempDomain.length() < 1) {
                Log.w("Skipping account save: username or domain not provided");
                return;
            }

            String identity = "sip:" + tempUsername + "@" + tempDomain;
            String proxy = "sip:";
            if (tempProxy == null) {
                proxy += tempDomain;
            } else {
                if (!tempProxy.startsWith("sip:") && !tempProxy.startsWith("<sip:")
                        && !tempProxy.startsWith("sips:") && !tempProxy.startsWith("<sips:")) {
                    proxy += tempProxy;
                } else {
                    proxy = tempProxy;
                }
            }
            LinphoneAddress proxyAddr = LinphoneCoreFactory.instance().createLinphoneAddress(proxy);
            LinphoneAddress identityAddr = LinphoneCoreFactory.instance().createLinphoneAddress(identity);

            if (tempDisplayName != null) {
                identityAddr.setDisplayName(tempDisplayName);
            }

            if (tempTransport != null) {
                proxyAddr.setTransport(tempTransport);
            }

            String route = tempOutboundProxy ? proxyAddr.asStringUriOnly() : null;

            LinphoneProxyConfig prxCfg = lc.createProxyConfig(identityAddr.asString(), proxyAddr.asStringUriOnly(), route, tempEnabled);

            if (tempContactsParams != null)
                prxCfg.setContactUriParameters(tempContactsParams);
            if (tempExpire != null) {
                try {
                    prxCfg.setExpires(Integer.parseInt(tempExpire));
                } catch (NumberFormatException nfe) {
                    throw new LinphoneCoreException(nfe);
                }
            }

            prxCfg.enableAvpf(tempAvpfEnabled);
            prxCfg.setAvpfRRInterval(tempAvpfRRInterval);
            prxCfg.enableQualityReporting(tempQualityReportingEnabled);
            prxCfg.setQualityReportingCollector(tempQualityReportingCollector);
            prxCfg.setQualityReportingInterval(tempQualityReportingInterval);



            if (tempPrefix != null) {
                prxCfg.setDialPrefix(tempPrefix);
            }


            if (tempRealm != null)
                prxCfg.setRealm(tempRealm);

            LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance().createAuthInfo(tempUsername, tempUserId, tempPassword, tempHa1, tempRealm, tempDomain);

            lc.addProxyConfig(prxCfg);
            lc.addAuthInfo(authInfo);

            if (!tempNoDefault)
                lc.setDefaultProxyConfig(prxCfg);
        }
    }

    public boolean isAccountEnabled(int n) {
        return getProxyConfig(n).registerEnabled();
    }

    // Video settings
    public boolean useFrontCam() {
        return getConfig().getBool("app", "front_camera_default", true);
    }

    public boolean isVideoEnabled() {
        return getLc().isVideoSupported() && getLc().isVideoEnabled();
    }


    public boolean shouldInitiateVideoCall() {
        return getLc().getVideoAutoInitiatePolicy();
    }


    public boolean shouldAutomaticallyAcceptVideoRequests() {
        return getLc().getVideoAutoAcceptPolicy();
    }



    public boolean isWifiOnlyEnabled() {
        return getConfig().getBool("app", "wifi_only", false);
    }


    public boolean isDebugEnabled() {
        return getConfig().getBool("app", "debug", false);
    }


    // Tunnel settings
    private TunnelConfig tunnelConfig = null;

    public TunnelConfig getTunnelConfig() {
        if (getLc().isTunnelAvailable()) {
            if (tunnelConfig == null) {
                TunnelConfig servers[] = getLc().tunnelGetServers();
                if (servers.length > 0) {
                    tunnelConfig = servers[0];
                } else {
                    tunnelConfig = LinphoneCoreFactory.instance().createTunnelConfig();
                }
            }
            return tunnelConfig;
        } else {
            return null;
        }
    }


    public String getTunnelMode() {
        return getConfig().getString("app", "tunnel", null);
    }



    public boolean firstTimeAskingForPermission(String permission) {
        return firstTimeAskingForPermission(permission, true);
    }

    public boolean firstTimeAskingForPermission(String permission, boolean toggle) {
        boolean firstTime = getConfig().getBool("app", permission, true);
        if (toggle) {
            permissionHasBeenAsked(permission);
        }
        return firstTime;
    }

    public void permissionHasBeenAsked(String permission) {
        getConfig().setBool("app", permission, false);
    }

    public boolean isDeviceRingtoneEnabled() {
        int readExternalStorage = mContext.getPackageManager().checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, mContext.getPackageName());
        return getConfig().getBool("app", "device_ringtone", true) && readExternalStorage == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isAutoAnswerEnabled() {
        return getConfig().getBool("app", "auto_answer", false);
    }


    public int getAutoAnswerTime() {
        return getConfig().getInt("app", "auto_answer_delay", 0);
    }

}
