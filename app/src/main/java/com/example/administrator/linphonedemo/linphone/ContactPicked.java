package com.example.administrator.linphonedemo.linphone;

import android.net.Uri;

public interface ContactPicked {
	void setAddresGoToDialerAndCall(String number, String name, Uri photo);
}
