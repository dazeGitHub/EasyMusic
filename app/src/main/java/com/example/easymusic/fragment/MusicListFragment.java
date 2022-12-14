package com.example.easymusic.fragment;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.easymusic.MainActivity;
import com.example.easymusic.R;
import com.example.easymusic.mysql.MyDBHelper;
import com.example.easymusic.myview.RefreshableListView;
import com.example.easymusic.util.LoadImageTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class MusicListFragment extends Fragment {
	private final String TAG = "MusicListFragment";
	private final String ACTION_REFRESH = "action.refreshmusicList";
	private List<Map<String, Object>> dbMusic = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> storedMusic = new ArrayList<Map<String, Object>>();
	private RefreshableListView musicListView;
	private LayoutInflater inflater;
	private CallBack mCallBack;
	private Context mContext;
	private MyDBHelper myHelper;
	private MainActivity mMainActivity;
	private Handler refreshHandler;
	public static final int REFERSH_MUSIC = 1;
	public static final int REFRESH_FINISH = 2;
	private int refreshAddMusic = 0;
	private int headerCount = 0;
	private Map<String, SoftReference<Bitmap>> ImageCacheMap = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		mContext = this.getActivity();
		ImageCacheMap = mMainActivity.getImageCacheMap();
		myHelper = new MyDBHelper(mContext, "easyMusic.db3", null, 1);
		inflater = LayoutInflater.from(mContext);
		dbMusic = mCallBack.getAllMusic();
		storedMusic = mCallBack.getStoredMusic();
		refreshHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case REFERSH_MUSIC:
					refreshMusicList();
					break;
				case REFRESH_FINISH:
					musicListAdapter.notifyDataSetChanged();
					musicListView.refreshComplete();
					Toast.makeText(mContext, "??????" + refreshAddMusic + "??????",Toast.LENGTH_SHORT).show();
					refreshAddMusic = 0;
					break;
				default:
					break;
				}
			}
			
		};
	}

	protected void refreshMusicList() {
		refreshAddMusic = mMainActivity.refreshMusicList();
		refreshHandler.sendMessage(refreshHandler.obtainMessage(REFRESH_FINISH));
	}

	// NetFragment???????????????????????????????????????view
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.musiclist, container, false);
		musicListView = (RefreshableListView) view.findViewById(R.id.musicList);
		musicListView.setAdapter(musicListAdapter);
		musicListView.setHandler(refreshHandler);
		headerCount = musicListView.getHeaderViewsCount();
		//musicListView.setHeaderViewHeight();
		musicListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (musicListView.isRefreshing()) {
					Toast.makeText(mContext,"??????????????????????????????",Toast.LENGTH_SHORT).show();
					return;
				}
				mCallBack.onItemSelected(position - headerCount, MainActivity.PLAY_ALL_MUSIC);
				TextView title = (TextView) view.findViewWithTag("title");
				Toast.makeText(mContext,"title = " + title.getText().toString(),Toast.LENGTH_SHORT).show();
			}
		});
		return view;
	}

	// ?????????????????????
	private BaseAdapter musicListAdapter = new BaseAdapter() {

		@Override
		public int getCount() {
			return dbMusic.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = convertView;
			Map<String, Object> item = dbMusic.get(position);
			if (convertView == null) {
				view = inflater.inflate(R.layout.musiclist_item, null);
			}
			final ImageView storeMusic = (ImageView) view.findViewById(R.id.love);
			if (checkIfStored((String) item.get("url"))) {
				storeMusic.setImageResource(android.R.drawable.btn_star_big_on);
			} else {
				storeMusic.setImageResource(android.R.drawable.btn_star_big_off);
			}
			storeMusic.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					new MyAsyncTask(storeMusic, dbMusic.get(position)).execute();
				}
			});
			ImageView musicTag = (ImageView) view.findViewById(R.id.musicTag);
			String title = (String) item.get("title");
			String artist = (String) item.get("artist");
			if (!checkBitmap(musicTag, title, artist, ImageCacheMap)) {
				musicTag.setImageResource(R.drawable.music);
			}
			//Bitmap bitmap = BitmapFactory.decodeFile(pathName);
			//musicTag.setImageBitmap(bitmap);
			TextView musicTitle = (TextView) view.findViewById(R.id.musicTitle);
			musicTitle.setTag("title");
			TextView musicArtist = (TextView) view.findViewById(R.id.musicArtist);
			musicTitle.setText(title);
			musicArtist.setText(artist);
			return view;
		}

	};

	// ??????????????????????????????????????????????????????????????????true???????????????false
	protected boolean checkIfStored(String url) {
		for (Map<String, Object> map : storedMusic) {
			if (url.equals((String) map.get("url"))) {
				return true;
			}
		}
		return false;
	}

	//???????????????????????????ImageCacheMap????????????????????????????????????????????????????????????????????????????????????????????????????????????
	public static boolean checkBitmap(ImageView view, String title, String artist, Map<String, SoftReference<Bitmap>> ImageCache) {
		if (ImageCache != null) {
			Log.d("checkBitmap", "ImageCacheMap.size = " + ImageCache.size());
			SoftReference<Bitmap> sBitmap = ImageCache.get(title+artist);
			if (sBitmap != null) {
				Bitmap bitmap = sBitmap.get();
				if (bitmap == null) {
					ImageCache.remove(title+artist);
                } else {
                	view.setImageBitmap(bitmap);
                	Log.d("checkBitmap", title + " get image from ImageCacheMap");
    				return true;
                }
			}
		}
		
		final String  localFile = MainActivity.downloadedPath
				+ "/album/"+ title + "-" + artist + ".jpg";
		File file = new File(localFile);
		if (file.exists()) {
			//????????????????????????????????????????????????
			new LoadImageTask(view, 50, 50, title+artist, ImageCache).execute(localFile);
			return true;
		}
		return false;
	}

	// ??????????????????????????????????????????????????????????????????
	private void refreshStoredMusic(Map<String, Object> musicInfo) {
		int i = 0;
		for (; i < storedMusic.size(); i++) {
			Map<String, Object> map = storedMusic.get(i);
			String url = (String) map.get("url");
			if (url.equals((String) musicInfo.get("url"))) {
				Log.d(TAG, "remove index =" + i);
				storedMusic.remove(i);
				//musicListAdapter.notifyDataSetChanged();
				mContext.sendBroadcast(new Intent(ACTION_REFRESH));
				return;
			}
		}
		storedMusic.add(musicInfo);
		mContext.sendBroadcast(new Intent(ACTION_REFRESH));
		// ????????????????????????????????????1??????????????????????????????-??????getStoredMusic()???????????????????????????2?????????????????????????????????storedMusic??????????????????
		// ????????????????????????
		// getStoredMusic();
		// ????????????????????????????????????????????????????????????????????????musicListView.setAdapter(adapter)?????????????????????????????????????????????????????????
		//musicListAdapter.notifyDataSetChanged();
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallBack = (CallBack) activity;
		mMainActivity = (MainActivity) activity;
	}

	public void onDestroy() {
		super.onDestroy();
	}

	public interface CallBack {
		public void onItemSelected(int position, int musicList);
		public List<Map<String, Object>> getAllMusic();
		public List<Map<String, Object>> getStoredMusic();
	}

	// ??????????????????/???????????????????????????
	private class MyAsyncTask extends AsyncTask<String, Void, Void> {
		private ImageView starView;
		private Map<String, Object> musicInfo;
		// ???????????????true???????????????????????????false????????????????????????
		private boolean storeSuccess;

		public MyAsyncTask(ImageView starView, Map<String, Object> musicInfo) {
			this.starView = starView;
			this.musicInfo = musicInfo;
		}

		protected void onPreExecute() {
			super.onPreExecute();
		}

		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		protected Void doInBackground(String... params) {
			Log.d(TAG, "doInBackground");
			Cursor cursor = myHelper.getReadableDatabase().rawQuery(
					"select * from stored_music", null);
			while (cursor.moveToNext()) {
				String title = cursor.getString(1);
				String artist = cursor.getString(2);
				Log.d(TAG, "title = " + title + " artist = " + artist);
				Log.d(TAG,
						"musicInfo.title = " + (String) musicInfo.get("title")
								+ " musicInfo.artist = "
								+ (String) musicInfo.get("artist"));
				if (cursor.getString(4).equals((String) musicInfo.get("url"))) {
					// ????????????????????????????????????????????????????????????-stored_music
					myHelper.getReadableDatabase()
							.execSQL(
									"delete from stored_Music where title like ? and artist like ?",
									new String[] { title, artist });
					storeSuccess = false;
					return null;
				}
			}
			// ????????????????????????????????????
			myHelper.getReadableDatabase().execSQL(
					"insert into stored_music values(null, ?, ?, ?, ?)",
					new Object[] { musicInfo.get("title"),
							musicInfo.get("artist"), musicInfo.get("duration"),
							musicInfo.get("url") });
			storeSuccess = true;
			return null;
		}

		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (storeSuccess) {
				starView.setImageResource(android.R.drawable.btn_star_big_on);
				refreshStoredMusic(musicInfo);
				Toast.makeText(mContext, "????????????", Toast.LENGTH_SHORT).show();
			} else {
				starView.setImageResource(android.R.drawable.btn_star_big_off);
				refreshStoredMusic(musicInfo);
				Toast.makeText(mContext, "????????????", Toast.LENGTH_SHORT).show();
			}
		}

	}

}
