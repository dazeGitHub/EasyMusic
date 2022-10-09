package com.example.easymusic;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.easymusic.fragment.NetFragment;
import com.example.easymusic.myview.LrcView;
import com.example.easymusic.service.MusicService;
import com.example.easymusic.util.DownloadUtil;
import com.example.easymusic.util.StringUtil;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class PlayMusicActivity extends Activity implements MusicService.Watcher {
	private static final String TAG = "PlayMusicActivity";
	/**
	 * 发送音乐播放控制的广播给MianActivity，音乐的控制统一交给MainActivity管理
	 * 下一首，暂停，上一首，播放，继续播放
	 */
	private static final String ACTION_NEXT_SONG = "action.nextsong";
	private static final String ACTION_PAUSE = "action.pause";
	private static final String ACTION_PRE_SONG = "action.presong";
	private static final String ACTION_PLAY_SONG = "action.playsong";
	private static final String ACTION_CONTINUE_PLAYING_SONG = "action.continueplaying";
	/**
	 * 接收广播，下载歌词完成，下载专辑图片完成，更新播放状态
	 */
	private static final String ACTION_DOWNLOADLRC_SUCCESS = "action_downloadlrc_success";
	private static final String ACTION_DOWNLOADPIC_SUCCESS = "action_downloadpic_success";
	private static final String ACTION_UPDATE_PLAYSTATE = "action.update.playstate";
	/**
	 * 上一首，播放/暂停，下一首，专辑图片， 循环
	 */
	private ImageView pre, playAndPause, next, albumPic, cycleView;
	/**
	 * 音乐标题，歌手，时长，已播放时长
	 */
	private TextView title, artist, duration, playedTimeView;
	//加载歌词
	private TextView loadLrc;
	//歌词View--自定义View
	private LrcView lrcView;
	//进度条
	private SeekBar seekBar;
	//表征当前播放状态
	private boolean isPlaying;
	//表征暂停键是否按下，若为true则下次点击播放为继续播放，和isPlaying不冲突
	private boolean pause;
	//音乐播放器后台服务
	private MusicService musicService;
	private Context mContext;
	//当前播放音乐的标题和歌手和时长
	private String currentMusicTitle, currentMusicArtist;
	private int currentMusicDuration;
	//播放界面专辑图片的动画-旋转
	private Animation playAnimation;
	//当前播放的进度
	private int currentProgress;
	private Handler myHandler;
	private static final int UPDATE_PROGRESS = 1;
	//添加网络请求队列
	private RequestQueue mQueue;
	//搜索歌词的API
	public static final String lrcApi = "http://geci.me/api/lyric/";
	//歌词下载的URL和网络响应
	private String lrcUrl;
	private String lrcResponse;
	//缓存专辑图片，避免每次从本地或网络加载
	private Map<String, SoftReference<Bitmap>> playImageCacheMap = new HashMap<String, SoftReference<Bitmap>>();
	//当前歌词是否正在展示
	private boolean showLrc = false;
	//循环控制切换对应的图片资源
	private int[] cycleViewResource;
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.play);
		mContext = this;
		MyApplication.getActivityList().add(this);
		//绑定服务并获得musicService的引用
		bindToService();
		cycleViewResource = new int[] {R.drawable.cycle_list, R.drawable.cycle_single, R.drawable.cycle_random};
		pre = (ImageView) findViewById(R.id.pre);
		pre.setOnClickListener(musicClickListener);
		playAndPause = (ImageView) findViewById(R.id.playAndpause);
		playAndPause.setOnClickListener(musicClickListener);
		next = (ImageView) findViewById(R.id.next);
		next.setOnClickListener(musicClickListener);
		albumPic = (ImageView) findViewById(R.id.lrcpic);
		title = (TextView) findViewById(R.id.title);
		artist = (TextView) findViewById(R.id.artist);
		duration = (TextView) findViewById(R.id.duration);
		playedTimeView = (TextView) findViewById(R.id.playedtime);
		lrcView = (LrcView) findViewById(R.id.lrcview);
		loadLrc = (TextView) findViewById(R.id.loadlrc);
		loadLrc.setOnClickListener(musicClickListener);
		cycleView = (ImageView) findViewById(R.id.cycleview);
		cycleView.setOnClickListener(musicClickListener);
		mQueue = Volley.newRequestQueue(mContext);
		playAnimation = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
		seekBar = (SeekBar) findViewById(R.id.musicProgress);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {//手动滑动进度条时的监听
					changeProgressFromUser(progress);//通知后台musicService改变播放进度
					//歌词信息跳转进度
					lrcView.checkLrcTime(StringUtil.formatDuration(progress), progress - currentProgress);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
		});
		myHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case UPDATE_PROGRESS://更新播放进度
					int progress = (Integer)msg.obj;
					seekBar.setProgress(progress);
					String playedTime = StringUtil.formatDuration(progress);
					playedTimeView.setText(playedTime);
					lrcView.checkLrcTime(playedTime, 1);
					Log.d(TAG, "update progress success!");
					break;
				default:
					break;
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_DOWNLOADLRC_SUCCESS);
		filter.addAction(ACTION_DOWNLOADPIC_SUCCESS);
		filter.addAction(ACTION_UPDATE_PLAYSTATE);
		mContext.registerReceiver(updateReceiver, filter);
	}
	
	// 绑定服务时的ServiceConnection参数
		private ServiceConnection conn = new ServiceConnection() {

			// 绑定成功后该方法回调，并获得服务端IBinder的引用
			public void onServiceConnected(ComponentName name, IBinder service) {
				// 通过获得的IBinder获取PlayMusicService的引用
				musicService = ((MusicService.MusicBinder) service).getService();
				musicService.addWatcher(PlayMusicActivity.this);
				//Toast.makeText(mContext, "onServiceConnected:musicService", Toast.LENGTH_LONG).show();
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
	
	//后台播放服务改变播放进度
	public void changeProgressFromUser(int progress) {
		musicService.changePlayProgress(progress);
	}

	protected void onStart() {
		updataPlayState();
		super.onStart();
	}
	
	

	protected void onDestroy() {
		musicService = null;
		this.unbindService(conn);
		this.unregisterReceiver(updateReceiver);
		super.onDestroy();
	}
	
	//更新播放状态
	private void updataPlayState() {
		Log.d(TAG, "updataPlayState");
		currentMusicTitle = MainActivity.currentMusicTitle;
		currentMusicArtist = MainActivity.currentMusicArtist;
		currentMusicDuration = (int) MainActivity.currentMusicDuration;
		isPlaying = MainActivity.isPlaying;
		pause = MainActivity.pause;
		seekBar.setMax((int)currentMusicDuration);
		title.setText(currentMusicTitle);
		artist.setText(currentMusicArtist);
		duration.setText(StringUtil.formatDuration(currentMusicDuration));
		if (isPlaying) {
			playAndPause.setImageResource(android.R.drawable.ic_media_pause);
			albumPic.startAnimation(playAnimation);
		} else {
			playAndPause.setImageResource(android.R.drawable.ic_media_play);
			albumPic.clearAnimation();
		}
		showAlbumPic();
		showLrc();
	}
	
	//控制音乐的播放转交给MianActivity
	private void changeMusicState(Intent intent) {
		Log.d(TAG, "changeMusicState:action = " + intent.getAction());
		this.sendBroadcast(intent);
	}
	
	private OnClickListener musicClickListener = new OnClickListener() {
		public void onClick(View v) {
			if (v == pre) {//上一首
				isPlaying = true;
				pause = false;
				showLrc = false;
				Intent intent = new Intent(ACTION_PRE_SONG);
				changeMusicState(intent);
			} else if (v == playAndPause) {//播放或者暂停
				if (pause) {//如果点击过暂停，下一次点击播放应当是继续播放
					isPlaying = true;
					pause = false;
					Intent intent = new Intent(ACTION_CONTINUE_PLAYING_SONG);
					changeMusicState(intent);
					return;
				}
				if (isPlaying) {//当前播放状态点击暂停
					isPlaying = false;
					pause = true;
					Intent intent = new Intent(ACTION_PAUSE);
					changeMusicState(intent);
				} else {//首次点击播放键，此时isPlaying和pause均为false
					isPlaying = true;
					Intent intent = new Intent(ACTION_PLAY_SONG);
					changeMusicState(intent);
				}
			} else if (v == next) {//下一首
				isPlaying = true;
				pause = false;
				showLrc = false;
				Intent intent = new Intent(ACTION_NEXT_SONG);
				changeMusicState(intent);
			} else if (v == loadLrc) {//加载歌词
				showLrc = true;
				String  localUrl = getLocalPath(0);
				File file = new File( localUrl);
				if (file.exists()) {
					showLrc();
					return;
				}
				searchLrc(currentMusicTitle, currentMusicArtist);
			} else if (v == cycleView) {//切换循环模式
				MainActivity.cycle += 1;
				if (MainActivity.cycle > 3) MainActivity.cycle = 1;
				cycleView.setImageResource(cycleViewResource[MainActivity.cycle-1]);
			}
		}
		
	};
	
	//搜索歌词
	private void searchLrc(String title, String artist) {
		String searchTitle = null;
		String searchArtist = null;
		Log.d(TAG, currentMusicTitle + currentMusicArtist);
		try {
			searchTitle = URLEncoder.encode(StringUtil.removeReg(currentMusicTitle, null), "UTF-8");
			searchArtist = URLEncoder.encode(StringUtil.removeReg(currentMusicArtist, null), "UTF-8");
			Log.d(TAG, searchTitle + searchArtist);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = lrcApi + searchTitle + "/" + searchArtist;
		if (searchTitle.indexOf("+")>=0 || searchArtist.indexOf("+")>=0) {
			url = url.replace('+', ' ');
		}
		Log.d(TAG, "url = " + url );
		StringRequest stringRequest = new StringRequest(Method.GET,url,  
                new Response.Listener<String>() {  
                    @Override  
                    public void onResponse(String response) {
                        Log.d(TAG, "response = " + response); 
                        //搜索歌词得到的响应
                        lrcResponse = response;          
                        analysisLrcUrl();
                    }  
                }, 
                new Response.ErrorListener() {  
                    @Override  
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.getMessage(), error);  
                        Toast.makeText(PlayMusicActivity.this, "加载失败！", 300).show();
                    }  
                });
		mQueue.add(stringRequest);	
	}
	
	//搜索专辑图片
	private void searchAlbumPic(String title, String artist) {
		Toast.makeText(mContext, "搜索专辑图片...", 300).show();
		//有些歌名和歌手信息中可能会携带些非原始的特殊符号需要去掉，比如《》.,之类的
		String ti = StringUtil.removeReg(title, null);
		String ar = StringUtil.removeReg(artist, null);
		Log.d(TAG, "searchAlbumPic:ti = " + ti + " ar = " + ar); 
		//启动查询歌词的异步任务
		new NetFragment().new SearchMusicTask(ti, ar, mContext).execute(NetFragment.getRealUrl(title));
	}
	
	//从响应中分离出歌词下载的URL
	private void analysisLrcUrl() {
		
		try {
			JSONObject jo = new JSONObject(lrcResponse);
			JSONArray result = jo.getJSONArray("result");
			JSONObject firstResult = result.getJSONObject(0);
			String lrc = firstResult.getString("lrc");
			lrcUrl = lrc;
			downloadLrc(lrcUrl);
			Log.d(TAG, "lrc = " + lrc);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(PlayMusicActivity.this, "抱歉，未搜索到歌词！", 300).show();
		}
	}
	
	//下载歌词
	private void downloadLrc(String lrcUrl) {
		final String musicLrc = lrcUrl;
		Toast.makeText(mContext, "正在下载歌词，请稍候...", 300).show();
		String  localFile = getLocalPath(0);
		File file = new File(localFile);
		//开启一个线程下载歌词
		DownloadUtil lrcUtil = new DownloadUtil(musicLrc, localFile, mContext, 1);
		musicService.downloadMusic(lrcUtil);
	}
	
	//下载专辑图片
	public void downloadAlbumPic(String albumPicUrl) {
		if (albumPicUrl == null) {
			Toast.makeText(mContext, "未搜索到匹配图片", 300).show();
			albumPic.setImageResource(R.drawable.rotate);
			return;
		}
		final String musicAlbumPic = albumPicUrl;
		//Toast.makeText(mContext, "正在下载专辑图片，请稍候...", 300).show();
		String localFile = getLocalPath(1);
		File file = new File(localFile);
		//开启两个线程下载专辑图片
		DownloadUtil albumPicUtil = new DownloadUtil(musicAlbumPic, localFile, mContext, 2);
		musicService.downloadMusic(albumPicUtil);
	}

	//显示歌词
	protected void showLrc() {
		if (!showLrc) {
			lrcView.setLrcList(null);
			lrcView.setCurrentLrcIndex(0);
			lrcView.setVisibility(View.GONE);
			loadLrc.setVisibility(View.VISIBLE);
			return;
		}
		String  localFile = getLocalPath(0);
		File file = new File(localFile);
		if (!file.exists()) return;
		//歌词的解析只需传进去一个歌词文件路径，自定义view-lrcView内部已经封装好
		lrcView.loadLrc(localFile);
		loadLrc.setVisibility(View.INVISIBLE);
	}
	
	//显示专辑图片
	private void showAlbumPic() {
		//首先从缓存中查询是否已加载有该专辑图片
		if (playImageCacheMap != null) {
			Log.d(TAG, "playImageCacheMap.size = " + playImageCacheMap.size());
			SoftReference<Bitmap> sBitmap = 
					playImageCacheMap.get(currentMusicTitle+currentMusicArtist);
			if (sBitmap != null) {
				Bitmap bitmap = sBitmap.get();
				if (bitmap != null) {
					albumPic.setImageBitmap(bitmap);
					Log.d(TAG, "get image from ImageCacheMap");
					return;
				} else {
					playImageCacheMap.remove(currentMusicTitle+currentMusicArtist);
				}
			}
		}
		
		//如果缓存中还未存入该专辑图片或者已经被回收，从本地加载并添加到缓存
		final String  localFile = getLocalPath(1);
		File file = new File(localFile);
		if (file.exists()) {
			myHandler.post(new Runnable() {
				public void run() {
					Bitmap bitmap = BitmapFactory.decodeFile(localFile);
					//int size = Math.min(lrcPic.getWidth(), lrcPic.getHeight());
					//Bitmap bitmap = BitmapUtil.getScropBitmap(localFile, size, size);
					playImageCacheMap.put(currentMusicTitle + currentMusicArtist, new SoftReference<Bitmap>(bitmap));
					Log.d(TAG, "add bitmap to ImageCacheMap, bytes = " + bitmap.getByteCount());
					albumPic.setImageBitmap(bitmap);
				}
			});
			return;
		}
		//如果缓存和本地均没有专辑图片则从网络中搜索并加载
		searchAlbumPic(currentMusicTitle, currentMusicArtist);
	}
	
	//获取本地文件路径，type为0表示歌词路径，为1表示专辑图片路径
	private String getLocalPath(int type) {
		String result = null;
		MainActivity.createFileDir();
		if (type == 0) {
			result = MainActivity.downloadedPath
					+ "/lrc/"+ currentMusicTitle + "-" + currentMusicArtist + ".lrc";
		} else {
			result = MainActivity.downloadedPath 
			+ "/album/"+ currentMusicTitle + "-" + currentMusicArtist + ".jpg";
		}
		
		return result;
	}

	@Override
	public void update(int currentProgress) {
		this.currentProgress = currentProgress;
		myHandler.sendMessage(myHandler.obtainMessage(UPDATE_PROGRESS, currentProgress));
	}
	
	private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_DOWNLOADLRC_SUCCESS)) {
				Toast.makeText(mContext, "下载歌词成功！", 100).show();
				showLrc();
			} else if (intent.getAction().equals(ACTION_DOWNLOADPIC_SUCCESS)) {
				Toast.makeText(mContext, "下载专辑图片成功！", 100).show();
				showAlbumPic();
			} else if (intent.getAction().equals(ACTION_UPDATE_PLAYSTATE)) {
				boolean autoChange = intent.getBooleanExtra("autoChange", false);
				if (autoChange) showLrc = !autoChange;
				updataPlayState();
			}
		}
		
	};

}
