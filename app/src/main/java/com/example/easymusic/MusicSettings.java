package com.example.easymusic;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MusicSettings extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.pref_settings);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
