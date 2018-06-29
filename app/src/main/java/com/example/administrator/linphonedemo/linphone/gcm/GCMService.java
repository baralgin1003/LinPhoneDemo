package com.example.administrator.linphonedemo.linphone.gcm;
/*
GCMService.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

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

import android.content.Context;
import android.content.Intent;

import com.example.administrator.linphonedemo.R;
import com.example.administrator.linphonedemo.linphone.LinphoneManager;
import com.example.administrator.linphonedemo.linphone.LinphonePreferences;
import com.example.administrator.linphonedemo.linphone.LinphoneService;
import com.example.administrator.linphonedemo.linphone.UIThreadDispatcher;
import com.google.android.gcm.GCMBaseIntentService;
import com.socks.library.KLog;

import org.linphone.core.LinphoneCoreFactory;
import org.linphone.mediastream.Log;

import static android.content.Intent.ACTION_MAIN;

/**
 * @author Sylvain Berfini
 */
// Warning ! Do not rename the service !
public class GCMService extends GCMBaseIntentService {
	private static final String TAG = "Linphone";

	public GCMService() {
		KLog.i(TAG, "========== GCMService.<init>");
	}
	
	private void initLogger(Context context) {
		LinphonePreferences.instance().setContext(context);
		boolean isDebugEnabled = LinphonePreferences.instance().isDebugEnabled();
		LinphoneCoreFactory.instance().enableLogCollection(isDebugEnabled);
		LinphoneCoreFactory.instance().setDebugMode(isDebugEnabled, context.getString(R.string.app_name));
	}
	
	@Override
	protected void onError(Context context, String errorId) {
		initLogger(context);
		Log.e("[Push Notification] Error while registering: " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		KLog.i(TAG, "========== GCMService.onMessage  " + "context = [" + context + "], intent = [" + intent + "]");
		initLogger(context);
		Log.d("[Push Notification] Received");
		
		if (!LinphoneService.isReady()) {
			startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
		} else if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() == 0) {
			UIThreadDispatcher.dispatch(new Runnable(){
				@Override
				public void run() {
					if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() == 0){
						LinphoneManager.getLc().setNetworkReachable(false);
						LinphoneManager.getLc().setNetworkReachable(true);
					}
				}
			});
		}
	}

	@Override
	protected void onRegistered(Context context, final String regId) {
		KLog.i(TAG, "========== GCMService.onRegistered  " + "context = [" + context + "], regId = [" + regId + "]");
		initLogger(context);
		Log.d("[Push Notification] Registered: " + regId);
		UIThreadDispatcher.dispatch(new Runnable(){
			@Override
			public void run() {
				LinphonePreferences.instance().setPushNotificationRegistrationID(regId);
			}
		});
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		KLog.i(TAG, "========== GCMService.onUnregistered  " + "context = [" + context + "], regId = [" + regId + "]");
		initLogger(context);
		Log.w("[Push Notification] Unregistered: " + regId);
		
		UIThreadDispatcher.dispatch(new Runnable(){
			@Override
			public void run() {
				LinphonePreferences.instance().setPushNotificationRegistrationID(null);
			}
		});
	}
	
	protected String[] getSenderIds(Context context) {
		KLog.i(TAG, "========== GCMService.getSenderIds  " + "context = [" + context + "]");
	    return new String[] { context.getString(R.string.push_sender_id) };
	}
}
