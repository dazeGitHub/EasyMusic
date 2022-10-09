package com.example.easymusic;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {
	private static final String TAG = "MyApplication";
	private static Context mContext;
	private static List<Activity> activityList = new ArrayList<Activity>();

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this.getApplicationContext();
	}

	public static Context getmContext() {
		return mContext;
	}

	public static List<Activity> getActivityList() {
		return activityList;
	}

	@Override
	public void onTerminate() {
		Log.d(TAG, "onTerminate");
		activityList.clear();
		mContext = null;
		super.onTerminate();
	}

}
