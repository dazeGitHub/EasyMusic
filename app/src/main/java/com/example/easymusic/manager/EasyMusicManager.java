package com.example.easymusic.manager;

import com.example.easymusic.MainActivity;
import com.example.easymusic.PlayMusicActivity;
import com.example.easymusic.service.MusicService;

public class EasyMusicManager {
	
	private static final String TAG = "EasyMusicManager";
	
	private MainActivity mMainActivity = null;
	
	private PlayMusicActivity mPlayActivity = null;
	
	private MusicService mMusicService = null;
	
	private boolean isDestroyed = false;
	
	
	private EasyMusicManager() {
		
	}
	
	public static EasyMusicManager getInstance() {
		return ManagerHolder.instance;
	}
	
	private static class ManagerHolder {
		private static final EasyMusicManager instance = new EasyMusicManager();
	}
	
	public boolean confirmCloseEasyMusic() {
		boolean result = true;
		if (mMainActivity != null) mMainActivity.finish();
		if (mPlayActivity != null) mPlayActivity.finish();
		//if (mMusicService != null) mMusicService.
		
		return result;
	}

	public MainActivity getmMainActivity() {
		return mMainActivity;
	}

	public void setmMainActivity(MainActivity mMainActivity) {
		this.mMainActivity = mMainActivity;
	}

	public PlayMusicActivity getmPlayActivity() {
		return mPlayActivity;
	}

	public void setmPlayActivity(PlayMusicActivity mPlayActivity) {
		this.mPlayActivity = mPlayActivity;
	}

	public MusicService getmMusicService() {
		return mMusicService;
	}

	public void setmMusicService(MusicService mMusicService) {
		this.mMusicService = mMusicService;
	}

}
