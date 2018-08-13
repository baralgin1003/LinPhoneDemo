package com.example.administrator.linphonedemo.linphone;

/*
SoftVolume.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.example.administrator.linphonedemo.R;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.capture.hwconf.Hacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Helpers.
 */
public final class LinphoneUtils {

	private LinphoneUtils(){}

	//private static final String sipAddressRegExp = "^(sip:)?(\\+)?[a-z0-9]+([_\\.-][a-z0-9]+)*@([a-z0-9]+([\\.-][a-z0-9]+)*)+\\.[a-z]{2,}(:[0-9]{2,5})?$";
	//private static final String strictSipAddressRegExp = "^sip:(\\+)?[a-z0-9]+([_\\.-][a-z0-9]+)*@([a-z0-9]+([\\.-][a-z0-9]+)*)+\\.[a-z]{2,}$";

	public static boolean isSipAddress(String numberOrAddress) {
		try {
			LinphoneCoreFactory.instance().createLinphoneAddress(numberOrAddress);
			return true;
		} catch (LinphoneCoreException e) {
			return false;
		}
	}

	public static boolean isNumberAddress(String numberOrAddress) {
		LinphoneProxyConfig proxy = LinphoneManager.getLc().createProxyConfig();
		return proxy.normalizePhoneNumber(numberOrAddress) != null;
	}

	public static boolean isStrictSipAddress(String numberOrAddress) {
		return isSipAddress(numberOrAddress) && numberOrAddress.startsWith("sip:");
	}

	public static String getAddressDisplayName(String uri){
		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(uri);
			return getAddressDisplayName(lAddress);
		} catch (LinphoneCoreException e) {
			return null;
		}
	}

	public static String getAddressDisplayName(LinphoneAddress address){
		if(address.getDisplayName() != null) {
			return address.getDisplayName();
		} else {
			if(address.getUserName() != null){
				return address.getUserName();
			} else {
				return address.asStringUriOnly();
			}
		}
	}

	public static String getUsernameFromAddress(String address) {
		if (address.contains("sip:"))
			address = address.replace("sip:", "");

		if (address.contains("@"))
			address = address.split("@")[0];

		return address;
	}

	public static boolean onKeyBackGoHome(Activity activity, int keyCode, KeyEvent event) {
		if (!(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)) {
			return false; // continue
		}

		activity.startActivity(new Intent()
			.setAction(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_HOME));
		return true;
	}

	public static boolean onKeyVolumeAdjust(int keyCode) {
		if (!((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				&& (Hacks.needSoftvolume())|| Build.VERSION.SDK_INT >= 15)) {
			return false; // continue
		}

		if (!LinphoneService.isReady()) {
			Log.i("Couldn't change softvolume has service is not running");
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			LinphoneManager.getInstance().adjustVolume(1);
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			LinphoneManager.getInstance().adjustVolume(-1);
		}
		return true;
	}




	public static final List<LinphoneCall> getLinphoneCalls(LinphoneCore lc) {
		// return a modifiable list
		return new ArrayList<LinphoneCall>(Arrays.asList(lc.getCalls()));
	}

	public static final List<LinphoneCall> getCallsInState(LinphoneCore lc, Collection<State> states) {
		List<LinphoneCall> foundCalls = new ArrayList<LinphoneCall>();
		for (LinphoneCall call : getLinphoneCalls(lc)) {
			if (states.contains(call.getState())) {
				foundCalls.add(call);
			}
		}
		return foundCalls;
	}

	public static boolean isCallRunning(LinphoneCall call)
	{
		if (call == null) {
			return false;
		}

		State state = call.getState();

		return state == State.Connected ||
				state == State.CallUpdating ||
				state == State.CallUpdatedByRemote ||
				state == State.StreamsRunning ||
				state == State.Resuming;
	}

	public static boolean isCallEstablished(LinphoneCall call) {
		if (call == null) {
			return false;
		}

		State state = call.getState();

		return isCallRunning(call) ||
				state == State.Paused ||
				state == State.PausedByRemote ||
				state == State.Pausing;
	}

	public static boolean isHighBandwidthConnection(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && isConnectionFast(info.getType(),info.getSubtype()));
    }

	private static boolean isConnectionFast(int type, int subType){
		if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            	return false;
            }
		}
        //in doubt, assume connection is good.
        return true;
    }


	public static String getDisplayableUsernameFromAddress(String sipAddress) {
		String username = sipAddress;
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) return username;

		if (username.startsWith("sip:")) {
			username = username.substring(4);
		}

		if (username.contains("@")) {
			String domain = username.split("@")[1];
			LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
			if (lpc != null) {
				if (domain.equals(lpc.getDomain())) {
					return username.split("@")[0];
				}
			}
		}
		return username;
	}

}

