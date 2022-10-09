package com.example.easymusic;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.easymusic.fragment.DownloadFragment;
import com.example.easymusic.fragment.MusicListFragment;
import com.example.easymusic.fragment.NetFragment;
import com.example.easymusic.fragment.StoredSongFragment;
import com.example.easymusic.mysql.MyDBHelper;
import com.example.easymusic.myview.MyProgressBar;
import com.example.easymusic.service.MusicService;
import com.example.easymusic.util.BitmapUtil;
import com.example.easymusic.util.DownloadUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends FragmentActivity implements
		MusicListFragment.CallBack, StoredSongFragment.CallBack,
		DownloadFragment.CallBack, View.OnClickListener, MusicService.Watcher {

	private static final String TAG = "MainActivity";
	private static final String ACTION_NEXT_SONG = "action.nextsong";
	private static final String ACTION_PAUSE = "action.pause";
	private static final String ACTION_PRE_SONG = "action.presong";
	private static final String ACTION_PLAY_SONG = "action.playsong";
	private static final String ACTION_CONTINUE_PLAYING_SONG = "action.continueplaying";
	private static final String ACTION_UPDATE_PLAYSTATE = "action.update.playstate";
	private static final String ACTION_EXIT = "action.exit";
	private static final String ACTION_PLAY_AND_PAUSE = "action.playandpause";
	public static final int FRAGMENT_COUNT = 4;
	// dbMusic保存媒体库中的所有音乐
	private ViewPager mViewPager;
	private SectionsPagerAdapter mSectionsPagerAdapter;
	// 不同音乐列表：所有音乐，收藏音乐
	private List<Map<String, Object>> allMusic = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> storedMusic = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> currentPlayMusic = null;
	private List<Fragment> fragmentList = new ArrayList<Fragment>();
	private Context mContext;
	private TextView fragmentTitle1, fragmentTitle2, fragmentTitle3,
			fragmentTitle4, titleBottomLine;
	// 音乐控制按钮
	private ImageButton preButton, playAndPauseButton, nextButton;
	private ImageView playTag;
	private TextView musicInfo;
	// 当前音乐播放的进度
	private MyProgressBar mainProgressBar;
	// screenWidth表示屏幕宽度
	private int screenWidth, bottomLineWidth;
	// 音乐后台服务，负责音乐的播放，执行下载任务
	private MusicService musicService;
	private MusicService.MusicBinder mBinder;
	private MyDBHelper myHelper;
	public static final String downloadedPath = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/musicplayer";
	// 当前播放的音乐信息
	public static String currentMusicTitle, currentMusicArtist;
	public static int currentMusicPos;
	public static long currentMusicDuration;
	public static boolean isPlaying, pause;
	private boolean autoChange;
	// 切换播放列表：1-所有；2-收藏；3-已下载
	public static final int PLAY_ALL_MUSIC = 1;
	public static final int PLAY_STORED_MUSIC = 2;
	public static final int PLAY_DOWNLOADED_MUSIC = 3;
	// 处理改变音乐的广播AUTO_CHANGE_SONG表示一首歌播放完自动播放下一首
	public static final int AUTO_CHANGE_SONG = 4;
	public static final int REFRESH_SONG = 5;
	private static final int UPDATE_PROGRESS = 6;
	private Handler myHandler;
	// 缓存专辑图片，当下载完成并播放歌曲时，点击播放图标加载专辑图标时避免每次重新联网搜索，直接从缓存中获取
	private Map<String, SoftReference<Bitmap>> ImageCacheMap = new HashMap<String, SoftReference<Bitmap>>();
	// 循环播放控制
	private static final int list_crcle = 1;// 列表循环
	private static final int single_cycle = 2;// 单曲循环
	private static final int random_cycle = 3;// 随机循环
	public static int cycle = list_crcle;// 默认列表循环
	private NotificationManager nm;
	private RemoteViews contentViews;
	private Notification notify;
	private int NOTIFICATION_ID = 123;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		MyApplication.getActivityList().add(this);
		myHelper = new MyDBHelper(mContext, "easyMusic.db3", null, 1);
		createFileDir();
		getAllMusicFromDb();
		getStoredMusic(storedMusic, myHelper);
		getBottomLineWidth();
		fragmentList.add(new MusicListFragment());
		fragmentList.add(new StoredSongFragment());
		fragmentList.add(new NetFragment());
		fragmentList.add(new DownloadFragment());
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		fragmentTitle1 = (TextView) findViewById(R.id.fragment1);
		fragmentTitle1.setOnClickListener(this);
		fragmentTitle2 = (TextView) findViewById(R.id.fragment2);
		fragmentTitle2.setOnClickListener(this);
		fragmentTitle3 = (TextView) findViewById(R.id.fragment3);
		fragmentTitle3.setOnClickListener(this);
		fragmentTitle4 = (TextView) findViewById(R.id.fragment4);
		fragmentTitle4.setOnClickListener(this);
		titleBottomLine = (TextView) findViewById(R.id.fragmentTitle);
		preButton = (ImageButton) findViewById(R.id.pre);
		preButton.setOnClickListener(this);
		playAndPauseButton = (ImageButton) findViewById(R.id.playandpause);
		playAndPauseButton.setOnClickListener(this);
		nextButton = (ImageButton) findViewById(R.id.next);
		nextButton.setOnClickListener(this);
		playTag = (ImageView) findViewById(R.id.playtag);
		playTag.setOnClickListener(this);
		musicInfo = (TextView) findViewById(R.id.musicInfo);
		mainProgressBar = (MyProgressBar) findViewById(R.id.mainProgress);
		myHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case AUTO_CHANGE_SONG:
					autoChange = true;
					nextSong();
					break;
				case REFRESH_SONG:
					refreshSong();
				case UPDATE_PROGRESS:
					int progress = (Integer) msg.obj;
					mainProgressBar.setProgress(progress);
					mainProgressBar.invalidate();
					break;
				default:
					break;
				}
			}

		};
		currentPlayMusic = allMusic;
		bindToService();
		// bindToDownloadService();
		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageSelected(int position) {

			}

			public void onPageScrolled(int item, float percent, int offset) {
				titleBottomLine.setX(item * bottomLineWidth + offset
						/ FRAGMENT_COUNT);
			}

			public void onPageScrollStateChanged(int position) {

			}
		});
		registerPlayReceiver();
		updatePlayMusicInfo();
		initNotification();
	}

	private void registerPlayReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_NEXT_SONG);
		filter.addAction(ACTION_PAUSE);
		filter.addAction(ACTION_PRE_SONG);
		filter.addAction(ACTION_PLAY_SONG);
		filter.addAction(ACTION_CONTINUE_PLAYING_SONG);
		filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		filter.addAction(ACTION_EXIT);
		filter.addAction(ACTION_PLAY_AND_PAUSE);
		this.registerReceiver(this.playMusicReceiver, filter);
	}

	protected void refreshSong() {
		getAllMusicFromDb();
	}

	@Override
	protected void onStart() {
		if (showNotification) {
			nm.cancel(NOTIFICATION_ID);
			showNotification = false;
		}
		
		updatePlayMusicInfo();
		super.onStart();
	}

	private void updatePlayMusicInfo() {
		if(currentPlayMusic != null && currentPlayMusic.size() != 0){
			currentMusicTitle = (String) currentPlayMusic.get(currentMusicPos).get("title");
			currentMusicArtist = (String) currentPlayMusic.get(currentMusicPos).get("artist");
			currentMusicDuration = (Long) (currentPlayMusic.get(currentMusicPos).get("duration"));
		}
		musicInfo.setText(currentMusicTitle + "-" + currentMusicArtist);
		if (isPlaying) {
			musicInfo.requestFocus();
			musicInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
			playAndPauseButton
					.setImageResource(android.R.drawable.ic_media_pause);
		} else {
			playAndPauseButton
					.setImageResource(android.R.drawable.ic_media_play);
		}
		if (showNotification) showNotification();
		notifyPlayActivity();
	}

	private void notifyPlayActivity() {
		Intent intent = new Intent(ACTION_UPDATE_PLAYSTATE);
		intent.putExtra("title", currentMusicTitle);
		intent.putExtra("artist", currentMusicArtist);
		intent.putExtra("isPlaying", isPlaying);
		intent.putExtra("duration", currentMusicDuration);
		intent.putExtra("autoChange", autoChange);
		sendBroadcast(intent);
		autoChange = false;
	}

	public static void createFileDir() {
		File musciPlayer = new File(downloadedPath);

		try {
			if (!musciPlayer.exists())
				musciPlayer.mkdir();
			File lrc = new File(downloadedPath + "/lrc");
			if (!lrc.exists())
				lrc.mkdir();
			File album = new File(downloadedPath + "/album");
			if (!album.exists())
				album.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(TAG, "createFileDir");
	}

	private void getBottomLineWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidth = dm.widthPixels;
		bottomLineWidth = screenWidth / FRAGMENT_COUNT;

	}

	// 绑定服务时的ServiceConnection参数
	private ServiceConnection conn = new ServiceConnection() {

		// 绑定成功后该方法回调，并获得服务端IBinder的引用
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBinder = (MusicService.MusicBinder)service;
			// 通过获得的IBinder获取PlayMusicService的引用
			musicService = mBinder.getService();
			musicService.setHandler(myHandler);
			musicService.setImageCacheMap(ImageCacheMap);
			musicService.addWatcher(MainActivity.this);
			// Toast.makeText(mContext, "onServiceConnected:musicService",
			// Toast.LENGTH_LONG).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "onServiceDisconnected:musicService");
		}

	};

	// 绑定服务MusicService
	private void bindToService() {
		bindService(new Intent(mContext,
				MusicService.class), conn,
				Service.BIND_AUTO_CREATE);
	}

	// 通过获得的MusicService引用调用播放音乐的方法，方法传进去的参数为音乐url
	protected void playMusic(int position) {
		if(currentPlayMusic == null || currentPlayMusic.size() == 0){
			Toast.makeText(this, "当前播放音乐的列表为空", Toast.LENGTH_SHORT).show();
			return;
		}
		String url = (String) currentPlayMusic.get(position).get("url");
		if (musicService != null) {
			musicService.play(url);
		}
		isPlaying = true;
		pause = false;
		updatePlayMusicInfo();
	}

	//下一首
	private void nextSong() {
		switch (cycle) {
		case list_crcle:
			currentMusicPos = currentMusicPos == currentPlayMusic.size() - 1 ? 0
					: currentMusicPos + 1;
			break;
		case single_cycle:
			// do nothing
			break;
		case random_cycle:
			currentMusicPos = (int) (Math.random() * (currentPlayMusic.size() - 1));
			break;
		}

		playMusic(currentMusicPos);
	}

	//上一首
	private void preSong() {
		switch (cycle) {
		case list_crcle:
			currentMusicPos = currentMusicPos == 0 ? currentPlayMusic.size() - 1
					: currentMusicPos - 1;
			break;
		case single_cycle:
			// do nothing
			break;
		case random_cycle:
			currentMusicPos = (int) (Math.random() * (currentPlayMusic.size() - 1));
			break;
		}

		playMusic(currentMusicPos);
	}

	//暂停
	private void pauseMusic() {
		if (musicService != null) {
			musicService.pauseMusic();
		}
		isPlaying = false;
		pause = true;
		updatePlayMusicInfo();
	}

	//继续播放
	private void continuePlaying() {
		musicService.continuePlaying();
		isPlaying = true;
		pause = false;
		updatePlayMusicInfo();
	}

	//刷新音乐列表，返回值表示增加几首歌
	public int refreshMusicList() {
		int preSize = allMusic.size();
		getAllMusicFromDb();
		int refreshedSize = allMusic.size();
		return refreshedSize - preSize;
	}

	// 从媒体库中查询音乐
	private void getAllMusicFromDb() {
		if (allMusic.size() > 0)
			allMusic.clear();
		Cursor musicCursor1 = this.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		// 从外部存储获取
		getMusic(musicCursor1);
		Cursor musicCursor2 = this.getContentResolver().query(
				MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		// 从内部存储获取
		getMusic(musicCursor2);
	}

	// 检查当前歌曲是否在收藏列表中，在收藏列表返回true，否则返回false
	protected boolean checkIfStored(String url) {
		for (Map<String, Object> map : storedMusic) {
			if (url.equals((String) map.get("url"))) {
				return true;
			}
		}
		return false;
	}
	
	private int safeGetColumnIndex(Cursor musicCursor, String columnName){
		int result = musicCursor.getColumnIndex(columnName);
		return result == -1 ? 0 : result;
	}

	// 获取到的音乐以Map的形式存储在dbMusic中
	private void getMusic(Cursor musicCursor) {
		while (musicCursor.moveToNext()) {
			Map<String, Object> item = new HashMap<String, Object>();
			long id = musicCursor.getLong(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media._ID));
			String title = musicCursor.getString(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media.TITLE));
			String artist = musicCursor.getString(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media.ARTIST));
			if (artist != null && artist.equals("<unknown>")) {
				continue;
			}
			long duration = musicCursor.getLong(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media.DURATION));
			long size = musicCursor.getLong(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media.SIZE));
			String url = musicCursor.getString(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media.DATA));
			int isMusic = musicCursor.getInt(safeGetColumnIndex(musicCursor, MediaStore.Audio.Media.IS_MUSIC));
			if (isMusic != 0) {
				item.put("id", id);
				item.put("title", title);
				item.put("artist", artist);
				// item.put("duration", formatDuration(duration));
				item.put("duration", duration);
				item.put("size", size);
				item.put("url", url);
				Log.d("MainActivity", "MusicTitle = " + title);
				Log.d("MainActivity", "MusicArtist = " + artist);
				Log.d("MainActivity", "MusicUrl = " + url);
				allMusic.add(item);
			}

		}
	}

	// 从数据库中查询已收藏音乐
	private void getStoredMusic(List<Map<String, Object>> storedMusic,
			MyDBHelper myHelper) {
		if (storedMusic.size() > 0)
			storedMusic.clear();
		Cursor cursor = myHelper.getReadableDatabase().rawQuery(
				"select * from stored_music", null);
		while (cursor.moveToNext()) {
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("title", cursor.getString(1));
			item.put("artist", cursor.getString(2));
			item.put("duration", Long.parseLong(cursor.getString(3)));
			item.put("url", cursor.getString(4));
			storedMusic.add(item);
		}
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragmentList.get(position);
		}

		@Override
		public int getCount() {
			return fragmentList.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			case 3:
				return getString(R.string.title_section4).toUpperCase(l);
			}
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			//Intent intent = new Intent(this, MusicSettings.class);
			//this.startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemSelected(int position, int musicList) {
		currentMusicPos = position;
		switch (musicList) {
		case PLAY_ALL_MUSIC:
			currentPlayMusic = allMusic;
			break;
		case PLAY_STORED_MUSIC:
			currentPlayMusic = storedMusic;
			break;
		case PLAY_DOWNLOADED_MUSIC:
			currentPlayMusic = getDownloadedMusic();
		}
		playMusic(position);
	}
	
	private List<Map<String, Object>> getDownloadedMusic() {
		return ((DownloadFragment)fragmentList.get(FRAGMENT_COUNT-1)).getDownloadedMusic();
	}

	@Override
	public List<Map<String, Object>> getAllMusic() {
		return allMusic;
	}

	@Override
	public List<Map<String, Object>> getStoredMusic() {
		return storedMusic;
	}

	public void executeDownloadUtil(DownloadUtil util) {
		Toast.makeText(mContext, "executeDownloadUtil", Toast.LENGTH_LONG).show();
		musicService.downloadMusic(util);
	}

	@Override
	public void onClick(View v) {
		if (v == fragmentTitle1) {
			mViewPager.setCurrentItem(0);
		} else if (v == fragmentTitle2) {
			mViewPager.setCurrentItem(1);
		} else if (v == fragmentTitle3) {
			mViewPager.setCurrentItem(2);
		} else if (v == fragmentTitle4) {
			mViewPager.setCurrentItem(3);
		}

		if (v == preButton) {
			preSong();
		} else if (v == nextButton) {
			nextSong();
		} else if (v == playAndPauseButton) {
			if (pause) {
				continuePlaying();
				return;
			}
			if (isPlaying) {
				pauseMusic();
			} else {
				playMusic(currentMusicPos);
			}
		} else if (v == playTag) {
			Intent intent = new Intent("com.example.easymusic.playmusic");
			intent.putExtra("title", currentMusicTitle);
			intent.putExtra("artist", currentMusicArtist);
			intent.putExtra("isPlaying", isPlaying);
			intent.putExtra("duration", currentMusicDuration);
			intent.putExtra("isPause", pause);
			intent.putExtra("progress", mainProgressBar.getProgress()
					* currentMusicDuration / 100);
			this.startActivity(intent);
		}
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "mainActivity:onDestroy");
		musicService = null;
		mBinder = null;
		if (showNotification) nm.cancel(NOTIFICATION_ID);
		this.unbindService(conn);
		this.unregisterReceiver(playMusicReceiver);
		isPlaying = false;
		pause = false;
		super.onDestroy();
	}

	public MyDBHelper getMyHelper() {
		return myHelper;
	}

	private BroadcastReceiver playMusicReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "action = " + action);
			if (action.equals(ACTION_NEXT_SONG)) {
				nextSong();
			} else if (action.equals(ACTION_PLAY_SONG)) {
				playMusic(currentMusicPos);
			} else if (action.equals(ACTION_PAUSE)) {
				pauseMusic();
			} else if (action.equals(ACTION_PRE_SONG)) {
				preSong();
			} else if (action.equals(ACTION_CONTINUE_PLAYING_SONG)) {
				continuePlaying();
			} else if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {//在播放器中点击home键时显示通知栏图标
				showNotification();
			} else if (action.equals(ACTION_EXIT)) {//通知栏点击退出图标
				for (Activity a : MyApplication.getActivityList()) {
					if (a != null) a.finish();
				}
			} else if (action.equals(ACTION_PLAY_AND_PAUSE)) {
				if (pause) {
					continuePlaying();
					pause = false;
				} else if (isPlaying) {
					pauseMusic();
				} else {
					playMusic(currentMusicPos);
				}
			}
		}

	};
	private boolean showNotification;
	
	@SuppressLint("NewApi")
	private void initNotification() {
		//NotificationManager的获取
		nm = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
		Intent mainIntent = new Intent(MainActivity.this, MainActivity.class);  
		PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, mainIntent, 0);
		notify = new Notification();
		notify.when = System.currentTimeMillis();
		notify.icon = R.drawable.music;
		notify.contentIntent = pi;//点击通知跳转到MainActivity
		notify.flags = Notification.FLAG_NO_CLEAR;
		contentViews = new RemoteViews(getPackageName(), R.layout.notification); 
        contentViews.setOnClickPendingIntent(R.id.playtag, pi);
        contentViews.setOnClickPendingIntent(R.id.currentmusic, pi);
		//上一首图标添加点击监听
		Intent previousButtonIntent = new Intent(ACTION_PRE_SONG);
        PendingIntent pendPreviousButtonIntent = PendingIntent.getBroadcast(this, 0, previousButtonIntent, 0);  
        contentViews.setOnClickPendingIntent(R.id.pre, pendPreviousButtonIntent);
        //播放/暂停添加点击监听
        Intent playPauseButtonIntent = new Intent(ACTION_PLAY_AND_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, playPauseButtonIntent, 0);  
        contentViews.setOnClickPendingIntent(R.id.playandpause, playPausePendingIntent);
        //下一首图标添加监听
        Intent nextButtonIntent = new Intent(ACTION_NEXT_SONG);
        PendingIntent pendNextButtonIntent = PendingIntent.getBroadcast(this, 0, nextButtonIntent, 0);  
        contentViews.setOnClickPendingIntent(R.id.next, pendNextButtonIntent);        
        //退出监听
        Intent exitButton = new Intent(ACTION_EXIT);  
        PendingIntent pendingExitButtonIntent = PendingIntent.getBroadcast(this,0,exitButton,0);  
        contentViews.setOnClickPendingIntent(R.id.close,pendingExitButtonIntent);
	}
	
	private void showNotification() {
		showNotification = true;
		
		if(isPlaying){  
            contentViews.setImageViewResource(R.id.playandpause,android.R.drawable.ic_media_pause);  
        }
        else{
            contentViews.setImageViewResource(R.id.playandpause,android.R.drawable.ic_media_play);  
        }
		contentViews.setTextViewText(R.id.currentmusic, currentMusicTitle + "—" + currentMusicArtist);
		String filePath = MainActivity.downloadedPath + "/album/"+ currentMusicTitle + "-" + currentMusicArtist + ".jpg";
		if (new File(filePath).exists()) {
			Bitmap bitmap = BitmapUtil.getScropBitmap(filePath, 60, 60);
			contentViews.setImageViewBitmap(R.id.playtag, bitmap);
		} else {
			contentViews.setImageViewResource(R.id.playtag, R.drawable.launcher);
		}
		notify.contentView = contentViews;

		nm.notify(NOTIFICATION_ID, notify);//调用notify方法后即可显示通知
	}

	@Override
	public void update(int currentProgress) {//实现MusicService.Watcher的回调方法，观察者模式的应用，更新播放进度
		int progress = (int) (currentProgress * 100 / currentMusicDuration);
		myHandler.sendMessage(myHandler
				.obtainMessage(UPDATE_PROGRESS, progress));
		mainProgressBar.setProgress(progress);
		Log.d(TAG, "update mainProgressBar: progress = " + progress);
	}

	public List<Fragment> getFragmentList() {
		return fragmentList;
	}

	public Map<String, SoftReference<Bitmap>> getImageCacheMap() {
		return ImageCacheMap;
	}

	private int backPressedCount = 0;
	private long firstBackPressedTime = System.currentTimeMillis() - 2000;
	@Override
	public void onBackPressed() {
		long det = System.currentTimeMillis() - firstBackPressedTime;
		if (backPressedCount == 0) {
			backPressedCount += 1;
			Toast.makeText(mContext, "再按一次退出", Toast.LENGTH_LONG).show();
			firstBackPressedTime = System.currentTimeMillis();
			return;
		} else if (backPressedCount == 1 && det < 2000) {
			super.onBackPressed();
		} else {
			Toast.makeText(mContext, "再按一次退出", Toast.LENGTH_LONG).show();
			firstBackPressedTime = System.currentTimeMillis();
		}
	}
}
