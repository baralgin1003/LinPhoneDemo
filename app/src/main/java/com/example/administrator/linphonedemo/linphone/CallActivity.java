package com.example.administrator.linphonedemo.linphone;

/*
CallActivity.java
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
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.linphonedemo.R;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphonePlayer;
import org.linphone.mediastream.Log;

import java.util.Arrays;
import java.util.List;

public class CallActivity extends LinphoneGenericActivity implements OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int PERMISSIONS_ENABLED_MIC = 204;

    private static CallActivity instance;

    private Handler mControlsHandler = new Handler();
    private Runnable mControls;
    private ImageView hangUp, micro, speaker, options;

    private CallVideoFragment videoCallFragment;
    private boolean isSpeakerEnabled = false, isMicMuted = false;
    private LinearLayout mControlsLayout;

    //private int cameraNumber;
    private CountDownTimer timer;
    private boolean isVideoCallPaused = false;
    private Dialog dialog = null;
    private static long TimeRemind = 0;
    private HeadsetReceiver headsetReceiver;

    //private LinearLayout callsList, conferenceList;
    private LinphoneCoreListenerBase mListener;


    private boolean oldIsSpeakerEnabled = false;

    public static CallActivity instance() {
        return instance;
    }

    public static boolean isInstanciated() {
        return instance != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

       // if (getResources().getBoolean(R.bool.orientation_portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
     //   }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.sip_call);

        //Earset Connectivity Broadcast Processing
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        headsetReceiver = new HeadsetReceiver();
        registerReceiver(headsetReceiver, intentFilter);

        LinphoneManager.getInstance().routeAudioToSpeaker();
        isSpeakerEnabled = true;

        mListener = new LinphoneCoreListenerBase() {
            @Override
            public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
            }

            @Override
            public void callState(LinphoneCore lc, final LinphoneCall call, State state, String message) {
                if (LinphoneManager.getLc().getCallsNb() == 0) {
                    finish();
                    return;
                }

                if (state == State.IncomingReceived) {
                    startIncomingCallActivity();
                    return;
                } else if (state == State.Paused || state == State.PausedByRemote || state == State.Pausing) {

                    if (isVideoEnabled(call)) {
                        showAudioView();
                    }
                } else if (state == State.Resuming) {
                    if (LinphonePreferences.instance().isVideoEnabled()) {
                        if (call.getCurrentParams().getVideoEnabled()) {
                            showVideoView();
                        }
                    }
                } else if (state == State.StreamsRunning) {
                    switchVideo(isVideoEnabled(call));
                    enableAndRefreshInCallActions();

                } else if (state == State.CallUpdatedByRemote) {
                    // If the correspondent proposes sip_video while audio sip_call
                    boolean videoEnabled = LinphonePreferences.instance().isVideoEnabled();
                    if (!videoEnabled) {
                        acceptCallUpdate(false);
                    }

                    boolean remoteVideo = call.getRemoteParams().getVideoEnabled();
                    boolean localVideo = call.getCurrentParams().getVideoEnabled();
                    boolean autoAcceptCameraPolicy = LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests();
                    if (remoteVideo && !localVideo && !autoAcceptCameraPolicy && !LinphoneManager.getLc().isInConference()) {
                        /*showAcceptCallUpdateDialog();
                        createTimerForDialog(SECONDS_BEFORE_DENYING_CALL_UPDATE);*/
                    }
//        			else if (remoteVideo && !LinphoneManager.getLc().isInConference() && autoAcceptCameraPolicy) {
//        				mHandler.post(new Runnable() {
//        					@Override
//        					public void run() {
//        						acceptCallUpdate(true);
//        					}
//        				});
//        			}
                }

                refreshIncallUi();
                //transfer.setEnabled(LinphoneManager.getLc().getCurrentCall() != null);
            }

            @Override
            public void callEncryptionChanged(LinphoneCore lc, final LinphoneCall call, boolean encrypted, String authenticationToken) {

            }

        };

        if (findViewById(R.id.fragmentContainer) != null) {
            initUI();

            if (LinphoneManager.getLc().getCallsNb() > 0) {
                LinphoneCall call = LinphoneManager.getLc().getCalls()[0];

                if (LinphoneUtils.isCallEstablished(call)) {
                    enableAndRefreshInCallActions();
                }
            }
            if (savedInstanceState != null) {
                // Fragment already created, no need to create it again (else it will generate a memory leak with duplicated fragments)
                isSpeakerEnabled = savedInstanceState.getBoolean("Speaker");
                isMicMuted = savedInstanceState.getBoolean("Mic");
                isVideoCallPaused = savedInstanceState.getBoolean("VideoCallPaused");
                if (savedInstanceState.getBoolean("AskingVideo")) {
                    //showAcceptCallUpdateDialog();
                    TimeRemind = savedInstanceState.getLong("TimeRemind");
                    createTimerForDialog(TimeRemind);
                }

                refreshInCallActions();
                return;
            } else {
                isSpeakerEnabled = LinphoneManager.getLc().isSpeakerEnabled();
                isMicMuted = LinphoneManager.getLc().isMicMuted();
            }

            Fragment callFragment;
            LinphoneManager.getLc().getCurrentCall().getCurrentParams().setVideoEnabled(true);
            disableVideo(false);

            callFragment = new CallVideoFragment();
            videoCallFragment = (CallVideoFragment) callFragment;
            //displayVideoCall(false);
            LinphoneManager.getInstance().routeAudioToSpeaker();
            isSpeakerEnabled = true;
            callFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.fragmentContainer, callFragment).commitAllowingStateLoss();

        }
    }

    public void createTimerForDialog(long time) {
        timer = new CountDownTimer(time, 1000) {
            public void onTick(long millisUntilFinished) {
                TimeRemind = millisUntilFinished;
            }

            public void onFinish() {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                acceptCallUpdate(false);
            }
        }.start();
    }

    private boolean isVideoEnabled(LinphoneCall call) {
        if (call != null) {
            return call.getCurrentParams().getVideoEnabled();
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("Speaker", LinphoneManager.getLc().isSpeakerEnabled());
        outState.putBoolean("Mic", LinphoneManager.getLc().isMicMuted());
        outState.putBoolean("VideoCallPaused", isVideoCallPaused);
        // outState.putBoolean("AskingVideo", isVideoAsk);
        outState.putLong("TimeRemind", TimeRemind);
        if (dialog != null) dialog.dismiss();
        super.onSaveInstanceState(outState);
    }

    private void initUI() {

        micro = (ImageView) findViewById(R.id.micro);
        micro.setOnClickListener(this);

        speaker = (ImageView) findViewById(R.id.speaker);
        speaker.setOnClickListener(this);

        options = (ImageView) findViewById(R.id.openDoor);
        options.setOnClickListener(this);

        // options.setEnabled(true);

        //BottonBar
        hangUp = (ImageView) findViewById(R.id.hang_up);
        hangUp.setOnClickListener(this);


        mControlsLayout = (LinearLayout) findViewById(R.id.menu);


        try {
            speaker.setVisibility(View.VISIBLE);
        } catch (NullPointerException npe) {
            Log.e("Bluetooth: Audio routes menu disabled on tablets for now (3)");
        }


        //createInCallStats();
        LinphoneManager.getInstance().changeStatusToOnThePhone();
    }

    public void checkAndRequestPermission(String permission, int result) {
        int permissionGranted = getPackageManager().checkPermission(permission, getPackageName());
        Log.i("[Permission] " + permission + " is " + (permissionGranted == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
            if (LinphonePreferences.instance().firstTimeAskingForPermission(permission) || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.i("[Permission] Asking for " + permission);
                ActivityCompat.requestPermissions(this, new String[]{permission}, result);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, final int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i("[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
        }

        switch (requestCode) {

            case PERMISSIONS_ENABLED_MIC:
                UIThreadDispatcher.dispatch(new Runnable() {
                    @Override
                    public void run() {
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            toggleMicro();
                        }
                    }
                });
                break;
        }
    }


    private void refreshIncallUi() {
        refreshInCallActions();
        refreshCallList(getResources());
        enableAndRefreshInCallActions();
    }


    protected void refreshInCallActions() {
        if (isSpeakerEnabled) {
            speaker.setImageResource(R.drawable.speaker_selected);
        } else {
            speaker.setImageResource(R.drawable.speaker_default);
        }

        if (getPackageManager().checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            isMicMuted = true;
        }
        if (isMicMuted) {
            micro.setImageResource(R.drawable.sip_micro_selected);
        } else {
            micro.setImageResource(R.drawable.sip_micro_default);
        }
    }

    private void enableAndRefreshInCallActions() {
        micro.setEnabled(true);
            speaker.setEnabled(true);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.micro) {
            int recordAudio = getPackageManager().checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName());
            Log.i("[Permission] Record audio permission is " + (recordAudio == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

            if (recordAudio == PackageManager.PERMISSION_GRANTED) {
                toggleMicro();
            } else {
                checkAndRequestPermission(Manifest.permission.RECORD_AUDIO, PERMISSIONS_ENABLED_MIC);
            }
        } else if (id == R.id.speaker) {
            toggleSpeaker();
        } else if (id == R.id.hang_up) {
            hangUp();
        } else if (id == R.id.openDoor) {
            openDoor();
        }
    }


    private void disableVideo(final boolean videoDisabled) {
        final LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
        if (call == null) {
            return;
        }

        if (videoDisabled) {
            LinphoneCallParams params = LinphoneManager.getLc().createCallParams(call);
            params.setVideoEnabled(false);
            LinphoneManager.getLc().updateCall(call, params);
        } else {
            //  videoProgress.setVisibility(View.VISIBLE);
            if (call.getRemoteParams() != null && !call.getRemoteParams().isLowBandwidthEnabled()) {
                LinphoneManager.getInstance().addVideo();
            }
        }
    }

    public void displayCustomToast(final String message, final int duration) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.sip_toast, (ViewGroup) findViewById(R.id.toastRoot));

        TextView toastText = (TextView) layout.findViewById(R.id.toastMessage);
        toastText.setText(message);

        final Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(duration);
        toast.setView(layout);
        toast.show();
    }

    private void switchVideo(final boolean displayVideo) {
        final LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
        if (call == null) {
            return;
        }

        //Check if the sip_call is not terminated
        if (call.getState() == State.CallEnd || call.getState() == State.CallReleased) return;

        if (!displayVideo) {
            showAudioView();
        } else {
            if (!call.getRemoteParams().isLowBandwidthEnabled()) {
                LinphoneManager.getInstance().addVideo();
                if (videoCallFragment == null || !videoCallFragment.isVisible())
                    showVideoView();
            } else {
                displayCustomToast("Плохая связь", Toast.LENGTH_LONG);
            }
        }
    }

    private void showAudioView() {


        displayAudioCall();
        removeCallbacks();
    }

    private void showVideoView() {

        Log.w("Bluetooth not available, using speaker");
        LinphoneManager.getInstance().routeAudioToSpeaker();
        isSpeakerEnabled = true;

        refreshInCallActions();


        replaceFragmentAudioByVideo();
    }


    private void displayAudioCall() {
        mControlsLayout.setVisibility(View.VISIBLE);
    }


    private void replaceFragmentAudioByVideo() {
//		Hiding controls to let displayVideoCallControlsIfHidden add them plus the callback
        videoCallFragment = new CallVideoFragment();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, videoCallFragment);
        try {
            transaction.commitAllowingStateLoss();
        } catch (Exception e) {
        }
    }

    private void toggleMicro() {
        LinphoneCore lc = LinphoneManager.getLc();
        isMicMuted = !isMicMuted;
        lc.muteMic(isMicMuted);
        if (isMicMuted) {
            micro.setImageResource(R.drawable.sip_micro_selected);
        } else {
            micro.setImageResource(R.drawable.sip_micro_default);
        }
    }

    protected void toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled;

        if (isSpeakerEnabled) {
            LinphoneManager.getInstance().routeAudioToSpeaker();
            speaker.setImageResource(R.drawable.speaker_selected);
            LinphoneManager.getLc().enableSpeaker(isSpeakerEnabled);
        } else {
            Log.d("Toggle speaker off, routing back to earpiece");
            LinphoneManager.getInstance().routeAudioToReceiver();
            speaker.setImageResource(R.drawable.speaker_default);
        }
    }


    private void hangUp() {
        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();

        if (currentCall != null) {
            lc.terminateCall(currentCall);
        } else if (lc.isInConference()) {
            lc.terminateConference();
        } else {
            lc.terminateAllCalls();
        }
    }

    public void displayVideoCall(boolean display) {
       /* if (display) {
            mControlsLayout.setVisibility(View.VISIBLE);
        } else {
            mControlsLayout.setVisibility(View.GONE);
        }*/
    }


    public void displayVideoCallControlsIfHidden() {
       /* if (mControlsLayout != null) {
            if (mControlsLayout.getVisibility() != View.VISIBLE) {
                displayVideoCall(true);
            }
            resetControlsHidingCallBack();
        }*/
    }


    public void removeCallbacks() {
        if (mControlsHandler != null && mControls != null) {
            mControlsHandler.removeCallbacks(mControls);
        }
        mControls = null;
    }


    private synchronized void openDoor() {
        //LinphoneManager.getLc().playDtmf("0".charAt(0), -1);
        LinphoneManager.getLc().sendDtmf("0".charAt(0));
        options.setImageResource(R.drawable.sip_options_selected);

        new CountDownTimer(1000, 500) {
            @Override
            public void onTick(long l) {
                LinphoneManager.getLc().sendDtmf("#".charAt(0));
            }

            @Override
            public void onFinish() {
                LinphoneManager.getLc().stopDtmf();
                options.setImageResource(R.drawable.sip_options_default);
            }
        }.start();
    }


    public void acceptCallUpdate(boolean accept) {
        if (timer != null) {
            timer.cancel();
        }

        LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
        if (call == null) {
            return;
        }

        LinphoneCallParams params = LinphoneManager.getLc().createCallParams(call);
        if (accept) {
            params.setVideoEnabled(true);
            LinphoneManager.getLc().enableVideo(true, true);
        }

        try {
            LinphoneManager.getLc().acceptCallUpdate(call, params);
        } catch (LinphoneCoreException e) {
            Log.e(e);
        }
    }

    public void startIncomingCallActivity() {
        startActivity(new Intent(this, CallIncomingActivity.class));
    }


    @Override
    protected void onResume() {

        instance = this;
        super.onResume();

        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }
        isSpeakerEnabled = LinphoneManager.getLc().isSpeakerEnabled();

        refreshIncallUi();
        handleViewIntent();


        if (!isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
            if (!isSpeakerEnabled) {

                removeCallbacks();
            }
        }
    }

    private void handleViewIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getAction() == "android.intent.action.VIEW") {
            LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
            if (call != null && isVideoEnabled(call)) {
                LinphonePlayer player = call.getPlayer();
                String path = intent.getData().getPath();
                Log.i("Openning " + path);
                int openRes = player.open(path);
                if (openRes == -1) {
                    String message = "Could not open " + path;
                    Log.e(message);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i("Start playing");
                if (player.start() == -1) {
                    player.close();
                    String message = "Could not start playing " + path;
                    Log.e(message);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.removeListener(mListener);
        }

        super.onPause();

        if (mControlsHandler != null && mControls != null) {
            mControlsHandler.removeCallbacks(mControls);
        }
        mControls = null;
    }

    @Override
    protected void onDestroy() {
        LinphoneManager.getInstance().changeStatusToOnline();

        unregisterReceiver(headsetReceiver);

        if (mControlsHandler != null && mControls != null) {
            mControlsHandler.removeCallbacks(mControls);
        }
        mControls = null;
        mControlsHandler = null;

        unbindDrawables(findViewById(R.id.topLayout));

        instance = null;
        super.onDestroy();
        System.gc();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ImageView) {
            view.setOnClickListener(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (LinphoneUtils.onKeyVolumeAdjust(keyCode)) return true;
        if (LinphoneUtils.onKeyBackGoHome(this, keyCode, event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override // Never invoke actually
    public void onBackPressed() {
        if (dialog != null) {
            acceptCallUpdate(false);
            dialog.dismiss();
            dialog = null;
        }
        return;
    }


    public void bindVideoFragment(CallVideoFragment fragment) {
        videoCallFragment = fragment;
    }


    public void refreshCallList(Resources resources) {

        List<LinphoneCall> pausedCalls = LinphoneUtils.getCallsInState(LinphoneManager.getLc(), Arrays.asList(State.PausedByRemote));


        //Active sip_call
        if (LinphoneManager.getLc().getCurrentCall() != null) {

            if (isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
                displayVideoCall(false);
            } else {
                displayAudioCall();
            }
        } else {
            showAudioView();
        }

    }


    ////Earset Connectivity Broadcast innerClass
    public class HeadsetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra("state")) {
                switch (intent.getIntExtra("state", 0)) {
                    case 0:
                        if (oldIsSpeakerEnabled) {
                            LinphoneManager.getInstance().routeAudioToSpeaker();
                            isSpeakerEnabled = true;
                            speaker.setEnabled(true);
                        }
                        break;
                    case 1:
                        LinphoneManager.getInstance().routeAudioToReceiver();
                        oldIsSpeakerEnabled = isSpeakerEnabled;
                        isSpeakerEnabled = false;
                        speaker.setEnabled(false);
                        break;
                }
                refreshInCallActions();
            }

        }
    }
}
