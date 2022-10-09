package com.example.easymusic.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.example.easymusic.MainActivity;
import com.example.easymusic.R;
import com.example.easymusic.util.DownloadUtil;
import com.example.easymusic.util.StringUtil;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class DownloadFragment extends Fragment {
	private final String TAG = "DownloadFragment";
	private static final String ACTION_DOWNLOADMUSIC_SUCCESS = "action_downloadmusic_success";
	//下载界面列表
	private ExpandableListView downloadListView;
	//已完成下载歌曲的list
	private List<Map<String, Object>> downloadedMusic = new ArrayList<Map<String, Object>>();
	//正在下载的歌曲list
	private List<Map<String, Object>> downloadingMusic = new ArrayList<Map<String, Object>>();
	private Context mContext;
	private LayoutInflater inflater;
	//Timer和Handler组合定时更新下载任务和下载进度
	private Timer refreshTimer;
	private Handler handler;
	//ExpandableListView分为“已下载”和“在正下载”两组
	private String[] musicDownloadGroup = new String[] {"最近已完成下载","正在下载"};
	private MainActivity mMainActivity;
	
	private static final int refresh_downloadingList = 1;
	private CallBack mCallBack;

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallBack = (CallBack)activity;
		mMainActivity = (MainActivity) activity;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this.getActivity();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_DOWNLOADMUSIC_SUCCESS);
		mContext.registerReceiver(updateDownloadListReceiver, filter);
		inflater = LayoutInflater.from(mContext);
		refreshTimer = new Timer();
		checkDownloadingMusic();
		Log.d(TAG, "onCreate:downloadingMusic.siez=" + downloadingMusic.size());
	}

	// DownloadFragment向外界展示的内容
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.downloadmusiclist, container,
				false);
		downloadListView = (ExpandableListView) view
				.findViewById(R.id.musicList);
		//downloadListView.setGroupIndicator(this.getResources().getDrawable(R.drawable.expandable_group_selector));
		downloadListView.setAdapter(downloadMusicListAdapter);
		downloadListView.expandGroup(1);
		downloadListView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v,int groupPosition, final int childPosition, long id) {
				if (groupPosition == 0) {
					handler.post(new Runnable() {
						public void run() {
							mCallBack.onItemSelected(childPosition, MainActivity.PLAY_DOWNLOADED_MUSIC);
						}
					});
				} else {
					
				}
				return false;
			}
		});
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if (downloadingMusic.size() == 0) return;
				if (msg.what == refresh_downloadingList) {
					Log.d(TAG, "handleMessage:refresh downloading list");
					downloadMusicListAdapter.notifyDataSetChanged();
				}
			}
		};
		//refreshTimer每隔500ms发送一次更新下载列表的消息
		refreshTimer.scheduleAtFixedRate(new TimerTask(){
			public void run() {
				handler.sendMessage(handler.obtainMessage(refresh_downloadingList));
			}
		}, 0, 500);
		return view;
	}

	// 音乐列表适配器
	private BaseExpandableListAdapter downloadMusicListAdapter = new BaseExpandableListAdapter() {

		public int getGroupCount() {
			return musicDownloadGroup.length;
		}

		public int getChildrenCount(int groupPosition) {
			if (groupPosition == 0) {
				return downloadedMusic.size();
			} else {
				return downloadingMusic.size();
			}
		}

		public Object getGroup(int groupPosition) {
			return musicDownloadGroup[groupPosition];
		}

		public Object getChild(int groupPosition, int childPosition) {
			if (groupPosition == 0) {
				return downloadedMusic.get(childPosition);
			} else {
				return downloadingMusic.get(childPosition);
			}
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public boolean hasStableIds() {
			return true;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView tv = new TextView(mContext);
			tv.setText((String) getGroup(groupPosition));
			tv.setTextSize(18);
			tv.setPadding(100, 10, 10, 10);
			return tv;
		}

		public View getChildView(int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			View view = convertView;
			Map<String, Object> item = null;
			if (groupPosition == 0) {
				item = downloadedMusic.get(childPosition);
				view = inflater.inflate(R.layout.downloadedmusiclist_item, null);
			} else {
				item = downloadingMusic.get(childPosition);
				view = inflater.inflate(R.layout.downloadingmusiclist_item, null);
			}
			String title = (String) item.get("title");
			String artist = (String) item.get("artist");
			TextView musicTitle = (TextView) view.findViewById(R.id.musicTitle);
			musicTitle.setTag("title");
			TextView musicArtist = (TextView) view.findViewById(R.id.musicArtist);
			musicTitle.setText(title);
			musicArtist.setText(artist);
			if (groupPosition == 1) {//groupPosition == 1表明为正在下载的列表，需要更新UI
				final ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.progressBar1);
				progressBar.setTag("progressBar");
				//获取对应的DownloadUtil以便获取下载进度
				final DownloadUtil util = (DownloadUtil)item.get("downloadUtil");
				progressBar.setProgress(util.getDownloadProgress());
				Log.d(TAG, title + ":下载进度为" + util.getDownloadProgress() + "%"); 
				final ImageView pause = (ImageView)view.findViewById(R.id.pause);
				pause.setTag("pause");
				pause.setImageResource(util.isPause()? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
				pause.setOnClickListener(new OnClickListener() {//暂停或者继续下载任务的监听
					public void onClick(View v) {
						if (util.isPause()) {
							util.setPause(false);
							pause.setImageResource(android.R.drawable.ic_media_pause);
						} else {
							util.setPause(true);
							pause.setImageResource(android.R.drawable.ic_media_play);
						}
					}
				});
				final ImageView delate = (ImageView)view.findViewById(R.id.delete);
				delate.setTag("delate");
				delate.setOnClickListener(new OnClickListener() {//删除下载任务的监听
					public void onClick(View v) {
						util.setDelete(true);
						downloadingMusic.remove(childPosition);
						downloadMusicListAdapter.notifyDataSetChanged();
						Toast.makeText(mContext, "删除成功", Toast.LENGTH_LONG)
						.show();
					}
				});
			}
			return view;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	};
	
	//初始化该Fragment检查数据库downloading_music表中是否有未完成的下载任务
	private void checkDownloadingMusic() {
		Cursor cursor = mMainActivity.getMyHelper().getReadableDatabase().rawQuery("select * from downloading_music", null);
		while (cursor.moveToNext()) {
			Map<String, Object> map = new HashMap<String, Object>();
			String title = cursor.getString(1);
			String artist = cursor.getString(2);
			String url = cursor.getString(3);
			String targetFile = cursor.getString(4);
			String fileSize = cursor.getString(5);
			String threadNum = cursor.getString(6);
			String currentSize = cursor.getString(7);
			String downloadSize = cursor.getString(8);
			map.put("title", title);
			map.put("artist", artist);
			map.put("url", url);
	    	DownloadUtil util = new DownloadUtil(title,artist,url,targetFile,
	    			Integer.parseInt(threadNum), mContext, mMainActivity.getMyHelper());
	    	map.put("downloadUtil", util);
	    	util.setPause(true);
	    	util.continueDownloading(Integer.parseInt(fileSize),Integer.parseInt(currentSize),downloadSize);
	    	downloadingMusic.add(map);
	    	Log.d(TAG, "add one downloading music:" + title);
		}
	}

	public void onDestroy() {
		if (updateDownloadListReceiver != null) mContext.unregisterReceiver(updateDownloadListReceiver);
		super.onDestroy();
	}
	
	public interface CallBack {
		public void onItemSelected(int position, int musicList);
	}

	
	private BroadcastReceiver updateDownloadListReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_DOWNLOADMUSIC_SUCCESS)) {
				String url = intent.getStringExtra("url");
				for (int i=0; i<downloadingMusic.size(); i++) {
					Map<String, Object> map = downloadingMusic.get(i);
					if (url.equals((String)map.get("url")))  {
						//一首歌曲下载完成时通知列表更新-正在下载移除该任务，已完成下载添加该音乐
						map.put("duration", StringUtil.getMusicLongDuration(intent.getStringExtra("filePath")));
						downloadedMusic.add(downloadingMusic.remove(i));
						Toast.makeText(mContext, "下载歌曲完成！", 100).show();
						break;
					}
				}
				downloadMusicListAdapter.notifyDataSetChanged();
			}
		}
		
	};

	public List<Map<String, Object>> getDownloadingMusic() {
		return downloadingMusic;
	}

	public List<Map<String, Object>> getDownloadedMusic() {
		return downloadedMusic;
	}

}
