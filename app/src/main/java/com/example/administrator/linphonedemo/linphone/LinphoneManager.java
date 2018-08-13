/*
LinphoneManager.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

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
package com.example.administrator.linphonedemo.linphone;

/*
LinphoneManager.java
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.example.administrator.linphonedemo.R;

import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAccountCreator;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.AuthMethod;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.LogCollectionUploadState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCore.RemoteProvisioningState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.OpenH264DownloadHelperListener;
import org.linphone.core.PresenceActivityType;
import org.linphone.core.PresenceBasicStatus;
import org.linphone.core.PresenceModel;
import org.linphone.core.PublishState;
import org.linphone.core.Reason;
import org.linphone.core.SubscriptionState;
import org.linphone.core.TunnelConfig;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;
import org.linphone.mediastream.video.capture.hwconf.Hacks;
import org.linphone.tools.H264Helper;
import org.linphone.tools.OpenH264DownloadHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.AudioManager.MODE_RINGTONE;
import static android.media.AudioManager.STREAM_RING;
import static android.media.AudioManager.STREAM_VOICE_CALL;

/**
 * Manager of the low level LibLinphone stuff.<br />
 * Including:<ul>
 * <li>Starting C liblinphone</li>
 * <li>Reacting to C liblinphone state changes</li>
 * <li>Calling Linphone android service listener methods</li>
 * <li>Interacting from Android GUI/service with low level SIP stuff/</li>
 * </ul>
 * <p>
 * Add Service Listener to react to Linphone state changes.
 */
public class LinphoneManager implements LinphoneCoreListener, LinphoneAccountCreator.LinphoneAccountCreatorListener {

    private static LinphoneManager instance;
    private Context mServiceContext;
    private AudioManager mAudioManager;
    private PowerManager mPowerManager;
    private Resources mR;
    private LinphonePreferences mPrefs;
    private LinphoneCore mLc;
    private OpenH264DownloadHelper mCodecDownloader;
    private OpenH264DownloadHelperListener mCodecListener;
    private String lastLcStatusMessage;
    private String basePath;
    private static boolean sExited;
    private boolean mAudioFocused;
    private boolean callGsmON;
    private int mLastNetworkType = -1;
    private ConnectivityManager mConnectivityManager;

    private Handler mHandler = new Handler();

    private LinphoneAccountCreator accountCreator;

    private boolean handsetON = false;

    private String mOpenCodec;

    public String wizardLoginViewDomain = null;


    protected LinphoneManager(final Context c) {
        sExited = false;
        mServiceContext = c;
        basePath = c.getFilesDir().getAbsolutePath();
        mOpenCodec = basePath + "/libopenh264.so";
        mLPConfigXsd = basePath + "/lpconfig.xsd";
        mLinphoneFactoryConfigFile = basePath + "/linphonerc";
        mLinphoneConfigFile = basePath + "/.linphonerc";
        mLinphoneRootCaFile = basePath + "/rootca.pem";
        mDynamicConfigFile = basePath + "/assistant_create.rc";
        mRingSoundFile = basePath + "/ringtone.mkv";
        mRingbackSoundFile = basePath + "/ringback.wav";
        mPauseSoundFile = basePath + "/hold.mkv";
        mChatDatabaseFile = basePath + "/linphone-history.db";
        mCallLogDatabaseFile = basePath + "/linphone-log-history.db";
        mFriendsDatabaseFile = basePath + "/linphone-friends.db";
        mErrorToneFile = basePath + "/error.wav";
        mUserCertificatePath = basePath;

        mPrefs = LinphonePreferences.instance();
        mAudioManager = ((AudioManager) c.getSystemService(Context.AUDIO_SERVICE));
        mVibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        mPowerManager = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        mConnectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        /*mSensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);*/
        mR = c.getResources();
        //  mPendingChatFileMessage = new ArrayList<LinphoneChatMessage>();
    }

    private static final int LINPHONE_VOLUME_STREAM = STREAM_VOICE_CALL;
    private static final int dbStep = 4;
    /**
     * Called when the activity is first created.
     */
    private final String mLPConfigXsd;
    private final String mLinphoneFactoryConfigFile;
    private final String mLinphoneRootCaFile;
    private final String mDynamicConfigFile;
    public final String mLinphoneConfigFile;
    private final String mRingSoundFile;
    private final String mRingbackSoundFile;
    private final String mPauseSoundFile;
    private final String mChatDatabaseFile;
    private final String mCallLogDatabaseFile;
    private final String mFriendsDatabaseFile;
    private final String mErrorToneFile;
    private final String mUserCertificatePath;
    private Timer mTimer;

    private void routeAudioToSpeakerHelper(boolean speakerOn) {
        Log.w("Routing audio to " + (speakerOn ? "speaker" : "earpiece") + ", disabling bluetooth audio route");


        mLc.enableSpeaker(speakerOn);
    }


    public void routeAudioToSpeaker() {
        routeAudioToSpeakerHelper(true);
    }

    public void routeAudioToReceiver() {
        routeAudioToSpeakerHelper(false);
    }

    public synchronized static final LinphoneManager createAndStart(Context c) {
        if (instance != null)
            throw new RuntimeException("Linphone Manager is already initialized");

        instance = new LinphoneManager(c);
        instance.startLibLinphone(c);
        //  instance.initOpenH264DownloadHelper();

        // H264 codec Management - set to auto mode -> MediaCodec >= android 5.0 >= OpenH264
        H264Helper.setH264Mode(H264Helper.MODE_OPENH264, getLc());


        return instance;
    }

    private boolean isPresenceModelActivitySet() {
        LinphoneCore lc = getLcIfManagerNotDestroyedOrNull();
        if (isInstanciated() && lc != null) {
            return lc.getPresenceModel() != null && lc.getPresenceModel().getActivity() != null;
        }
        return false;
    }

    public void changeStatusToOnline() {
        LinphoneCore lc = getLcIfManagerNotDestroyedOrNull();
        if (isInstanciated() && lc != null && isPresenceModelActivitySet() && lc.getPresenceModel().getActivity().getType() != PresenceActivityType.TV) {
            lc.getPresenceModel().getActivity().setType(PresenceActivityType.TV);
        } else if (isInstanciated() && lc != null && !isPresenceModelActivitySet()) {
            PresenceModel model = LinphoneCoreFactory.instance().createPresenceModel(PresenceActivityType.TV, null);
            lc.setPresenceModel(model);
        }
    }

    public void changeStatusToOnThePhone() {
        LinphoneCore lc = getLcIfManagerNotDestroyedOrNull();
        if (isInstanciated() && isPresenceModelActivitySet() && lc.getPresenceModel().getActivity().getType() != PresenceActivityType.OnThePhone) {
            lc.getPresenceModel().getActivity().setType(PresenceActivityType.OnThePhone);
        } else if (isInstanciated() && !isPresenceModelActivitySet()) {
            PresenceModel model = LinphoneCoreFactory.instance().createPresenceModel(PresenceActivityType.OnThePhone, null);
            lc.setPresenceModel(model);
        }
    }

    public void changeStatusToOffline() {
        LinphoneCore lc = getLcIfManagerNotDestroyedOrNull();
        if (isInstanciated() && lc != null) {
            lc.getPresenceModel().setBasicStatus(PresenceBasicStatus.Closed);
        }
    }

    public void subscribeFriendList(boolean enabled) {
        LinphoneCore lc = getLcIfManagerNotDestroyedOrNull();
        if (lc != null && lc.getFriendList() != null && lc.getFriendList().length > 0) {
            LinphoneFriendList mFriendList = (lc.getFriendLists())[0];
            Log.i("Presence list subscription is " + (enabled ? "enabled" : "disabled"));
            mFriendList.enableSubscriptions(enabled);
        }
    }


    public static synchronized final LinphoneManager getInstance() {
        if (instance != null) return instance;

        if (sExited) {
            throw new RuntimeException("Linphone Manager was already destroyed. "
                    + "Better use getLcIfManagerNotDestroyedOrNull and check returned value");
        }

        throw new RuntimeException("Linphone Manager should be created before accessed");
    }

    public static synchronized final LinphoneCore getLc() {
        return getInstance().mLc;
    }


    private void resetCameraFromPreferences() {
        boolean useFrontCam = mPrefs.useFrontCam();

        int camId = 0;
        AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
        for (AndroidCamera androidCamera : cameras) {
            if (androidCamera.frontFacing == useFrontCam)
                camId = androidCamera.id;
        }
        LinphoneManager.getLc().setVideoDevice(camId);
    }

    public static interface AddressType {
        void setText(CharSequence s);

        CharSequence getText();

        void setDisplayedName(String s);

        String getDisplayedName();
    }


    public void terminateCall(LinphoneCall call) {
     /*   if (mLc.isIncall()) {
            mLc.terminateCall(mLc.getCurrentCall());
        }*/
      /* if(sip_call!=null){
           mLc.terminateCall(sip_call);
       }*/
        destroyLinphoneCore();
        LinphoneService.instance().stopSelf();
    }

    public void initTunnelFromConf() {
        if (!mLc.isTunnelAvailable())
            return;

        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        mLc.tunnelCleanServers();
        TunnelConfig config = mPrefs.getTunnelConfig();
        if (config.getHost() != null) {
            mLc.tunnelAddServer(config);
            manageTunnelServer(info);
        }
    }

    private boolean isTunnelNeeded(NetworkInfo info) {
        if (info == null) {
            Log.i("No connectivity: tunnel should be disabled");
            return false;
        }

        String pref = mPrefs.getTunnelMode();

        if ("always".equals(pref)) {
            return true;
        }

        if (info.getType() != ConnectivityManager.TYPE_WIFI
                && "3G_only".equals(pref)) {
            Log.i("need tunnel: 'no wifi' connection");
            return true;
        }

        return false;
    }

    private void manageTunnelServer(NetworkInfo info) {
        if (mLc == null) return;
        if (!mLc.isTunnelAvailable()) return;

        Log.i("Managing tunnel");
        if (isTunnelNeeded(info)) {
            Log.i("Tunnel need to be activated");
            mLc.tunnelSetMode(LinphoneCore.TunnelMode.enable);
        } else {
            Log.i("Tunnel should not be used");
            String pref = mPrefs.getTunnelMode();
            mLc.tunnelSetMode(LinphoneCore.TunnelMode.disable);
            if ("auto".equals(pref)) {
                mLc.tunnelSetMode(LinphoneCore.TunnelMode.auto);
            }
        }
    }

    public synchronized final void destroyLinphoneCore() {
        sExited = true;

        try {
            mTimer.cancel();
            mLc.destroy();
        } catch (RuntimeException e) {
            Log.e(e);
        } finally {
            mLc = null;
        }
    }

    public void restartLinphoneCore() {
        destroyLinphoneCore();
        startLibLinphone(mServiceContext);
        sExited = false;
    }

    private synchronized void startLibLinphone(Context c) {
        try {
            copyAssetsFromPackage();
            //traces alway start with traces enable to not missed first initialization
            mLc = LinphoneCoreFactory.instance().createLinphoneCore(this, mLinphoneConfigFile, mLinphoneFactoryConfigFile, null, c);
            TimerTask lTask = new TimerTask() {
                @Override
                public void run() {
                    UIThreadDispatcher.dispatch(new Runnable() {
                        @Override
                        public void run() {
                            if (mLc != null) {
                                mLc.iterate();
                            }
                        }
                    });
                }
            };
            /*use schedule instead of scheduleAtFixedRate to avoid iterate from being sip_call in burst after cpu wake up*/
            mTimer = new Timer("Linphone scheduler");
            mTimer.schedule(lTask, 0, 20);
        } catch (Exception e) {
            Log.e(e, "Cannot start linphone");
        }
    }

    private synchronized void initLiblinphone(LinphoneCore lc) throws LinphoneCoreException {
        mLc = lc;

        mLc.setZrtpSecretsCache(basePath + "/zrtp_secrets");

        try {
            String versionName = mServiceContext.getPackageManager().getPackageInfo(mServiceContext.getPackageName(), 0).versionName;
            if (versionName == null) {
                versionName = String.valueOf(mServiceContext.getPackageManager().getPackageInfo(mServiceContext.getPackageName(), 0).versionCode);
            }
            mLc.setUserAgent("LinphoneAndroid", versionName);
        } catch (NameNotFoundException e) {
            Log.e(e, "cannot get version name");
        }

        mLc.setRingback(mRingbackSoundFile);
        mLc.setRootCA(mLinphoneRootCaFile);
      //  mLc.setPlayFile(mPauseSoundFile);
        mLc.setCallLogsDatabasePath(mCallLogDatabaseFile);
        mLc.setFriendsDatabasePath(mFriendsDatabaseFile);
        mLc.setUserCertificatesPath(mUserCertificatePath);
        //mLc.setCallErrorTone(Reason.NotFound, mErrorToneFile);
        enableDeviceRingtone(mPrefs.isDeviceRingtoneEnabled());

        int availableCores = Runtime.getRuntime().availableProcessors();
        Log.w("MediaStreamer : " + availableCores + " cores detected and configured");
        mLc.setCpuCount(availableCores);

        mLc.migrateCallLogs();



		/*
		 You cannot receive this through components declared in manifests, only
		 by explicitly registering for it with Context.registerReceiver(). This is a protected intent that can only
		 be sent by the system.
		*/

        updateNetworkReachability();

        resetCameraFromPreferences();

        accountCreator = LinphoneCoreFactory.instance().createAccountCreator(LinphoneManager.getLc(), LinphonePreferences.instance().getConfig().getString("assistant", "xmlrpc_url", null));
        accountCreator.setListener(this);
        callGsmON = false;
    }


    private void copyAssetsFromPackage() throws IOException {
        copyIfNotExist(R.raw.libopenh264, mOpenCodec);
        copyIfNotExist(R.raw.notes_of_the_optimistic, mRingSoundFile);
        copyIfNotExist(R.raw.ringback, mRingbackSoundFile);
     //   copyIfNotExist(R.raw.hold, mPauseSoundFile);
        //copyIfNotExist(R.raw.incoming_chat, mErrorToneFile);
        copyIfNotExist(R.raw.linphonerc_default, mLinphoneConfigFile);
        copyFromPackage(R.raw.linphonerc_factory, new File(mLinphoneFactoryConfigFile).getName());
        copyIfNotExist(R.raw.lpconfig, mLPConfigXsd);
        copyFromPackage(R.raw.rootca, new File(mLinphoneRootCaFile).getName());
        copyFromPackage(R.raw.assistant_create, new File(mDynamicConfigFile).getName());
    }

    public void copyIfNotExist(int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists() | lFileToCopy.length() == 0) {
            copyFromPackage(ressourceId, lFileToCopy.getName());
        }
    }

    public void copyFromPackage(int ressourceId, String target) throws IOException {
        FileOutputStream lOutputStream = mServiceContext.openFileOutput(target, 0);
        InputStream lInputStream = mR.openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }


    public void updateNetworkReachability() {
        if (mConnectivityManager == null) return;

        boolean connected = false;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        connected = networkInfo != null && networkInfo.isConnected();

        if (networkInfo == null && Version.sdkAboveOrEqual(Version.API21_LOLLIPOP_50)) {
            for (Network network : mConnectivityManager.getAllNetworks()) {
                if (network != null) {
                    networkInfo = mConnectivityManager.getNetworkInfo(network);
                    if (networkInfo != null && networkInfo.isConnected()) {
                        connected = true;
                        break;
                    }
                }
            }
        }

        if (networkInfo == null || !connected) {
            Log.i("No connectivity: setting network unreachable");
            mLc.setNetworkReachable(false);
        } else if (connected) {
            manageTunnelServer(networkInfo);

            boolean wifiOnly = LinphonePreferences.instance().isWifiOnlyEnabled();
            if (wifiOnly) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    mLc.setNetworkReachable(true);
                } else {
                    Log.i("Wifi-only mode, setting network not reachable");
                    mLc.setNetworkReachable(false);
                }
            } else {
                int curtype = networkInfo.getType();

                if (curtype != mLastNetworkType) {
                    //if kind of network has changed, we need to notify network_reachable(false) to make sure all current connections are destroyed.
                    //they will be re-created during setNetworkReachable(true).
                    Log.i("Connectivity has changed.");
                    mLc.setNetworkReachable(false);
                }
                mLc.setNetworkReachable(true);
                mLastNetworkType = curtype;
            }
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doDestroy() {

        try {
            mTimer.cancel();
            mLc.destroy();
        } catch (RuntimeException e) {
            Log.e(e);
        } finally {
           /* try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    mServiceContext.unregisterReceiver(mNetworkReceiver);
                }
            } catch (Exception e) {
                Log.e(e);
            }
            try {
                mServiceContext.unregisterReceiver(mHookReceiver);
            } catch (Exception e) {
                Log.e(e);
            }
            try {
                mServiceContext.unregisterReceiver(mKeepAliveReceiver);
            } catch (Exception e) {
                Log.e(e);
            }
            try {
                mServiceContext.unregisterReceiver(mCallReceiver);
            } catch (Exception e) {
                Log.e(e);
            }*/

            mLc = null;
            instance = null;
        }
    }


    public static synchronized void destroy() {
        if (instance == null) return;
        getInstance().changeStatusToOffline();
        sExited = true;
        instance.doDestroy();
    }

    private String getString(int key) {
        return mR.getString(key);
    }


    private LinphoneCall ringingCall;

    private MediaPlayer mRingerPlayer;
    private Vibrator mVibrator;

    public void displayWarning(LinphoneCore lc, String message) {
    }

    public void displayMessage(LinphoneCore lc, String message) {
    }

    public void show(LinphoneCore lc) {
    }

    public void newSubscriptionRequest(LinphoneCore lc, LinphoneFriend lf, String url) {
    }

    public void notifyPresenceReceived(LinphoneCore lc, LinphoneFriend lf) {

    }

    @Override
    public void dtmfReceived(LinphoneCore lc, LinphoneCall call, int dtmf) {
        Log.d("DTMF received: " + dtmf);
    }

    @Override
    public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {

    }

    @Override
    public void messageReceivedUnableToDecrypted(LinphoneCore lc, LinphoneChatRoom cr,
                                                 LinphoneChatMessage message) {

    }

    public void displayStatus(final LinphoneCore lc, final String message) {
        Log.i(message);
        lastLcStatusMessage = message;
    }

    public void globalState(final LinphoneCore lc, final GlobalState state, final String message) {
        Log.i("New global state [", state, "]");
        if (state == GlobalState.GlobalOn) {
            try {
                Log.e("LinphoneManager", " globalState ON");
                initLiblinphone(lc);

            } catch (IllegalArgumentException iae) {
                Log.e(iae);
            } catch (LinphoneCoreException e) {
                Log.e(e);
            }
        }
    }

    public void registrationState(final LinphoneCore lc, final LinphoneProxyConfig proxy, final RegistrationState state, final String message) {
        Log.i("New registration state [" + state + "]");
        if (LinphoneManager.getLc().getDefaultProxyConfig() == null) {
            subscribeFriendList(false);
        }
    }

    public Context getContext() {
        try {
            if (CallActivity.isInstanciated())
                return CallActivity.instance();
            else if (CallIncomingActivity.isInstanciated())
                return CallIncomingActivity.instance();
            else if (mServiceContext != null)
                return mServiceContext;
            else if (LinphoneService.isReady())
                return LinphoneService.instance().getApplicationContext();
        } catch (Exception e) {
            Log.e(e);
        }
        return null;
    }

    public void setAudioManagerInCallMode() {
        if (mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            Log.w("[AudioManager] already in MODE_IN_COMMUNICATION, skipping...");
            return;
        }
        Log.d("[AudioManager] Mode: MODE_IN_COMMUNICATION");

        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    @SuppressLint("Wakelock")
    public void callState(final LinphoneCore lc, final LinphoneCall call, final State state, final String message) {
        Log.i("New sip_call state [", state, "]");
        if (state == State.IncomingReceived && !call.equals(lc.getCurrentCall())) {
            if (call.getReplacedCall() != null) {
                // attended transfer
                // it will be accepted automatically.
                return;
            }
        }
        if (state == State.CallEnd) {

        }

        if (state == State.IncomingReceived && getCallGsmON()) {
            if (mLc != null) {
                mLc.declineCall(call, Reason.Busy);
            }
        } else if (state == State.IncomingReceived && (LinphonePreferences.instance().isAutoAnswerEnabled()) && !getCallGsmON()) {
            TimerTask lTask = new TimerTask() {
                @Override
                public void run() {
                    if (mLc != null) {
                        try {
                            if (mLc.getCallsNb() > 0) {
                                mLc.acceptCall(call);
                                if (LinphoneManager.getInstance() != null) {
                                    LinphoneManager.getInstance().routeAudioToReceiver();
                                    getContext().startActivity(new Intent(getContext(), CallIncomingActivity.class));
                                }
                            }
                        } catch (LinphoneCoreException e) {
                            Log.e(e);
                        }
                    }
                }
            };
            mTimer = new Timer("Auto answer");
            mTimer.schedule(lTask, mPrefs.getAutoAnswerTime());
        } else if (state == State.IncomingReceived || (state == State.CallIncomingEarlyMedia)) {
            // Brighten screen for at least 10 seconds
            if (mLc.getCallsNb() == 1) {
                requestAudioFocus(STREAM_RING);

                ringingCall = call;
                startRinging();
                // otherwise there is the beep
            }
        } else if (call == ringingCall && isRinging) {
            //previous state was ringing, so stop ringing
            stopRinging();
        }

        if (state == State.Connected) {
            if (mLc.getCallsNb() == 1) {
                //It is for incoming calls, because outgoing calls enter MODE_IN_COMMUNICATION immediately when they start.
                //However, incoming sip_call first use the MODE_RINGING to play the local ring.
                if (call.getDirection() == CallDirection.Incoming) {
                    setAudioManagerInCallMode();
                    requestAudioFocus(STREAM_VOICE_CALL);
                }
            }

            if (Hacks.needSoftvolume()) {
                Log.w("Using soft volume audio hack");
                adjustVolume(0); // Synchronize
            }
        }

        if (state == State.CallEnd || state == State.Error) {
            if (mLc.getCallsNb() == 0) {
                Context activity = getContext();
                if (mAudioFocused) {
                    int res = mAudioManager.abandonAudioFocus(null);
                    Log.d("Audio focus released a bit later: " + (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted" : "Denied"));
                    mAudioFocused = false;
                }
                if (activity != null) {
                    TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                        Log.d("---AudioManager: back to MODE_NORMAL");
                        mAudioManager.setMode(AudioManager.MODE_NORMAL);
                        Log.d("All sip_call terminated, routing back to earpiece");
                        routeAudioToReceiver();
                    }
                }
            }
        }
        if (state == State.CallUpdatedByRemote) {
            // If the correspondent proposes sip_video while audio sip_call
            boolean remoteVideo = call.getRemoteParams().getVideoEnabled();
            boolean localVideo = call.getCurrentParams().getVideoEnabled();
            boolean autoAcceptCameraPolicy = LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests();
            if (remoteVideo && !localVideo && !autoAcceptCameraPolicy && !LinphoneManager.getLc().isInConference()) {
                try {
                    LinphoneManager.getLc().deferCallUpdate(call);
                } catch (LinphoneCoreException e) {
                    Log.e(e);
                }
            }
        }
        if (state == State.OutgoingInit) {
            //Enter the MODE_IN_COMMUNICATION mode as soon as possible, so that ringback
            //is heard normally in earpiece or bluetooth receiver.
            setAudioManagerInCallMode();
            requestAudioFocus(STREAM_VOICE_CALL);
            startBluetooth();
        }

        if (state == State.StreamsRunning) {
            setAudioManagerInCallMode();
        }
    }

    public void startBluetooth() {

    }

    public void callStatsUpdated(final LinphoneCore lc, final LinphoneCall call, final LinphoneCallStats stats) {
    }

    public void callEncryptionChanged(LinphoneCore lc, LinphoneCall call,
                                      boolean encrypted, String authenticationToken) {
    }

    private boolean isRinging;

    private void requestAudioFocus(int stream) {
        if (!mAudioFocused) {
            int res = mAudioManager.requestAudioFocus(null, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
            Log.d("Audio focus requested: " + (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted" : "Denied"));
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) mAudioFocused = true;
        }
    }

    public void enableDeviceRingtone(boolean use) {
        if (use) {
            mLc.setRing(null);
        } else {
            mLc.setRing(mRingSoundFile);
        }
    }

    private synchronized void startRinging() {
        if (!LinphonePreferences.instance().isDeviceRingtoneEnabled()) {
            // Enable speaker audio route, linphone library will do the ringing itself automatically
            routeAudioToSpeaker();
            return;
        }
        routeAudioToSpeaker(); // Need to be able to ear the ringtone during the early media

        //if (Hacks.needGalaxySAudioHack())
        mAudioManager.setMode(MODE_RINGTONE);

        try {
            if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
                long[] patern = {0, 1000, 1000};
                mVibrator.vibrate(patern, 1);
            }
            if (mRingerPlayer == null) {
                requestAudioFocus(STREAM_RING);
                mRingerPlayer = new MediaPlayer();
                mRingerPlayer.setAudioStreamType(STREAM_RING);

                String ringtone = LinphonePreferences.instance().getRingtone(Settings.System.DEFAULT_RINGTONE_URI.toString());
                try {
                    if (ringtone.startsWith("content://")) {
                        mRingerPlayer.setDataSource(mServiceContext, Uri.parse(ringtone));
                    } else {
                        FileInputStream fis = new FileInputStream(ringtone);
                        mRingerPlayer.setDataSource(fis.getFD());
                        fis.close();
                    }
                } catch (IOException e) {
                    Log.e(e, "Cannot set ringtone");
                }

                mRingerPlayer.prepare();
                mRingerPlayer.setLooping(true);
                mRingerPlayer.start();
            } else {
                Log.w("already ringing");
            }
        } catch (Exception e) {
            Log.e(e, "cannot handle incoming sip_call");
        }
        isRinging = true;
    }

    private synchronized void stopRinging() {
        if (mRingerPlayer != null) {
            mRingerPlayer.stop();
            mRingerPlayer.release();
            mRingerPlayer = null;
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }

        if (Hacks.needGalaxySAudioHack())
            mAudioManager.setMode(AudioManager.MODE_NORMAL);

        isRinging = false;

        routeAudioToSpeaker();
    }


    public static String extractADisplayName(Resources r, LinphoneAddress address) {
        if (address == null) return "Неизвестно";

        final String displayName = address.getDisplayName();
        if (displayName != null) {
            return displayName;
        } else if (address.getUserName() != null) {
            return address.getUserName();
        } else {
            String rms = address.toString();
            if (rms != null && rms.length() > 1)
                return rms;

            return "Неизвестно";
        }
    }

    public static boolean reinviteWithVideo() {
        return CallManager.getInstance().reinviteWithVideo();
    }

    /**
     * @return false if already in sip_video sip_call.
     */
    public boolean addVideo() {
        return reinviteWithVideo();
    }

    public boolean acceptCallWithParams(LinphoneCall call, LinphoneCallParams params) {
        try {
            mLc.acceptCallWithParams(call, params);
            return true;
        } catch (LinphoneCoreException e) {
            Log.i(e, "Accept sip_call failed");
        }
        return false;
    }

    public void adjustVolume(int i) {
        if (Build.VERSION.SDK_INT < 15) {
            int oldVolume = mAudioManager.getStreamVolume(LINPHONE_VOLUME_STREAM);
            int maxVolume = mAudioManager.getStreamMaxVolume(LINPHONE_VOLUME_STREAM);

            int nextVolume = oldVolume + i;
            if (nextVolume > maxVolume) nextVolume = maxVolume;
            if (nextVolume < 0) nextVolume = 0;

            mLc.setPlaybackGain((nextVolume - maxVolume) * dbStep);
        } else
            // starting from ICS, volume must be adjusted by the application, at least for STREAM_VOICE_CALL volume stream
            mAudioManager.adjustStreamVolume(LINPHONE_VOLUME_STREAM, i < 0 ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public static synchronized LinphoneCore getLcIfManagerNotDestroyedOrNull() {
        if (sExited || instance == null) {
            // Can occur if the UI thread play a posted event but in the meantime the LinphoneManager was destroyed
            // Ex: stop sip_call and quickly terminate application.
            return null;
        }
        return getLc();
    }

    public static final boolean isInstanciated() {
        return instance != null;
    }

    public boolean getCallGsmON() {
        return callGsmON;
    }

    @SuppressWarnings("serial")

    @Override
    public void notifyReceived(LinphoneCore lc, LinphoneCall call,
                               LinphoneAddress from, byte[] event) {
    }

    @Override
    public void transferState(LinphoneCore lc, LinphoneCall call,
                              State new_call_state) {

    }

    @Override
    public void infoReceived(LinphoneCore lc, LinphoneCall call, LinphoneInfoMessage info) {
        Log.d("Info message received from " + call.getRemoteAddress().asString());
        LinphoneContent ct = info.getContent();
        if (ct != null) {
            Log.d("Info received with body with mime type " + ct.getType() + "/" + ct.getSubtype() + " and data [" + ct.getDataAsString() + "]");
        }
    }

    @Override
    public void subscriptionStateChanged(LinphoneCore lc, LinphoneEvent ev,
                                         SubscriptionState state) {
        Log.d("Subscription state changed to " + state + " event name is " + ev.getEventName());
    }

    @Override
    public void notifyReceived(LinphoneCore lc, LinphoneEvent ev,
                               String eventName, LinphoneContent content) {
        Log.d("Notify received for event " + eventName);
        if (content != null)
            Log.d("with content " + content.getType() + "/" + content.getSubtype() + " data:" + content.getDataAsString());
    }

    @Override
    public void publishStateChanged(LinphoneCore lc, LinphoneEvent ev,
                                    PublishState state) {
        Log.d("Publish state changed to " + state + " for event name " + ev.getEventName());
    }

    @Override
    public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom cr) {
        Log.d("Composing received for chatroom " + cr.getPeerAddress().asStringUriOnly());
    }

    @Override
    public void configuringStatus(LinphoneCore lc,
                                  RemoteProvisioningState state, String message) {
        Log.d("Remote provisioning status = " + state.toString() + " (" + message + ")");

        if (state == RemoteProvisioningState.ConfiguringSuccessful) {
            if ((LinphonePreferences.instance().getConfig() != null) && LinphonePreferences.instance().getConfig().getBool("app", "show_login_view", false)) {
                LinphoneProxyConfig proxyConfig = lc.createProxyConfig();
                try {
                    LinphoneAddress addr = LinphoneCoreFactory.instance().createLinphoneAddress(proxyConfig.getIdentity());
                    wizardLoginViewDomain = addr.getDomain();
                } catch (LinphoneCoreException e) {
                    wizardLoginViewDomain = null;
                }
            }
        }
    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore lc,
                                               LinphoneChatMessage message, LinphoneContent content, int progress) {

    }

    @Override
    public void fileTransferRecv(LinphoneCore lc, LinphoneChatMessage message,
                                 LinphoneContent content, byte[] buffer, int size) {

    }

    @Override
    public int fileTransferSend(LinphoneCore lc, LinphoneChatMessage message,
                                LinphoneContent content, ByteBuffer buffer, int size) {
        return 0;
    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int offset, int total) {
        if (total > 0)
            Log.d("Log upload progress: currently uploaded = " + offset + " , total = " + total + ", % = " + String.valueOf((offset * 100) / total));
    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LogCollectionUploadState state, String info) {
        Log.d("Log upload state: " + state.toString() + ", info = " + info);
    }

    @Override
    public void ecCalibrationStatus(LinphoneCore lc, EcCalibratorStatus status,
                                    int delay_ms, Object data) {
        ((AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_NORMAL);
        mAudioManager.abandonAudioFocus(null);
        Log.i("Set audio mode on 'Normal'");
    }

    @Override
    public void friendListCreated(LinphoneCore lc, LinphoneFriendList list) {
        // TODO Auto-generated method stub
    }

    @Override
    public void friendListRemoved(LinphoneCore lc, LinphoneFriendList list) {
        // TODO Auto-generated method stub
    }

    @Override
    public void networkReachableChanged(LinphoneCore lc, boolean enable) {

    }

    @Override
    public void authInfoRequested(LinphoneCore lc, String realm,
                                  String username, String domain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void authenticationRequested(LinphoneCore lc,
                                        LinphoneAuthInfo authInfo, AuthMethod method) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAccountCreatorIsAccountUsed(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
        if (status.equals(LinphoneAccountCreator.RequestStatus.AccountExist)) {
            accountCreator.isAccountLinked();
        }
    }

    @Override
    public void onAccountCreatorAccountCreated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
    }

    @Override
    public void onAccountCreatorAccountActivated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
    }

    @Override
    public void onAccountCreatorAccountLinkedWithPhoneNumber(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

    }

    @Override
    public void onAccountCreatorPhoneNumberLinkActivated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
    }

    @Override
    public void onAccountCreatorIsAccountActivated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
    }

    @Override
    public void onAccountCreatorPhoneAccountRecovered(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
    }

    @Override
    public void onAccountCreatorIsAccountLinked(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

    }

    @Override
    public void onAccountCreatorIsPhoneNumberUsed(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {
    }

    @Override
    public void onAccountCreatorPasswordUpdated(LinphoneAccountCreator accountCreator, LinphoneAccountCreator.RequestStatus status) {

    }
}
